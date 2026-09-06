# Membangun ulang instans Tomcat UAT terisolasi dari instalasi bersama.
#
# MENGAPA TERISOLASI
#   Instalasi Tomcat bersama TIDAK boleh disentuh. Dari 4.009 kelas hasil kompilasi sumber
#   terkini, 2.936 berbeda dari yang ter-deploy dan hanya 41 milik pekerjaan tenant; sisanya
#   pekerjaan sesi lain. Menyalinnya ke sana akan menerbitkan semua itu sekaligus.
#
# EMPAT PENYESUAIAN YANG WAJIB, dan alasannya (rincian: docs/pos/116 §1)
#   1. Port digeser ke 18xxx           -- agar bisa hidup berdampingan dengan instans 8080.
#   2. cfg Hibernate -> 127.0.0.1:55600 -- INI YANG PALING BERBAHAYA bila terlewat.
#      HibernateUtil pada mode bawaan mengembalikan factory ZKPlus SEBELUM
#      DbCredentialOverride sempat jalan, sehingga hibernate.cfg.xml dipakai apa adanya --
#      dan isinya menunjuk 127.0.0.1:5432, klaster PostgreSQL NYATA di mesin ini.
#   3. META-INF/context.xml -> 55600   -- datasource JNDI TIDAK dijangkau DbCredentialOverride
#      (mekanisme itu hanya menambal objek Configuration Hibernate).
#   4. autoDeploy="false"              -- setiap penyuntingan berkas memicu deploy ulang 240
#      detik; selama itu konektornya turun dan seluruh permintaan dijawab 404.
#
# Plus satu tambalan lingkungan: entitas GrupItemBiayaSekolah (pekerjaan sesi lain) belum
# terdaftar di hibernate.cfg.xml, dan tanpa itu SessionFactory gagal dengan 102
# AnnotationException.
#
# PAKAI:  powershell -ExecutionPolicy Bypass -File bangun-instans-uat.ps1

$ErrorActionPreference = 'Stop'
$HOME_TC = 'C:\opt\tomcat7\apache-tomcat-7.0.109'
$BASE    = 'C:\opt\uat-inventory\tomcat-uat'
$KELAS   = 'C:\opt\uat-inventory\kelas-pentas'

Write-Output '== 1. kerangka CATALINA_BASE =='
foreach ($d in @('conf','webapps','logs','temp','work','bin')) {
    New-Item -ItemType Directory -Force (Join-Path $BASE $d) | Out-Null
}
Copy-Item "$HOME_TC\conf\*" (Join-Path $BASE 'conf') -Recurse -Force

Write-Output '== 2. port 18xxx =='
$f = Join-Path $BASE 'conf\server.xml'
$x = Get-Content $f -Raw
$x = $x -replace '<Server port="8005"', '<Server port="18005"'
$x = $x -replace 'port="8080"',  'port="18080"'
$x = $x -replace 'port="8443"',  'port="18443"'
$x = $x -replace 'port="8009"',  'port="18009"'
# -replace tidak peka huruf: penggantian di atas ikut mengubah redirectPort menjadi
# redirectport. Atribut XML PEKA huruf, jadi kapitalisasinya dikembalikan.
$x = $x -creplace 'redirectport=', 'redirectPort='
$x = $x -creplace 'autoDeploy="true"', 'autoDeploy="false"'
Set-Content -Path $f -Value $x -NoNewline

Write-Output '== 3. setenv =='
@'
@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202"
set "JRE_HOME=C:\Program Files\Java\jdk1.8.0_202\jre"
rem MaxPermSize sengaja tidak dipasang: PermGen sudah diganti Metaspace di Java 8.
set "CATALINA_OPTS=-Xms512m -Xmx2048m"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dais.db.override.file=C:\opt\uat-inventory\db-uat.properties"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
'@ | Set-Content (Join-Path $BASE 'bin\setenv.bat') -NoNewline

Write-Output '== 4. salin webapp (1,3 GB) =='
& robocopy.exe "$HOME_TC\webapps\ais" (Join-Path $BASE 'webapps\ais') /E /NFL /NDL /NJH /NP /R:1 /W:1 /MT:16 | Out-Null
# robocopy: 0..7 sukses (1=tersalin, 2=ada ekstra di tujuan). >=8 barulah galat.
if ($LASTEXITCODE -ge 8) { throw "robocopy gagal: $LASTEXITCODE" }

Write-Output '== 5. cfg Hibernate + context.xml -> 55600 =='
& python 'C:\opt\uat-inventory\betulkan-cfg.py'
$c = Join-Path $BASE 'webapps\ais\WEB-INF\classes\hibernate.cfg.xml'
$s = Get-Content $c -Raw
$blok = @"
<property name="hibernate.connection.driver_class">org.postgresql.Driver</property>
"@
# Blok AKTIF diikat sebagai satu kesatuan; blok MySQL yang dikomentari juga memuat "root".
$s = $s -replace 'jdbc:postgresql://127\.0\.0\.1:5432/ais', 'jdbc:postgresql://127.0.0.1:55600/ais_uat'
$s = $s -replace '<property name="hibernate\.connection\.password">root23</property>',
                 '<property name="hibernate.connection.password">uat</property>'
$s = $s -replace '(<property name="hibernate\.connection\.url">jdbc:postgresql://127\.0\.0\.1:55600/ais_uat</property>\s*\r?\n\s*<property name="hibernate\.connection\.username">)root(</property>)',
                 '$1uat$2'
Set-Content -Path $c -Value $s -NoNewline

$ctx = Join-Path $BASE 'webapps\ais\META-INF\context.xml'
$t = Get-Content $ctx -Raw
$t = $t -replace 'jdbc:postgresql://localhost:5433/ais', 'jdbc:postgresql://127.0.0.1:55600/ais_uat'
$t = $t -replace 'username="root"', 'username="uat"'
$t = $t -replace 'password="\$\{AIS_DB_PASSWORD\}"', 'password="uat"'
Set-Content -Path $ctx -Value $t -NoNewline

Write-Output '== 6. daftarkan GrupItemBiayaSekolah =='
$s = Get-Content $c -Raw
if ($s -notmatch 'GrupItemBiayaSekolah') {
    $jangkar = '<mapping class="ais.database.model.sekolah.ItemBiayaSekolah"></mapping>'
    $s = $s.Replace($jangkar, $jangkar + "`n`t`t" +
        '<mapping class="ais.database.model.sekolah.GrupItemBiayaSekolah"></mapping>')
    Set-Content -Path $c -Value $s -NoNewline
}

Write-Output '== 7. overlay kelas tenant =='
& robocopy.exe $KELAS (Join-Path $BASE 'webapps\ais\WEB-INF\classes') /E /NFL /NDL /NJH /NP /R:1 /W:1 /MT:16 | Out-Null
if ($LASTEXITCODE -ge 8) { throw "overlay gagal: $LASTEXITCODE" }

Write-Output ''
Write-Output 'SELESAI. Menyalakan:'
Write-Output '  $env:CATALINA_HOME="C:\opt\tomcat7\apache-tomcat-7.0.109"'
Write-Output '  $env:CATALINA_BASE="C:\opt\uat-inventory\tomcat-uat"'
Write-Output '  cmd /c "%CATALINA_HOME%\bin\catalina.bat run > C:\opt\uat-inventory\tomcat.log 2>&1"'
