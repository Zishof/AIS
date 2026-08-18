-- ============================================================================
-- Migrasi WAJIB sebelum deploy fitur Unit Usaha per Toko.
--
-- Kolom BARU koperasi.toko.unit_usaha_json ditambahkan otomatis ke tabel
-- utama oleh hbm2ddl.auto=update saat server start, TETAPI Envers TIDAK
-- menyinkron kolom baru ke tabel audit entitas @Audited yang sudah ada
-- (gotcha terdokumentasi di javadoc Toko.java). Tanpa ALTER di bawah, SETIAP
-- update baris toko gagal menulis baris auditnya.
--
-- Jalankan SEKALI di database produksi SEBELUM Tomcat di-restart dengan
-- build yang memuat fitur ini:
--   psql -d <database_ais> -f migrasi_toko_unit_usaha_audit.sql
-- ============================================================================

ALTER TABLE new_audit.toko__audit ADD COLUMN unit_usaha_json text;
