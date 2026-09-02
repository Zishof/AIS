<#
.SYNOPSIS
Terjemahkan JSP dengan Jasper (prakompiler Tomcat) untuk menangkap JSP rusak
SEBELUM penggunanya yang menemukannya.

.DESCRIPTION
Gerbang kompilasi di alat/kompilasi-*.sh hanya menutup berkas .java. JSP tidak
ikut: ia baru diterjemahkan Tomcat ketika halamannya dibuka, jadi tag yang tidak
ditutup atau direktif yang salah lolos ke produksi tanpa satu pun peringatan --
dan baru ketahuan sebagai halaman galat di depan pengguna.

Yang diperiksa adalah TERJEMAHAN JSP-nya: tag, direktif, penutupan scriptlet,
resolusi include dan taglib. Java DI DALAM scriptlet tidak ikut dinilai, karena
itu menuntut seluruh pohon kelas proyek ikut dikompilasi.

Classpath-nya tidak sepele dan ketiga bagiannya wajib ada:
  lib Tomcat   -- jasper.jar, servlet-api, el
  bin Tomcat   -- tomcat-juli.jar (org.apache.juli.logging.LogFactory)
  lib Ant      -- JspC extends org.apache.tools.ant.Task
Kurang salah satunya, kegagalannya menyesatkan: "Could not find or load main
class org.apache.jasper.JspC", seolah Jasper-nya tidak ada.

.PARAMETER Sejak
Tanggal (2026-09-01) atau revisi (83000). Bawaan: awal hari ini.

.PARAMETER Semua
Sapu SELURUH JSP di webapp, bukan hanya yang berubah.

.EXAMPLE
  powershell -File jsp-terjemah.ps1
  powershell -File jsp-terjemah.ps1 -Sejak 2026-09-01
  powershell -File jsp-terjemah.ps1 -Semua

.NOTES
Sudah dibuktikan MENOLAK, bukan sekadar melaporkan bersih: JSP dgn scriptlet
tidak ditutup menghasilkan "Unterminated [<%] tag" dan hitungan galat 1.
Pemeriksa yang belum pernah dibuktikan gagal tidak layak dipakai sebagai bukti.
#>
param(
    [string]$Sejak = (Get-Date -Format 'yyyy-MM-dd'),
    [switch]$Semua,
    [string]$Webapp = 'C:\opt\AIS\ais\src\main\webapp',
    [string]$Tomcat = 'C:\opt\tomcat7\apache-tomcat-7.0.109',
    [string]$Ant    = 'C:\opt\apache-ant-1.10.15'
)

$ErrorActionPreference = 'Continue'
$keluar = Join-Path ([IO.Path]::GetTempPath()) 'jsp-terjemah'
Remove-Item -Recurse -Force $keluar -EA SilentlyContinue
New-Item -ItemType Directory -Force $keluar | Out-Null
$log = Join-Path $keluar 'log.txt'

$jars = @()
foreach ($d in "$Tomcat\lib\*.jar", "$Tomcat\bin\*.jar", "$Ant\lib\*.jar") {
    $jars += (Get-ChildItem $d -EA SilentlyContinue | ForEach-Object { $_.FullName })
}
if ($jars.Count -lt 20) { "classpath tidak lengkap ($($jars.Count) jar); periksa jalur Tomcat/Ant."; exit 1 }
$cp = $jars -join ';'

Set-Location $Webapp
if ($Semua) {
    "cakupan      : SELURUH webapp"
    & java -Xmx2g -cp $cp org.apache.jasper.JspC -uriroot $Webapp -d $keluar -webapp $Webapp *> $log
} else {
    $spek = if ($Sejak -match '^\d+$') { $Sejak } else { "{$Sejak}" }
    $daftar = @()
    $daftar += (& svn diff --summarize -r "$spek`:HEAD" . 2>$null | ForEach-Object { $_.Substring(8).Trim() })
    $daftar += (& svn status . 2>$null | Where-Object { $_ -match '^[MA]' } | ForEach-Object { $_.Substring(8).Trim() })
    $jsp = $daftar | Where-Object { $_ -like '*.jsp' } | Sort-Object -Unique |
           Where-Object { Test-Path (Join-Path $Webapp $_) }
    "cakupan      : berubah sejak $spek"
    "berkas diuji : $($jsp.Count)"
    if ($jsp.Count -eq 0) { "tidak ada JSP yang berubah"; exit 0 }
    $penuh = $jsp | ForEach-Object { Join-Path $Webapp $_ }
    & java -Xmx2g -cp $cp org.apache.jasper.JspC -uriroot $Webapp -d $keluar @penuh *> $log
}

# Tiga syarat, sama seperti gerbang .java: hitungan galat saja tidak cukup, karena
# Jasper bisa gagal SEBELUM menerjemahkan apa pun dan keluarannya tanpa "SEVERE".
$severe = @(Select-String -Path $log -Pattern 'SEVERE' -EA SilentlyContinue).Count
$hasil  = @(Get-ChildItem $keluar -Recurse -Filter *.java -EA SilentlyContinue).Count
"galat        : $severe"
"java dihasilkan: $hasil"

if ($severe -gt 0) {
    ''
    Select-String -Path $log -Pattern 'SEVERE' | Select-Object -First 20 | ForEach-Object { $_.Line }
    ''
    "log lengkap: $log"
    exit 1
}
if ($hasil -eq 0) {
    ''
    'tidak satu berkas pun diterjemahkan; penerjemahannya tidak benar-benar berjalan.'
    Get-Content $log -TotalCount 5
    exit 1
}
'BERSIH'
exit 0
