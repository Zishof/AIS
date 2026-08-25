# Observabilitas dan readiness Repository

## Tujuan

Dashboard administrasi `/repository-workspace?view=admin` menjadi titik pemeriksaan operasional
pertama. Pemeriksaan lokal tidak memanggil layanan eksternal dan tidak menampilkan rahasia.

## Sinyal yang tersedia

- antrean review dan item yang melewati tujuh hari;
- sinkronisasi gagal serta waktu sinkronisasi terakhir;
- jumlah search alert aktif, kegagalan alert, dan eksekusi manual;
- kegagalan integrasi dalam 24 jam;
- checksum kosong, scan tertunda, dan berkas terinfeksi;
- writable storage serta ruang bebas;
- kesiapan executable antivirus, analytics salt, dan secret token OAI;
- kualitas metadata, penggunaan manusia/bot, negara, referrer, dan tren 30 hari;
- agregat penilaian manual bantuan: membantu atau perlu diperjelas.

## Ambang tindakan awal

| Sinyal | Peringatan | Tindakan awal |
|---|---:|---|
| Review tertunda | > 0 selama 7 hari | Periksa assignment dan kapasitas reviewer. |
| Alert gagal | > 0 | Baca audit aplikasi, perbaiki query/config, jalankan manual. |
| Integrasi gagal | > 0 dalam 24 jam | Kelompokkan berdasarkan layanan dan request ID. |
| Scan tertunda | > 0 di luar waktu proses normal | Periksa executable scanner dan permission. |
| Storage bebas | < 20% atau proyeksi 30 hari | Tambah kapasitas dan verifikasi backup. |
| Checksum kosong | > 0 | Jalankan pemeriksaan fixity pada data uji dahulu. |
| Bantuan perlu diperjelas | meningkat | Tinjau komentar, revisi bab, perbarui tanggal tinjau. |

## Runbook singkat

1. Catat waktu, tenant, request ID, gejala, dan perubahan terakhir.
2. Periksa dashboard tanpa menjalankan aksi mutasi.
3. Bedakan masalah storage, database, scanner, scheduler, atau integrasi eksternal.
4. Gunakan tombol retry/manual hanya setelah penyebab dan dampaknya dipahami.
5. Setelah pulih, verifikasi satu alur publik dan satu alur pengguna berwenang.
6. Dokumentasikan penyebab, tindakan, bukti pemulihan, serta pencegahan pengulangan.

Jangan menyalin token, kata sandi, payload sensitif, atau alamat IP mentah ke tiket. Gunakan request
ID dan nilai yang sudah disamarkan. Prosedur backup/restore terpisah tersedia di
`BACKUP_RESTORE.md`.
