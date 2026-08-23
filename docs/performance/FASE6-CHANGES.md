# Fase 6 — JasperReports: virtualizer & batas concurrency (2026-08-19)

## Perubahan
| File | Perubahan |
|---|---|
| `ais/action/report/ReportThrottle.java` (BARU) | Semaphore global adil (FIFO) `report_maks_paralel` (default 4) dengan tunggu `report_tunggu_antrian_detik` (default 120) lalu pesan sibuk ramah; `JRSwapFileVirtualizer` per job — halaman > `report_virtualizer_max_halaman_memori` (default 200) di-swap ke `java.io.tmpdir/ais-report-swap`; matikan via konfigurasi `report_virtualizer_nonaktif`=aktif; `cleanup()` di finally SETELAH export |
| `ais/action/report/Report.java` | (1) `generateFileReportCore`: fill+export+retry dibungkus izin semaphore + virtualizer, dilepas di `finally`; (2) `generateFileImageReport` (render JPG, fill kedua + ≤100 BufferedImage halaman) juga ambil izin; (3) `pesanErrorLaporan` meneruskan pesan throttle apa adanya agar pengguna tahu cukup coba ulang |

## Konfigurasi baru (semua punya default aman = perilaku konservatif)
- `report_maks_paralel` = 4 — naikkan/turunkan lewat load test.
- `report_tunggu_antrian_detik` = 120.
- `report_virtualizer_max_halaman_memori` = 200.
- `report_virtualizer_nonaktif` = tidak aktif (virtualizer ON). **Rollback runtime instan tanpa deploy: set konfigurasi ini aktif.**

## Verifikasi hipotesis runbook (sudah dimitigasi terdahulu — TANPA perubahan)
- Kompilasi runtime hanya saat JRXML lebih baru dari `.jasper` (compile-to-file, reuse) + lock granular per-nama-file + retry race — sudah ada.
- Preview HTML pendamping: sudah ber-gate konfigurasi (`previewHtmlAktif()`), dilewati untuk mode unduh, dan di-skip di atas 300 halaman — sudah ada.
- `CommonReport` tidak memiliki jalur fill sendiri — seluruh fill lewat `Report.java` (dua titik, keduanya kini terlindungi).

## Validasi
- `javac -source 8 -target 8`: bersih tanpa error (JRSwapFileVirtualizer/JRSwapFile terverifikasi ada di jasperreports-6.4.1).
- Benchmark WAJIB oleh operator (template di `BEFORE_AFTER.md`): report kecil/sedang/besar × {1 job, 4 job paralel, 8 job antre} — ukur durasi, peak heap, Old Gen setelah GC, ukuran `ais-report-swap`, dan **bandingkan output PDF secara visual + jumlah halaman** sebelum–sesudah (virtualizer tidak boleh mengubah isi).
- Smoke test: cetak PDF, XLS, dan satu laporan bergambar (jalur JPG); pastikan file swap di temp terhapus setelah selesai; uji 5+ cetak bersamaan → job ke-5+ antre lalu selesai, bukan error.

## Risiko & rollback
- Virtualizer menambah I/O disk untuk laporan >200 halaman (trade-off disengaja: heap turun). Bila ada laporan yang error/berubah tampilan → set `report_virtualizer_nonaktif`=aktif (tanpa deploy) dan laporkan template-nya.
- Bila 4 slot paralel terasa kurang di instalasi besar → naikkan `report_maks_paralel` bertahap sambil pantau heap/koneksi.
- Rollback penuh: `svn merge -c -<rev>` untuk kedua file.
