-- Snapshot UOM per baris Permintaan Pembelian (PR).
-- Kolom utama biasanya ditambahkan hbm2ddl.auto=update, tetapi migrasi ini
-- wajib dijalankan pada instalasi yang memakai hbm2ddl=none dan memastikan
-- tabel audit Envers mempunyai bentuk yang sama.

BEGIN;

ALTER TABLE asset.permintaan_pengadaan_master_asset_detail
    ADD COLUMN IF NOT EXISTS satuan_input_id bigint,
    ADD COLUMN IF NOT EXISTS satuan_input_nama varchar(100),
    ADD COLUMN IF NOT EXISTS faktor_ke_dasar double precision;

ALTER TABLE asset.permintaan_pengadaan_master_asset_detail_aud
    ADD COLUMN IF NOT EXISTS satuan_input_id bigint,
    ADD COLUMN IF NOT EXISTS satuan_input_nama varchar(100),
    ADD COLUMN IF NOT EXISTS faktor_ke_dasar double precision;

COMMIT;
