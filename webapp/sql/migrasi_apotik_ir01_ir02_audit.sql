-- Migrasi WAJIB sebelum deploy IR-01/IR-02 (modernisasi UI/UX Apotik).
--
-- hbm2ddl=update menambah kolom baru ke tabel UTAMA, tetapi TIDAK selalu ke
-- tabel audit Envers di schema new_audit. Bila tabel audit ketinggalan kolom,
-- INSERT audit gagal -> flush rollback -> DATA TIDAK TERSIMPAN.
-- Jalankan skrip ini SEBELUM restart Tomcat produksi.

-- IR-01: atribut obat pada profil item apotik.
ALTER TABLE sirs.apotik_item_profile ADD COLUMN IF NOT EXISTS bentuk_sediaan varchar(60);
ALTER TABLE sirs.apotik_item_profile ADD COLUMN IF NOT EXISTS kekuatan varchar(60);
ALTER TABLE sirs.apotik_item_profile ADD COLUMN IF NOT EXISTS high_alert boolean;
ALTER TABLE sirs.apotik_item_profile ADD COLUMN IF NOT EXISTS cold_chain boolean;

ALTER TABLE new_audit.apotik_item_profile__audit ADD COLUMN IF NOT EXISTS bentuk_sediaan varchar(60);
ALTER TABLE new_audit.apotik_item_profile__audit ADD COLUMN IF NOT EXISTS kekuatan varchar(60);
ALTER TABLE new_audit.apotik_item_profile__audit ADD COLUMN IF NOT EXISTS high_alert boolean;
ALTER TABLE new_audit.apotik_item_profile__audit ADD COLUMN IF NOT EXISTS cold_chain boolean;

-- IR-02: status lot pada batch/kedaluwarsa. NULL diperlakukan ELIGIBLE oleh
-- entity, jadi seluruh baris lama tetap layak jual tanpa backfill.
ALTER TABLE sirs.kadaluarsa ADD COLUMN IF NOT EXISTS status_lot varchar(24);
ALTER TABLE new_audit.kadaluarsa__audit ADD COLUMN IF NOT EXISTS status_lot varchar(24);

-- IR-07 TIDAK butuh migrasi: sirs.apotik_pembayaran_transaksi adalah tabel
-- BARU sehingga hbm2ddl membuatnya sendiri berikut tabel auditnya.
