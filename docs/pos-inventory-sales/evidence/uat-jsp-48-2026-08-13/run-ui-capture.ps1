param(
    [string]$BaseUrl = 'http://localhost:18080/ais',
    [Parameter(Mandatory = $true)][string]$Username,
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$OutputDir = '',
    [int]$DelaySeconds = 3,
    [ValidateRange(1,48)][int]$From = 1,
    [ValidateRange(1,48)][int]$To = 48
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $PSScriptRoot 'screenshots'
}
Add-Type -AssemblyName System.Drawing
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class InventoryUatWindow {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int command);
}
'@

$pages = @(
    'data_supplier','daftar_supplier','detail_supplier_aktif','data_customer',
    'daftar_customer','detail_customer_aktif','data_sales','data_stok_barang',
    'laporan_opname','cetak_laporan_opname','harga_beli_jual','cetak_harga_beli_jual',
    'cetak_harga_jual','ekspor_harga_stok','cetak_daftar_stok','hasil_cetak_stok',
    'menu_master_harga','harga_beli_supplier','harga_jual_customer','pembelian_supplier',
    'hutang_pembelian','data_hutang_supplier','hutang_dengan_lunas','pembayaran_hutang',
    'riwayat_pembayaran_hutang','cetak_pembayaran_hutang','analisis_hutang','cetak_faktur_pembelian',
    'laporan_pembelian_periode','penjualan_sales','piutang_penjualan','data_piutang_customer',
    'piutang_dengan_lunas','pembayaran_piutang','riwayat_pembayaran_piutang','cetak_pembayaran_piutang',
    'analisis_piutang_customer','analisis_piutang_sales','surat_perintah_sales','nota_sales',
    'laporan_piutang','cetak_laporan_piutang','kas_jurnal','data_perkiraan',
    'parameter_laba_rugi','cetak_laba_rugi_kotor','laporan_laba_rugi','cetak_laporan_laba_rugi'
)

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$before = @(Get-Process msedge -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } |
    ForEach-Object { $_.MainWindowHandle.ToInt64() })
$edgeExe = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if (-not (Test-Path $edgeExe)) {
    $edgeExe = (Get-Command msedge.exe -ErrorAction Stop).Source
}
$loginFile = Join-Path $env:TEMP 'ais-inventory-uat-login.html'
$encodedUser = [System.Net.WebUtility]::HtmlEncode($Username)
$encodedPassword = [System.Net.WebUtility]::HtmlEncode($Password)
$loginHtml = '<!doctype html><html><body><form id="f" method="post" action="{0}/j_spring_security_check"><input name="j_username" value="{1}"><input name="j_password" value="{2}"></form><script>document.getElementById("f").submit()</script></body></html>' -f $BaseUrl.TrimEnd('/'), $encodedUser, $encodedPassword
[System.IO.File]::WriteAllText($loginFile, $loginHtml, [System.Text.Encoding]::UTF8)
$loginUrl = ([uri]$loginFile).AbsoluteUri
Start-Process -FilePath $edgeExe -ArgumentList @('--new-window', '--start-maximized', $loginUrl)

$edge = $null
for ($attempt = 0; $attempt -lt 30 -and $null -eq $edge; $attempt++) {
    Start-Sleep -Milliseconds 500
    $edge = Get-Process msedge -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 -and $before -notcontains $_.MainWindowHandle.ToInt64() } |
        Sort-Object StartTime -Descending | Select-Object -First 1
}
if ($null -eq $edge) {
    $edge = Get-Process msedge -ErrorAction Stop | Where-Object { $_.MainWindowHandle -ne 0 } |
        Sort-Object StartTime -Descending | Select-Object -First 1
}
if ($null -eq $edge) { throw 'Jendela Microsoft Edge untuk UAT tidak ditemukan.' }

$handle = $edge.MainWindowHandle
[InventoryUatWindow]::ShowWindow($handle, 3) | Out-Null
[InventoryUatWindow]::SetForegroundWindow($handle) | Out-Null
Start-Sleep -Seconds 3
Remove-Item -LiteralPath $loginFile -Force -ErrorAction SilentlyContinue
$shell = New-Object -ComObject WScript.Shell

function Open-UatPage([string]$url) {
    [InventoryUatWindow]::SetForegroundWindow($handle) | Out-Null
    Set-Clipboard -Value $url
    $shell.SendKeys('^l')
    Start-Sleep -Milliseconds 150
    $shell.SendKeys('^v')
    $shell.SendKeys('{ENTER}')
    Start-Sleep -Seconds $DelaySeconds
}

function Save-WindowCapture([string]$path) {
    $rect = New-Object InventoryUatWindow+RECT
    if (-not [InventoryUatWindow]::GetWindowRect($handle, [ref]$rect)) {
        throw 'Tidak dapat membaca batas jendela Edge.'
    }
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
    $bitmap = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

if ($From -gt $To) { throw 'Parameter From tidak boleh lebih besar daripada To.' }
for ($i = $From - 1; $i -lt $To; $i++) {
    $number = ($i + 1).ToString('00')
    $page = $pages[$i]
    Open-UatPage ('{0}/main?inventory={1}#screen={2}' -f $BaseUrl.TrimEnd('/'), $page, $number)
    Save-WindowCapture (Join-Path $OutputDir ('{0}-{1}.png' -f $number, $page))
    Write-Host ('[{0}/48] {1}' -f ($i + 1), $page)
}

Write-Host ('Selesai. Bukti UI: {0}' -f $OutputDir)
