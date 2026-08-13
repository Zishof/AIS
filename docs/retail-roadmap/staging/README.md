# Lingkungan Staging Retail

Lingkungan staging wajib terpisah dari produksi pada URL, basis data, direktori Tomcat, kredensial, dan penyimpanan berkas. Salinan data produksi hanya boleh dipakai setelah proses masking selesai dan divalidasi.

## Urutan penyiapan

1. Buat database dan user PostgreSQL khusus staging. User aplikasi tidak boleh menjadi superuser.
2. Pulihkan salinan terbaru produksi ke database staging yang tidak dapat diakses publik.
3. Jalankan `mask_staging_data.sql` menggunakan user migrasi, bukan user aplikasi.
4. Isi konfigurasi Tomcat dari `staging.properties.example`; jangan menyimpan secret ke repository.
5. Jalankan aplikasi sekali. `RetailDatabaseMigrations` akan membuat dan mencatat migrasi pada `public.ais_schema_history`.
6. Pastikan URL staging menampilkan penanda visual **STAGING** dan integrasi pembayaran/email/WA menggunakan sandbox atau dinonaktifkan.
7. Salin `smoke-config.example.json` sebagai berkas lokal di luar Git, isi fixture staging, lalu jalankan `smoke-retail.ps1`.

## Gerbang rilis

- Tidak ada data kontak, password, token, PIN, atau nomor identitas produksi yang tersisa.
- Migrasi database seluruhnya berstatus `success=true` dan checksum tidak berubah.
- Smoke test login, topup, penjualan, pembatalan, retur, serta preview impor lulus.
- Uji tulis hanya berjalan dengan `-AllowWrites` dan host yang mengandung kata `staging`, `test`, `localhost`, atau alamat loopback.
- Rekonsiliasi stok tidak menemukan selisih untuk produk yang sudah tercakup ledger. Produk lama yang belum tercakup dicatat sebagai baseline saat mutasi pertama.

Deployment produksi belum boleh dilakukan tanpa host, akun SSH, lokasi Tomcat, prosedur backup, dan prosedur rollback yang disetujui pemilik sistem.

