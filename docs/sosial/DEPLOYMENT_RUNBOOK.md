# Deployment dan Rollback Modul Sosial AIS

## Prasyarat

1. Catat revision SVN, checksum artifact, database target, dan tenant pilot.
2. Jalankan build WAR penuh serta regression test legacy sosial.
3. Backup database dan buktikan restore di non-production.
4. Pastikan semua mutation flag off.
5. Verifikasi kontrak Smartlink create-order, callback signature, source IP, inquiry, expiry, dan settlement.
6. Dapatkan sign-off legal, syariah/amil, finance, security, dan operations.

## Urutan deployment

1. Deploy artifact ke non-production.
2. Start Tomcat dan periksa mapping Hibernate/tabel baru.
3. Periksa servlet `/sosial`, `/sosial-api`, `/SosialSmartlinkCallback`, dan `/sosial-receipt-pdf`.
4. Seed tenant setting, jenis dana, jenis zakat, policy, kategori penerima, program extension, dan role mapping.
5. Jalankan SQL index setelah tabel terbentuk.
6. Aktifkan portal read-only dan transparansi; smoke seluruh legacy CRUD.
7. Aktifkan kalkulator setelah golden cases ditandatangani reviewer.
8. Konfigurasi sedikitnya dua profil Smartlink sandbox, pool tenant/pemetaan jenis dana, callback secret/IP per profil, receipt, lalu aktifkan flag sandbox.
   Migrasikan konfigurasi tunggal lama ke kode profil versioned; jangan menghapus profil lama selama masih ada order terbuka.
9. Jalankan callback test matrix sebelum production collection.
10. Canary satu tenant, satu program, channel terbatas, dan nominal limit konservatif.
11. Pantau minimal dua siklus settlement sebelum ekspansi.

## Uji callback setelah deployment

- Valid success callback.
- Duplicate callback body identik.
- Duplicate gateway transaction ID dengan body berbeda.
- Invalid HMAC.
- Source IP tidak diizinkan.
- Amount mismatch.
- Currency mismatch bila field disediakan.
- Unknown order.
- Expired-then-paid.
- Concurrent callback dan inquiry.
- Redirect browser tanpa callback.
- Receipt dan allocation hanya satu kali.
- Reconciliation exception tercipta untuk mismatch.
- Dua donasi yang dirutekan ke profil berbeda memakai username/endpoint dan callback secret profil masing-masing.
- Callback yang ditandatangani secret profil lain ditolak.

Simpan raw test fixture tanpa secret/PII, response status, database snapshot ter-redaksi, dan correlation ID.

## Rollback

1. Matikan `sosial_public_collection_enabled` dan `sosial_smartlink_enabled`.
2. Pertahankan status/read-only portal bila aman, atau matikan `sosial_portal_enabled`.
3. Kembalikan artifact aplikasi ke versi sebelumnya.
4. Jangan drop tabel yang telah memiliki transaksi.
5. Jangan memperbaiki record paid/posted dengan SQL tanpa correction/reversal event dan approval.
6. Rekonsiliasi seluruh order yang dibuat sebelum rollback dengan gateway dan bank.
