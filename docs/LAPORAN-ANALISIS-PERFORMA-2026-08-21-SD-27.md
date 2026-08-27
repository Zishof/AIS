# Analisis Performa eCampus 21-27 Agustus 2026

## Ringkasan data

- Snapshot dianalisis: 284 snapshot bermasalah.
- Thread maksimum: 956; 116 snapshot mencatat lebih dari 800 thread.
- Deadlock: tidak ditemukan pada seluruh snapshot.
- Heap maksimum: 50%; tidak pernah mencapai ambang tekanan memori 85%.
- Sesi online maksimum: 5.334.
- Cache maksimum: 2.475.587 baris; heap/Old Gen tetap stabil dan tidak menunjukkan kebocoran kritis.

## Kelompok akar masalah

1. Antrean event ZK per desktop mulai konsisten pada 21-08-2026 15:19:56. Sampai 63 request dari desktop/tab yang sama menunggu `UiEngineImpl.doActivate`. `FilterJSP` muncul karena merupakan filter pembungkus semua request, bukan sumber lock.
2. Lock convoy pembaruan `Kegiatan` terlihat pada 27-08-2026 09:47:06 dan memuncak 41 thread. Sebagian merupakan tabrakan striped lock antar-ID saat sinkronisasi pembayaran massal.
3. Pemuatan ringkasan materi/diskusi membuat fixed thread pool per request hingga 230 worker. Beberapa request bersamaan dapat menaikkan thread dan menghabiskan koneksi database.
4. Lonjakan total thread di atas 800 pada 26-27 Agustus didominasi worker AJP/Tomcat dalam `TIMED_WAITING` pada task queue. Kondisi ini bukan deadlock atau kebocoran aktif, sehingga total thread saja tidak layak menentukan status kesehatan.
5. Frekuensi G1 Young GC masih sejalan dengan heap yang rendah-sedang; tidak ada Full/Old GC dan tidak ada bukti heap mendekati penuh.

## Perbaikan r78407

- `MateriDanKomentarHelper`: paralelisme materi/diskusi dibatasi lewat `DbThreadPool.safe` (default 16, batas keras 32); executor dihentikan secara deterministik dan dipaksa berhenti saat timeout/interupsi.
- `KegiatanPersistenceHelper`: striped lock diperbesar dari 64 menjadi 1024 agar ID kegiatan berbeda tidak mudah saling memblokir; ID yang sama tetap serial.
- `PerformaSnapshotUtil`: antrean aktivasi ZK dikenali secara khusus, tidak dihitung sebagai worker aktif, tetapi tetap dilaporkan sebagai kontensi; class filter infrastruktur tidak lagi menutupi handler bisnis pada daftar top class.
- `PerformaLog`: total worker idle di atas 800 tidak lagi sendirian menaikkan status menjadi Perhatian. Deadlock, heap/Metaspace, thread BLOCKED, dan kontensi satu lock tetap menjadi penentu.

## Verifikasi

- Full Ant compile berhasil untuk 7.286 source Java dengan target/source Java 1.6.
- Tidak ada perubahan skema database.
- Tidak ada fungsi bisnis yang dimatikan.
- Kode tetap tanpa lambda, Stream API, diamond operator, try-with-resources, atau fitur Java 8+.

## Rekomendasi operasional

- Pantau snapshot baru setelah deploy dan bandingkan `grup thread aktif`, bukan hanya total thread.
- Jika antrean ZK kembali tinggi, telusuri handler RUNNABLE dari desktop yang sama dan cegah klik ganda pada operasi berat.
- Pertahankan `max_thread_db_paralel` pada 16; naikkan hanya setelah mengukur kapasitas c3p0 dan database.
- Tuning `maxThreads` AJP/Tomcat dilakukan di konfigurasi server, bukan di aplikasi. Nilai besar boleh dipertahankan bila mayoritas worker idle dan latensi request normal.
