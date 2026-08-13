# Implementasi Roadmap Retail 1–9

Dokumen ini adalah gerbang teknis, bukan izin deploy produksi.

1. **Topup manual:** perbaikan default cara pembayaran/jenis tabungan dari SVN r77380 tetap dipertahankan. Topup baru dilindungi idempotency key agar retry tidak menggandakan saldo.
2. **Staging:** contoh konfigurasi, masking data, guard anti-produksi, dan runbook tersedia di folder `staging`.
3. **Smoke test:** `smoke-retail.ps1` menguji login, katalog, rekonsiliasi, preview impor (tanpa komit), serta—hanya dengan `-AllowWrites`—topup, penjualan, retur, dan pembatalan beserta retry exactly-once.
4. **Migrasi berversi:** `RetailDatabaseMigrations` memakai advisory lock, checksum immutable, dan `public.ais_schema_history`. Ini adalah jembatan aman sampai build legacy dimigrasikan ke Flyway/Liquibase.
5. **Observability:** sampler lima-menit menyimpan query aktif di atas satu detik selama 30 hari. Endpoint `error_log_health` menyajikan jumlah error, aktivitas DB, sampel, dan `pg_stat_statements` bila tersedia. Menu Log Error klien dapat menampilkan serta menyalin detail.
6. **Idempotensi:** tabel `retail_request_idempotency` dipakai topup, retur penjualan, retur pembelian, dan pembatalan. Penjualan tetap memakai `kodeUnik` yang sudah berlaku sebelumnya.
7. **Inventory ledger:** `koperasi.inventory_movement` bersifat append-only dan berjalan dalam mode **shadow audit**; stok legacy tetap menjadi sumber operasional selama masa transisi.
8. **Hook transaksi:** pembelian/kulakan, penjualan, retur penjualan yang kembali ke stok, retur pembelian, dan pembatalan penjualan dicatat ke ledger. Produk lama memperoleh opening balance otomatis saat peristiwa pertama.
9. **Rekonsiliasi:** endpoint dan tab Produk → Rekonsiliasi Stok membandingkan `produk.stok` dengan jumlah ledger, memakai query server-side 15 baris per halaman dan default hanya menampilkan selisih.

## Strategi aktivasi

1. Backup database dan verifikasi restore di staging.
2. Deploy ke staging; restart satu node lebih dahulu agar migrasi tercatat.
3. Periksa `public.ais_schema_history` dan log startup.
4. Jalankan smoke test baca, lalu smoke test tulis memakai fixture staging.
5. Lakukan transaksi representatif dan pastikan rekonsiliasi nol.
6. Jalankan minimal satu siklus operasional staging. Ledger tetap shadow dan tidak mengubah stok.
7. Baru jadwalkan produksi dengan host/path/backup/rollback yang disetujui.

## Rollback

- Rollback aplikasi: kembalikan WAR/binary sebelumnya. Tabel baru aman dibiarkan karena belum menjadi sumber stok utama.
- Jangan drop tabel ledger/idempotensi saat rollback darurat; keduanya mengandung bukti audit.
- Bila migrasi gagal, transaksi migrasi otomatis rollback dan aplikasi mencatat exception. Jangan mengubah checksum versi lama; perbaikan wajib menjadi versi migrasi baru.
- Jika rekonsiliasi menemukan selisih, jangan menimpa stok otomatis. Telusuri dokumen sumber dan buat adjustment yang diaudit setelah penyebab disetujui supervisor.

