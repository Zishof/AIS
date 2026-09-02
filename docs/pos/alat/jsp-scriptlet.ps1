<#
.SYNOPSIS
Kompilasi Java DI DALAM scriptlet JSP terhadap pohon kelas proyek.

.DESCRIPTION
Gerbang keempat. Tiga sebelumnya menutup: .java yang berubah, seluruh .java, dan
TERJEMAHAN JSP. Yang ini menutup sisanya -- Java di dalam scriptlet, satu-satunya
kanal yang tidak pernah dilihat kompilator mana pun sampai Tomcat menerjemahkan
halamannya untuk pengguna.

Caranya: Jasper menerjemahkan tiap JSP menjadi berkas .java biasa, lalu berkas
itu dikompilasi terhadap pohon kelas proyek. Rujukan yang basi -- metode yang
sudah dihapus, kelas yang berpindah, pemanggilan statis atas metode instance --
muncul di sini.

Tiga halaman rusak ditemukan pada jalan pertamanya (docs/pos/84).

PENTING soal derau: banyak berkas .jsp adalah POTONGAN yang di-include dan
memakai variabel milik halaman pemanggilnya. Dikompilasi satu-satu, potongan itu
menghasilkan galat palsu berlimpah (59 galat "variable vm" pada uji 300 berkas).
Dikompilasi SEKALIGUS dalam satu pemanggilan javac, derau itu nyaris hilang --
lima galat dari 10.374 berkas, semuanya nyata. Karena itu skrip ini sengaja
tidak menyediakan mode per-berkas.

PENTING soal kesegaran: pohon kelas yang basi memberi hasil yang basi. Metode
yang dihapus pagi ini tidak akan terdeteksi sampai pohon kelasnya dibangun ulang.
Jalankan alat/kompilasi-penuh.sh lebih dulu, lalu tunjuk keluarannya lewat -Kelas.

.PARAMETER Kelas
Pohon kelas proyek hasil kompilasi penuh. WAJIB.

.EXAMPLE
  sh kompilasi-penuh.sh /tmp/kelas-ais
  powershell -File jsp-scriptlet.ps1 -Kelas /tmp/kelas-ais
#>
param(
    [Parameter(Mandatory=$true)][string]$Kelas,
    [string]$Webapp = 'C:\opt\AIS\ais\src\main\webapp',
    [string]$Tomcat = 'C:\opt\tomcat7\apache-tomcat-7.0.109',
    [string]$Ant    = 'C:\opt\apache-ant-1.10.15'
)

if (-not (Test-Path $Kelas)) { "pohon kelas tidak ditemukan: $Kelas"; exit 1 }
$jml = @(Get-ChildItem $Kelas -Recurse -Filter *.class -EA SilentlyContinue).Count
if ($jml -lt 1000) { "pohon kelas terlalu kecil ($jml kelas); kompilasi penuhnya gagal?"; exit 1 }

$kerja = Join-Path ([IO.Path]::GetTempPath()) 'jsp-scriptlet'
Remove-Item -Recurse -Force $kerja -EA SilentlyContinue
New-Item -ItemType Directory -Force "$kerja\gen" | Out-Null
New-Item -ItemType Directory -Force "$kerja\kelas" | Out-Null

$tom = @()
$tom += (Get-ChildItem "$Tomcat\lib\*.jar" -EA SilentlyContinue | ForEach-Object { $_.FullName })
$tom += (Get-ChildItem "$Tomcat\bin\*.jar" -EA SilentlyContinue | ForEach-Object { $_.FullName })
$antj = @(Get-ChildItem "$Ant\lib\*.jar" -EA SilentlyContinue | ForEach-Object { $_.FullName })
if ($tom.Count -lt 20 -or $antj.Count -lt 1) { 'classpath Tomcat/Ant tidak lengkap.'; exit 1 }

"pohon kelas   : $jml kelas"
'--- 1/2 menerjemahkan JSP'
& java -Xmx2g -cp (($tom + $antj) -join ';') org.apache.jasper.JspC `
      -uriroot $Webapp -d "$kerja\gen" -webapp $Webapp *> "$kerja\terjemah.log"
$gen = @(Get-ChildItem "$kerja\gen" -Recurse -Filter *.java -EA SilentlyContinue)
$severe = @(Select-String -Path "$kerja\terjemah.log" -Pattern 'SEVERE' -EA SilentlyContinue).Count
"diterjemahkan : $($gen.Count)  (galat terjemahan: $severe)"
if ($severe -gt 0) {
    Select-String -Path "$kerja\terjemah.log" -Pattern 'SEVERE' | Select-Object -First 10 | ForEach-Object { $_.Line }
    exit 1
}
if ($gen.Count -eq 0) { 'tidak satu JSP pun diterjemahkan.'; Get-Content "$kerja\terjemah.log" -TotalCount 5; exit 1 }

# Urutan classpath penting: API JSP/servlet Tomcat HARUS mendahului WEB-INF/lib,
# yang memuat jsp-api lawas dan menutupi getJspApplicationContext milik JSP 2.1.
$cp = $tom + $Kelas + @(Get-ChildItem "$Webapp\WEB-INF\lib\*.jar" -EA SilentlyContinue | ForEach-Object { $_.FullName })
# Potongan JSP (berkas berawalan garis bawah) memakai variabel milik halaman yang
# meng-include-nya. Dikompilasi berdiri sendiri, ia SELALU gagal, dan galatnya palsu.
# Jasper menyandikan "_" sebagai "_005f" pada nama berkas hasil terjemahan.
$potongan = @($gen | Where-Object { $_.Name -like '_005f*' })
$pakai    = @($gen | Where-Object { $_.Name -notlike '_005f*' })
"potongan dilewati: $($potongan.Count)"
($pakai | ForEach-Object { $_.FullName }) | Set-Content "$kerja\daftar.txt"

'--- 2/2 mengompilasi scriptlet'
# Dijalankan lewat cmd, bukan langsung dari PowerShell. Baik "*>" maupun
# "2>&1 | Out-File" tetap melewatkan stderr javac melalui pemformat record galat
# PowerShell, yang MEMOTONG baris di lebar konsol -- jalur berkas terbelah dua
# sehingga lognya tidak dapat diurai alat mana pun. Redirection cmd menulis
# keluaran javac apa adanya.
$opsi = @(
    '-source', '1.7', '-target', '1.7', '-encoding', 'UTF-8', '-nowarn',
    '-proc:none', '-Xmaxerrs', '20000',
    '-cp', ($cp -join ';'),
    '-d', ("$kerja" + '\kelas')
) -join "`n"
Set-Content -Encoding ascii "$kerja\opsi.txt" $opsi
cmd /c "javac -J-Xmx3g `"@$kerja\opsi.txt`" `"@$kerja\daftar.txt`" 2> `"$kerja\galat.log`""
$kode  = $LASTEXITCODE
$galat = @(Select-String -Path "$kerja\galat.log" -Pattern 'error:' -EA SilentlyContinue).Count
$hasil = @(Get-ChildItem "$kerja\kelas" -Recurse -Filter *.class -EA SilentlyContinue).Count
"galat         : $galat"
"kelas         : $hasil"

if ($galat -gt 0) {
    ''
    Select-String -Path "$kerja\galat.log" -Pattern 'error:' -Context 0,2 |
        Select-Object -First 15 | ForEach-Object { $_.Line; $_.Context.PostContext }
    ''
    "log lengkap: $kerja\galat.log"
    exit 1
}
if ($kode -ne 0) { ''; 'javac gagal tanpa galat kompilasi:'; Get-Content "$kerja\galat.log" -TotalCount 5; exit 1 }
if ($hasil -eq 0) { ''; 'tidak satu kelas pun dihasilkan; kompilasinya tidak berjalan.'; exit 1 }
'BERSIH'
exit 0
