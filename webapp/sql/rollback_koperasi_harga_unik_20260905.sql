-- Rollback untuk migrasi_koperasi_harga_unik_20260905.sql.
-- Jalankan hanya bila index unik ini ternyata menghalangi alur produksi yang
-- sah (mis. ditemukan kasus bisnis lain yang belum tercakup skrip migrasi).
-- Menghapus index TIDAK menghapus data -- baris yang sudah terlanjur tersimpan
-- duplikat (bila ada, sebelum migrasi diterapkan) tidak disentuh oleh rollback ini.

BEGIN;

DROP INDEX IF EXISTS koperasi.uq_koperasi_harga_beli_supplier;
DROP INDEX IF EXISTS koperasi.uq_koperasi_harga_jual_umum;
DROP INDEX IF EXISTS koperasi.uq_koperasi_harga_jual_customer;

COMMIT;
