# Fase 3 — Startup warm-up & preload (2026-08-19)

## Perubahan (`ais/common/InitDataHelper.java`, mirror tersinkron)
1. **Preload seluruh tabel Pegawai DIHAPUS** — hipotesis runbook terkonfirmasi: `handlePegawai` lama mem-paging SELURUH tabel (50/halaman tanpa batas atas) ke cache memory, menahan ribuan entity graph permanen. Pegawai kini mengikuti pola tabel besar Tbmuser/Dosen: warm-start hanya id dengan riwayat akses nyata 3 hari (`EntityAccessCache`), sisanya on-demand.
   - **Bukti aman:** semua konsumen memakai `ConstantValues.ambil(Pegawai.class.getName(), id)` yang punya fallback DB (`session.get`) saat cache miss; cabang skip tetap membuat map kosong sehingga `classExist` true dan routing pembacaan tidak berubah.
2. **Ambang "kelas kecil boleh full-load" jadi configurable** — `preload_maks_baris_kecil` (default 100 = perilaku lama; 0 = matikan full-load kelas kecil).

## Verifikasi arsitektur startup (TANPA perubahan — sudah baik)
- Daftar class preload adalah **allowlist eksplisit hardcoded** di `InitData.initClasses(...)` (ratusan class master dienumerasi manual) — bukan "cache semua class" buta; heuristik per-class besar (Mahasiswa 3 angkatan, Siswa 6 tahun, Perkuliahan max 5000, Item max 500) sudah ada.
- Init berat sudah berjalan di thread latar (`AIS-Init-Data`, daemon) dengan saklar `-Dais.init.async=false`; Tomcat siap melayani tanpa menunggu.
- `statement_timeout_init_ms` (default 120 dtk) membatasi COUNT/load per class; COUNT gagal → class di-skip, bootstrap lanjut.
- `InitData.executor` fixed 5 thread + `awaitTermination` configurable (`await_init_executor_detik`) + `shutdownNow` fallback — jauh di bawah kapasitas pool c3p0 (80).
- Task warm-up opsional (NotifikasiCache, DepositoAro, StokThreshold, RepositorySync, seed SPI/JenisLokasi) masing-masing terisolasi try/catch — gagal tidak menggagalkan startup.
- **`DataUtil.PAKAI_MAPDB_ENTITY` ON = miss tanpa fallback DB di jalur `ambilData(c,key,false)`** — inilah alasan preload tidak boleh dimatikan wholesale; kontrol dilakukan dari sisi pengisian (allowlist + ambang + pola tabel besar).

## Validasi
- Kompilasi bersih (`javac -source 8 -target 8`).
- Smoke test WAJIB sebelum dianggap selesai:
  1. Startup Tomcat → perhatikan log `doInitData`: Pegawai harus tampil "Warm-start ... dari riwayat akses" atau "Skip preload memory ... (tabel besar...)", TIDAK lagi loop "loading data ...Pegawai sebanyak 50, mulai -> N".
  2. Buka layar yang menampilkan pegawai (kepegawaian, penggajian, catatan pegawai, KinerjaAction/BKD) — nama/data pegawai harus tetap tampil (via fallback DB).
  3. Bandingkan waktu startup dan heap setelah init (baseline vs sesudah) — kolom `BEFORE_AFTER.md`.
- Rollback: `svn merge -c -<rev>` file ini saja.

## Ditunda (butuh keputusan/pengukuran)
- Konsolidasi thread startup (initThread/branding/index/warmup pool) — bounded & one-shot; keuntungan kecil, risiko regresi urutan init.
- Penambahan konfigurasi off-switch preload per class — tunggu bukti kebutuhan dari pengukuran startup nyata.
