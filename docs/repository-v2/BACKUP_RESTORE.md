# Backup, restore, dan pemulihan bencana Repository AIS

Dokumen ini adalah runbook operator. Jangan menjalankan restore langsung ke produksi. Uji setiap
backup pada database kosong dan storage karantina, lalu cocokkan jumlah record, checksum, akses,
tenant, dan identifier sebelum cutover.

## Sasaran operasional

- RPO awal: maksimum 24 jam untuk database dan storage. Sesuaikan dengan kebijakan institusi.
- RTO awal: maksimum 8 jam untuk layanan baca publik. Integrasi eksternal dapat dipulihkan setelahnya.
- Simpan sedikitnya satu salinan lokal terenkripsi, satu salinan media/host berbeda, dan satu salinan
  di lokasi berbeda. Backup pada disk yang sama dengan storage bukan perlindungan bencana.
- Database dan storage harus berasal dari jendela backup yang sama. Selama backup besar, hentikan
  perubahan deposit atau gunakan snapshot storage yang konsisten.

## Membuat backup

1. Pastikan `pg_dump`, `tar`, dan `sha256sum` tersedia.
2. Tentukan `DATABASE_URL`, `AIS_REPOSITORY_STORAGE`, dan `REPOSITORY_BACKUP_DIR` melalui secret
   manager atau sesi operator; jangan simpan kata sandi dalam source maupun history shell.
3. Jalankan `sh docs/repository-v2/repository-backup.sh`.
4. Jalankan `sh docs/repository-v2/repository-restore-verify.sh /path/backup`.
5. Salin direktori hasil secara atomik ke media sekunder dan verifikasi `SHA256SUMS` lagi.
6. Catat waktu, operator, ukuran, hasil verifikasi, versi aplikasi, dan tenant yang tercakup.

## Uji restore terisolasi

1. Siapkan database PostgreSQL kosong yang tidak dapat diakses aplikasi produksi.
2. Siapkan storage karantina kosong; jangan gunakan `/`, home pengguna, atau storage produksi.
3. Verifikasi checksum dengan script non-destruktif.
4. Tampilkan isi dump memakai `pg_restore --list repository-db.dump` dan periksa sumbernya.
5. Restore ke database kosong menggunakan akun terbatas:
   `pg_restore --no-owner --no-privileges --dbname="$RESTORE_DATABASE_URL" repository-db.dump`.
6. Ekstrak storage hanya ke direktori karantina yang telah diperiksa:
   `tar -C "$RESTORE_STORAGE_QUARANTINE" -xzf repository-storage.tar.gz`.
7. Jalankan aplikasi staging dengan kedua target karantina. Biarkan Hibernate memvalidasi/mengelola
   schema; jangan menulis DDL manual ke produksi.
8. Jalankan `validate-repository-server.sh`, uji login per peran, OAI-PMH, pencarian, detail,
   pratinjau, unduhan, checksum, dan fixity.

## Kriteria penerimaan restore

- Jumlah item, koleksi, metadata, contributor, bitstream, workflow event, preference, notification,
  usage event, dan integration event sesuai manifest operasional.
- Tidak ada tenant yang dapat membaca record tenant lain.
- Semua identifier OAI tetap sama dan tidak duplikat; DOI/Handle mengarah ke landing page benar.
- Semua berkas utama ditemukan; checksum sama; tidak ada berkas terinfeksi yang menjadi publik.
- Record embargo, restricted, metadata-only, withdrawn, dan tombstone mempertahankan statusnya.
- Scheduler sinkron dan alert dinyalakan hanya setelah data serta konfigurasi diverifikasi.

## Pemulihan bencana

1. Bekukan perubahan dan simpan log insiden.
2. Tentukan titik pemulihan terakhir yang checksum-nya valid.
3. Bangun database dan storage baru; jangan menimpa sumber rusak sebelum investigasi selesai.
4. Pulihkan rahasia melalui secret manager, bukan dari backup aplikasi.
5. Validasi tenant, RBAC, OAI, download, fixity, dan integrasi di alamat sementara.
6. Lakukan cutover DNS/proxy setelah acceptance criteria terpenuhi.
7. Pantau HTTP 5xx, login, query lambat, storage, scheduler, OAI, serta alert sekurangnya 24 jam.
8. Dokumentasikan kehilangan data terhadap RPO dan tindakan pencegahan berulang.

## Retensi dan latihan

Gunakan retensi contoh 7 backup harian, 5 mingguan, dan 12 bulanan bila sesuai kebijakan. Jangan
menghapus satu-satunya backup valid. Lakukan restore drill minimal setiap tiga bulan dan setiap ada
perubahan besar pada schema, storage, enkripsi, atau versi PostgreSQL. Bukti keberhasilan backup
tanpa bukti restore tidak cukup untuk menyatakan layanan dapat dipulihkan.
