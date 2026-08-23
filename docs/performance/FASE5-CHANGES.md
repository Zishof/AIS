# Fase 5 — ZK, HttpSession, dan state UI (2026-08-19)

## Temuan utama: server push dinyalakan tanpa pernah dimatikan (41 berkas)

ZK 5.5 memakai **PollingServerPush**: begitu push aktif, browser terus-menerus mengirim request
polling, dan **setiap polling menahan satu thread Tomcat** (`http-nio-...-exec-N`) sampai timeout,
lalu polling lagi. Selama tab terbuka, hal ini berlangsung SELAMANYA — walau tugas aslinya
(cetak laporan, muat dashboard) sudah selesai dalam hitungan detik.

`AsyncTaskManager` sudah pernah diperbaiki dengan reference counting untuk jalurnya sendiri
(insiden ledakan ~15.000 thread http-nio, terdokumentasi di kelas itu). Namun audit Fase 5
menemukan **41 berkas lain** memakai pola lama:

```java
if (!desktop.isServerPushEnabled()) desktop.enableServerPush(true);   // ON
new Thread(new Runnable() { ... }).start();                            // thread MENTAH
// tidak pernah ada enableServerPush(false)
```

Dua masalah sekaligus: push bocor, dan thread mentah per operasi (tak berbatas, tak bernama,
tidak berhenti saat redeploy).

### Perbaikan: satu jalur terkelola
Ditambahkan `AsyncTaskManager.jalankanDenganPush(Desktop, Runnable)` yang **memakai ulang**
mekanisme yang sudah terbukti di kelas itu:
- push dinyalakan lewat reference counting (aman bila beberapa tugas berjalan pada desktop sama);
- tugas dijalankan pada pool **daemon berbatas** milik AsyncTaskManager (≤16 thread, sudah
  di-shutdown rapi di `contextDestroyed` sejak Fase 1) — bukan thread mentah;
- push **DILEPAS di `finally`** begitu tugas selesai atau gagal.

Konversi per berkas menjadi satu baris:
```java
// sebelum
if (!desktop.isServerPushEnabled()) desktop.enableServerPush(true);
new Thread(new Runnable() { ... }).start();

// sesudah
ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() { ... });
```

## Batas server push per sesi (`zk.xml`)

`max-pushes-per-session` **-1 (tak terbatas) → 15**, disamakan dengan `max-desktops-per-session`
sehingga paling banyak satu push aktif per tab. Ini membatasi kerusakan bahkan bila ada jalur
yang belum dikonversi. `max-desktops-per-session=15` dan `max-requests-per-session=10` dinilai
sudah wajar untuk aplikasi ber-UI tab dan TIDAK diubah.

## Yang sengaja TIDAK diubah (dengan alasan)

1. **`max-upload-size=907200` KB (±886 MB)** dibiarkan. Aplikasi punya fitur unggah yang
   menyebut **"maks 500 Mb"** (video e-learning), jadi menurunkannya akan mematahkan fitur nyata.
   Perlu keputusan pemilik + pengukuran: apakah 500 MB benar dipakai, dan apakah kode pembacanya
   memakai stream (aman) atau `getByteData()` (memuat seluruh berkas ke heap — berbahaya).
2. **Properti ambang spill-to-disk ZK tidak ditambahkan.** Nama properti yang lazim dipakai
   (`org.zkoss.zk.ui.sys.FileSizeThreshold`) TIDAK ditemukan di jar ZK 5.5 yang dibundel, dan
   runbook melarang memasang parameter yang tidak didukung versi. Perlu verifikasi versi ZK dulu.
3. **Timeout sesi** sudah dikelola programatik di `SessionCounter` (`session_timeout_dosen` 60 mnt,
   `session_timeout_admin` 120 mnt) sehingga tidak ada `<session-timeout>` di web.xml — ini
   disengaja, bukan kelalaian.

## Validasi
- `AsyncTaskManager.jalankanDenganPush` kompilasi **bersih**.
- `zk.xml` tervalidasi **XML VALID** setelah perubahan.
- Konversi percontohan (`LaporanGelombangSidang`) kompilasi bersih sebelum pola disebar.
- **Belum diukur:** jumlah thread http-nio dan push aktif sebelum vs sesudah. WAJIB diuji operator.

### Smoke test wajib
1. Buka beberapa layar laporan/dashboard yang dikonversi → laporan tetap tampil normal
   (progress bar / hasil PDF muncul seperti biasa).
2. Setelah laporan selesai, biarkan tab terbuka beberapa menit lalu pantau jumlah thread
   `http-nio-*-exec-*` (mis. `jstack <PID> | grep -c http-nio`) — seharusnya TIDAK terus bertambah
   seperti sebelumnya.
3. Buka 3–5 tab sekaligus lalu jalankan laporan di beberapa tab bersamaan → pastikan tidak ada
   error "too many pushes" (bila muncul, naikkan `max-pushes-per-session` bertahap).
4. Soak test: login → buka/tutup banyak tab → logout → tunggu timeout; pastikan jumlah thread dan
   sesi kembali ke baseline.

## Rollback
Per berkas via `svn merge -c -<rev>`. `zk.xml`: kembalikan `max-pushes-per-session` ke `-1`.
`jalankanDenganPush` bersifat aditif — berkas yang belum dikonversi tetap berjalan seperti semula.
