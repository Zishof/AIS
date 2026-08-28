-- Rollback kompatibilitas aplikasi untuk migrasi UOM 2026-08-28.
-- Jalankan hanya setelah server/aplikasi dikembalikan ke versi lama.
-- Kolom baru sengaja tidak dihapus agar histori dan audit tetap dapat dipulihkan.

BEGIN;
SET LOCAL lock_timeout = '15s';

UPDATE koperasi.produk
SET satuan_pembelian = satuan
WHERE satuan_pembelian IS NULL AND satuan IS NOT NULL;

UPDATE koperasi.pengadaan_produk
SET qty_input = COALESCE(qty_input, qty),
    faktor_konversi = CASE WHEN COALESCE(faktor_konversi, 0) <= 0 THEN 1 ELSE faktor_konversi END,
    harga_beli_satuan_input = COALESCE(harga_beli_satuan_input, hargabelisatuan);

-- Jika penghapusan fisik kolom diwajibkan, lakukan terpisah pada jadwal
-- maintenance setelah backup. Jangan DROP kolom audit/snapshot saat darurat.

COMMIT;
