param(
    [string]$ProjectRoot = (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
)

$ErrorActionPreference = 'Stop'
$failures = New-Object System.Collections.Generic.List[string]

function Assert-Contains([string]$Path, [string]$Pattern, [string]$Message) {
    if (-not (Select-String -LiteralPath $Path -Pattern $Pattern -Quiet)) { $failures.Add($Message) }
}

function Assert-Balanced([string]$Path, [string]$Open, [string]$Close, [string]$Label) {
    $raw = Get-Content -LiteralPath $Path -Raw
    $a = ([regex]::Matches($raw, $Open)).Count
    $b = ([regex]::Matches($raw, $Close)).Count
    if ($a -ne $b) { $failures.Add("$Label tidak seimbang pada $Path ($a/$b)") }
}

$webXml = Join-Path $ProjectRoot 'src/main/webapp/WEB-INF/web.xml'
[xml](Get-Content -LiteralPath $webXml -Raw) | Out-Null

$content = Join-Path $ProjectRoot 'src/main/java/ais/common/home/HomePortalContentService.java'
$pages = Join-Path $ProjectRoot 'src/main/java/ais/common/home/WebsitePageService.java'
$security = Join-Path $ProjectRoot 'src/main/java/ais/common/home/WebsitePublicSecurityFilter.java'
$discovery = Join-Path $ProjectRoot 'src/main/java/ais/action/servlet/WebsiteDiscovery.java'
$web = Join-Path $ProjectRoot 'src/main/java/ais/action/servlet/Web.java'
$homeJsp = Join-Path $ProjectRoot 'src/main/webapp/WEB-INF/baru/website/home.jsp'
$pageJsp = Join-Path $ProjectRoot 'src/main/webapp/WEB-INF/baru/website/page.jsp'

Assert-Contains $content 'g\.perguruanTinggi\.id = :college' 'Gelombang PMB wajib tenant-scoped.'
Assert-Contains $content '_college_agenda_shared' 'Agenda kampus tanpa tenant wajib ditutup secara default.'
Assert-Contains $pages 'p\.perguruanTinggi\.id = :tenant' 'Berita kampus detail/listing wajib tenant-scoped.'
Assert-Contains $pages 'k\.sekolah\.id = :tenant' 'Program sekolah detail/listing wajib tenant-scoped.'
Assert-Contains $security 'Content-Security-Policy' 'CSP belum diterapkan.'
Assert-Contains $security 'X-Content-Type-Options' 'Header nosniff belum diterapkan.'
Assert-Contains $security 'Referrer-Policy' 'Referrer Policy belum diterapkan.'
Assert-Contains $discovery 'sitemap.xml' 'Sitemap dinamis belum tersedia.'
Assert-Contains $discovery 'robots.txt' 'Robots dinamis belum tersedia.'
Assert-Contains $web 'WebsitePageService' 'Router halaman website belum aktif.'
Assert-Contains $homeJsp 'skip-link' 'Skip link beranda hilang.'
Assert-Contains $pageJsp 'breadcrumbs' 'Breadcrumb halaman detail hilang.'
Assert-Contains $pageJsp 'role="search"' 'Form pencarian aksesibel belum tersedia.'
Assert-Contains $pageJsp 'websiteCspNonce' 'JSON-LD halaman detail belum memakai nonce.'

foreach ($file in @($content, $pages, $security, $discovery, $web)) {
    Assert-Balanced $file '\{' '\}' 'Kurung kurawal Java'
}
foreach ($file in @($homeJsp, $pageJsp)) {
    Assert-Balanced $file '<%' '%>' 'Scriptlet JSP'
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host 'Validasi statis Website Institusi V4: LULUS' -ForegroundColor Green
Write-Host 'Pemeriksaan ini tidak melakukan compile, build WAR, atau deployment.'
