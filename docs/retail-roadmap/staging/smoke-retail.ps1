param(
  [Parameter(Mandatory=$true)][string]$Config,
  [switch]$AllowWrites
)

$ErrorActionPreference = 'Stop'
$cfg = Get-Content -LiteralPath $Config -Raw | ConvertFrom-Json
$uri = [Uri]$cfg.baseUrl
$safeHost = $uri.Host -match '(staging|test|localhost)' -or $uri.IsLoopback
if (-not $safeHost) { throw "Smoke test ditolak: host '$($uri.Host)' tidak dikenali sebagai staging/test." }
$password = [Environment]::GetEnvironmentVariable([string]$cfg.passwordEnvironmentVariable)
if ([string]::IsNullOrWhiteSpace($password)) { throw "Environment variable password belum diisi." }

$apiUrl = "$($cfg.baseUrl.TrimEnd('/'))/Api_eBisnis"
$login = Invoke-WebRequest -Method Post -Uri $apiUrl -ContentType 'application/json' `
  -Body (@{action='login'; username=$cfg.username; password=$password; labelPerangkat='SMOKE-STAGING'} | ConvertTo-Json -Compress) -UseBasicParsing
if ($login.StatusCode -ne 200) {
  throw "Login staging gagal. HTTP $($login.StatusCode)."
}
$loginJson = $login.Content | ConvertFrom-Json
if ($loginJson.status -ne 'success' -or [string]::IsNullOrWhiteSpace([string]$loginJson.token)) {
  throw "Login staging ditolak: $($loginJson.message)"
}
$headers = @{ Authorization="Bearer $($loginJson.token)"; 'X-Request-ID'="SMOKE-$([guid]::NewGuid())" }

function Invoke-PosAction([string]$Action, [object]$Payload) {
  $body = @{ action=$Action }
  if ($Payload -is [System.Collections.IDictionary]) {
    foreach ($key in $Payload.Keys) { $body[$key] = $Payload[$key] }
  } elseif ($null -ne $Payload) {
    foreach ($property in $Payload.PSObject.Properties) {
      $body[$property.Name] = $property.Value
    }
  }
  $response = Invoke-WebRequest -Method Post -Uri $apiUrl -Headers $headers -ContentType 'application/json' `
    -Body ($body | ConvertTo-Json -Depth 8 -Compress) -UseBasicParsing
  if ($response.StatusCode -ne 200) { throw "$Action gagal dengan HTTP $($response.StatusCode)." }
  $json = $response.Content | ConvertFrom-Json
  if ($json.status -ne 'success') { throw "$Action ditolak: $($json.message)" }
  return $json
}

$results = @()
$results += [pscustomobject]@{ test='login'; status='LULUS' }
$catalog = Invoke-PosAction 'katalog' @{}
$results += [pscustomobject]@{ test='baca katalog'; status=if ($catalog) {'LULUS'} else {'GAGAL'} }
$recon = Invoke-PosAction 'produk_rekonsiliasi_ledger' @{ page=1; per_page=15; hanya_selisih=$true }
$results += [pscustomobject]@{ test='rekonsiliasi ledger'; status=if ($recon) {'LULUS'} else {'GAGAL'} }

if ($AllowWrites) {
  if ([long]$cfg.memberId -le 0 -or [long]$cfg.productId -le 0) {
    throw 'Fixture memberId dan productId wajib diisi untuk smoke test tulis.'
  }
  # Kunci unik yang sama dipakai dua kali untuk membuktikan exactly-once topup.
  $key = "SMOKE-TOPUP-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
  $payload = @{ id_member=[long]$cfg.memberId; nominal=1; keterangan='SMOKE TEST'; idempotency_key=$key }
  $first = Invoke-PosAction 'topup_saldo' $payload
  $second = Invoke-PosAction 'topup_saldo' $payload
  if (($first | ConvertTo-Json -Compress) -ne ($second | ConvertTo-Json -Compress)) {
    throw 'Idempotensi topup gagal: respons pengulangan berbeda.'
  }
  $results += [pscustomobject]@{ test='topup exactly-once'; status='LULUS' }

  if ($null -eq $cfg.salePayload -or $cfg.salePayload.transaksi.Count -eq 0) {
    throw 'salePayload.transaksi wajib berisi fixture untuk smoke penjualan.'
  }
  $unique = "SMOKE-SALE-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
  $saleJson = ($cfg.salePayload | ConvertTo-Json -Depth 12 -Compress).Replace('{{unique}}',$unique)
  $sale = Invoke-PosAction 'bayar' ($saleJson | ConvertFrom-Json)
  $saleId = $sale.idTransaksi
  if ($null -eq $saleId) { $saleId = $sale.id }
  if ($null -eq $saleId) { throw 'Respons penjualan tidak membawa id transaksi untuk pengujian lanjut.' }
  $results += [pscustomobject]@{ test='penjualan'; status='LULUS' }

  if ($null -eq $cfg.returnPayload -or $cfg.returnPayload.items.Count -eq 0) {
    throw 'returnPayload.items wajib berisi fixture retur penjualan.'
  }
  $returnJson = ($cfg.returnPayload | ConvertTo-Json -Depth 12 -Compress).Replace('{{unique}}',$unique).Replace('{{saleId}}',[string]$saleId)
  $returnPayload = $returnJson | ConvertFrom-Json
  $returnPayload | Add-Member -NotePropertyName idempotency_key -NotePropertyValue "SMOKE-RETURN-$unique" -Force
  [void](Invoke-PosAction 'retur_penjualan_simpan' $returnPayload)
  # Ulangi persis request yang sama: tidak boleh membuat retur kedua.
  [void](Invoke-PosAction 'retur_penjualan_simpan' $returnPayload)
  $results += [pscustomobject]@{ test='retur exactly-once'; status='LULUS' }

  $cancelPayload = @{
    id=$saleId
    alasan='Pembatalan otomatis smoke test staging'
    idempotency_key="SMOKE-CANCEL-$unique"
  }
  [void](Invoke-PosAction 'batalkan_transaksi' $cancelPayload)
  [void](Invoke-PosAction 'batalkan_transaksi' $cancelPayload)
  $results += [pscustomobject]@{ test='pembatalan exactly-once'; status='LULUS' }
}

if (-not [string]::IsNullOrWhiteSpace([string]$cfg.importFixtureXlsx)) {
  if (-not (Test-Path -LiteralPath $cfg.importFixtureXlsx)) {
    throw "Fixture impor tidak ditemukan: $($cfg.importFixtureXlsx)"
  }
  $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($cfg.importFixtureXlsx))
  [void](Invoke-PosAction 'produk_impor_excel_preview' @{file_base64=$base64; format='accurate'})
  $results += [pscustomobject]@{ test='preview impor (tanpa komit)'; status='LULUS' }
}

$results | Format-Table -AutoSize

