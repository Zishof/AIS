-- ============================================================================
-- Migrasi WAJIB sebelum deploy fitur Grup Produk (Harga Terpusat).
--
-- Kolom BARU koperasi.produk.grup_produk ditambahkan otomatis ke tabel utama
-- oleh hbm2ddl.auto=update saat server start, TETAPI Envers TIDAK menyinkron
-- kolom baru ke tabel audit entitas @Audited yang sudah ada (gotcha
-- terdokumentasi di javadoc Toko.java / Produk.getGrupProduk()). Tanpa ALTER
-- di bawah, SETIAP update baris produk gagal menulis baris auditnya.
--
-- Tabel baru koperasi.grup_produk + tabel auditnya dibuat otomatis (entitas
-- baru, bukan kolom baru) -- tidak perlu ditangani di sini.
--
-- Jalankan SEKALI di database produksi SEBELUM Tomcat di-restart dengan
-- build yang memuat fitur ini:
--   psql -d <database_ais> -f migrasi_grup_produk_audit.sql
-- ============================================================================

ALTER TABLE new_audit.produk__audit ADD COLUMN grup_produk int8;
