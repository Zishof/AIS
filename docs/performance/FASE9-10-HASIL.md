# Fase 9 & 10 — Hasil pengerjaan (2026-08-19)

Dokumen ini menggantikan bagian Fase 9-10 pada `FASE7-10-CHANGES.md` yang sebelumnya masih berupa rencana. Di sini yang dicatat adalah **apa yang benar-benar dikerjakan dan apa yang terbukti tidak perlu dikerjakan**.

---

## FASE 9 — Efisiensi kode: hasilnya NEGATIF (dan itu temuan yang berguna)

Runbook melarang optimasi berbasis asumsi, jadi saya cari bukti dulu sebelum mengubah apa pun. Hasil pemindaian menyeluruh:

| Kandidat | Temuan awal | Setelah diperiksa satu per satu |
|---|---:|---|
| `Pattern.compile` di dalam method | 7 | Semua di jalur jarang, bukan hot path |
| `new SimpleDateFormat` | 283 | Lihat analisis di bawah |
| `.matches()` / `.replaceAll()` | 1.457 | Tersebar, tidak terkonsentrasi di jalur panas |
| `System.out.println` | 6.049 | **Hanya 1** yang berada di dalam method `render(Row ...)` |

### Kenapa 283 SimpleDateFormat itu ternyata bukan masalah

Deteksi awal saya menandai 12 di antaranya "berada di dalam loop". Setelah dibaca satu per satu, **semuanya false positive atau memang benar demikian**:

- `DasbordApresiasi:572`, `DasbordPelanggaran:633`, `DashboardReportKit:625` — formatter dibuat **sebelum** loop; detektor tertipu oleh loop header yang sudah tertutup di atasnya.
- `GenericCrudValueConverter:57`, `GenericRevisiHelper:4000`, `LaporanJspUtil:48` — loop-nya mengiterasi **array pola tanggal** (3–6 pola) dan membuat satu formatter per pola untuk mencoba parsing. Ini memang harus satu formatter per pola, dan loop berhenti pada pola pertama yang cocok. Bukan pemborosan.
- `DasbordCatatan:1053` — satu-satunya pembuatan formatter di dalam loop nyata, dengan **maksimal 5 iterasi**. Dampaknya dapat diabaikan.

### Kesimpulan Fase 9

**Tidak ada perubahan kode yang dilakukan**, dan itu keputusan yang disengaja. Mengubah puluhan berkas untuk penghematan yang tidak terukur justru melanggar dua aturan runbook sekaligus: *"Jangan menukar readability dan correctness demi micro-optimization yang tidak terukur"* dan *"Jangan mengklaim hasil yang tidak diukur"*.

Hal ini juga berarti sesuatu yang positif: **beban berat aplikasi ini bukan pada alokasi objek di hot path**, melainkan pada hal-hal struktural yang sudah ditangani Fase 1–7 (kebocoran sesi/push/koneksi, cache tanpa batas, preload berlebihan, laporan tanpa virtualizer, kelas JSP tanpa batas).

Langkah berikutnya yang benar untuk Fase 9 — **butuh profiler, bukan pencarian teks**: jalankan allocation sampling (mis. async-profiler atau JFR) pada 3 layar tersibuk saat beban nyata, lalu optimalkan hanya hot path yang benar-benar muncul di hasilnya.

Yang juga tetap tidak dikerjakan (dengan alasan): pemecahan `Common.java` (21.019 baris) — manfaatnya maintainability, bukan RAM, sementara risikonya tinggi karena kelas itu disentuh hampir seluruh aplikasi.

---

## FASE 10 — Observability: DIKERJAKAN

Masalahnya nyata: setelah 10 fase optimasi, **tidak ada satu pun angka di aplikasi yang bisa membuktikan perbaikannya bekerja**. Snapshot performa yang sudah ada (`PerformaSnapshotUtil`) mencatat thread, deadlock, memori, GC, dan kelas — tetapi tidak satu pun metrik yang memetakan ke pekerjaan Fase 1–7.

### Yang ditambahkan

Bagian baru **"METRIK OPTIMASI (Fase 1-10)"** di dalam snapshot performa, memakai sumber data yang sudah tersedia:

| Baris metrik | Sumber | Membuktikan |
|---|---|---|
| `Pool c3p0` per factory (total/sibuk/idle/menunggu) | `C3P0Registry` via refleksi | **Fase 4** — kapasitas 6.100 → 115 koneksi |
| `Pool async` (aktif/pool/antre/selesai) | `AsyncTaskManager.statistikPool()` | **Fase 1 & 5** — pool berbatas, tugas latar terkelola |
| `Sesi online` | `SecurityFilter.dataOnline` | **Fase 1** — registry sesi dibersihkan terpusat |
| `Slot laporan` (bebas/menunggu) | `ReportThrottle.statistik()` | **Fase 6** — batas laporan paralel berlaku |
| `Cache memori` (region/total baris) | `MemoryCacheUtil.statistik()` | **Fase 2 & 3** — preload/cache terkendali |

### Prinsip implementasi

- **Tidak boleh menggagalkan snapshot.** Setiap pembacaan dibungkus `try/catch` sendiri; bila satu sumber tidak tersedia, barisnya hanya berisi "tidak terbaca" dan sisanya tetap jalan.
- **Tidak mengikat versi c3p0.** Pool dibaca lewat refleksi (`C3P0Registry`), mengikuti pola defensif yang sudah dipakai kelas ini untuk shutdown Ehcache.
- **Tidak ada data pribadi maupun kredensial** — hanya nama pool dan angka.
- **Tidak menambah beban tetap.** Metrik hanya dibaca saat snapshot dijalankan (interval dikonfigurasi `performa_log_interval_menit`, default 5 menit), bukan per request.

### Berkas yang diubah
- `ais/common/PerformaSnapshotUtil.java` — bagian metrik + helper refleksi
- `ais/common/AsyncTaskManager.java` — `statistikPool()`
- `ais/action/report/ReportThrottle.java` — `statistik()`
- `ais/common/MemoryCacheUtil.java` — `statistik()`

Semua **kompilasi bersih** dan ter-mirror ke `src/main/src`.

### Cara memakai
Buka layar snapshot performa aplikasi (atau tunggu perekaman berkala). Bagian `--- METRIK OPTIMASI (Fase 1-10) ---` akan muncul di laporan teks. Bandingkan sebelum dan sesudah deploy build baru untuk mengisi tabel di `BEFORE_AFTER.md`.

---

## Catatan proses yang perlu diketahui

1. **Satu berkas sempat rusak saat pengerjaan.** Escape `\n` termakan shell ketika menambahkan bagian metrik, sehingga `PerformaSnapshotUtil.java` tidak dapat dikompilasi. Terdeteksi langsung oleh kompilasi, dipulihkan dengan `svn revert`, lalu diterapkan ulang lewat berkas skrip (bukan heredoc) dengan escape aman. Kompilasi akhir bersih.
2. **Working copy dipakai bersama proses lain.** Method `statistikPool()` yang sudah saya tambahkan sempat hilang tertimpa, terdeteksi saat kompilasi, dan ditambahkan ulang. Perlu diperhatikan bila ada dua sesi kerja berjalan bersamaan pada pohon kode yang sama.
3. **Semua angka masih "belum diukur".** Fase 10 menyediakan alat ukurnya; pengisian angka sebelum–sesudah tetap perlu dijalankan operator pada lingkungan nyata.
