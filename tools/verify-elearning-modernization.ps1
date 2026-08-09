param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$failures = New-Object System.Collections.Generic.List[string]
$index = Join-Path $RepositoryRoot 'webapp\WEB-INF\baru\modul\elearning\index.jsp'
$css = Join-Path $RepositoryRoot 'webapp\css\baru\base-elearning.css'
$requiredPages = @('dasbor.jsp','ringkasan.jsp','linimasa.jsp','materi.jsp','tugas.jsp','ujian.jsp','diskusi.jsp','kalender.jsp','obe.jsp','laporan.jsp')

function Assert-Contains([string]$Path, [string]$Pattern, [string]$Message) {
    if (-not (Select-String -LiteralPath $Path -Pattern $Pattern -Quiet)) { $failures.Add($Message) }
}

foreach ($page in $requiredPages) {
    $path = Join-Path $RepositoryRoot ('webapp\WEB-INF\baru\modul\elearning\' + $page)
    if (-not (Test-Path -LiteralPath $path)) { $failures.Add("Halaman wajib hilang: $page") }
    Assert-Contains $index ([regex]::Escape($page)) "Halaman tidak di-include oleh shell: $page"
}

$ids = Select-String -LiteralPath $index -Pattern 'id="([A-Za-z][A-Za-z0-9_-]*)"' -AllMatches |
    ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
$duplicateIds = $ids | Group-Object | Where-Object Count -gt 1 | Select-Object -ExpandProperty Name
if ($duplicateIds) { $failures.Add('ID statis duplikat pada index.jsp: ' + ($duplicateIds -join ', ')) }

Assert-Contains $index 'elearning_workspace_modern_aktif' 'Feature toggle workspace modern tidak ditemukan.'
Assert-Contains $index 'index_classic.jsp' 'Fallback tampilan klasik tidak ditemukan.'
Assert-Contains $index 'data-role=' 'Dashboard belum memiliki konteks role.'
Assert-Contains $index 'data-el-course-open' 'Navigasi workspace mata kuliah tidak ditemukan.'
Assert-Contains $index 'unhandledrejection' 'Error boundary Promise tidak ditemukan.'
Assert-Contains $css '@media \(max-width: 991\.98px\)' 'Breakpoint tablet tidak ditemukan.'
Assert-Contains $css '@media \(max-width: 575\.98px\)' 'Breakpoint mobile tidak ditemukan.'
Assert-Contains $css 'prefers-reduced-motion' 'Dukungan reduced motion tidak ditemukan.'

$trackedSvn = & git -C $RepositoryRoot ls-files 2>$null | Where-Object { $_ -match '(^|/)\.svn(/|$)' }
if ($trackedSvn) { $failures.Add('Metadata .svn terlacak Git: ' + ($trackedSvn -join ', ')) }

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output ('OK: {0} layar, feature toggle, role context, workspace course, error boundary, responsive CSS, dan isolasi .svn tervalidasi.' -f $requiredPages.Count)
