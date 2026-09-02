-- Migrasi grup item biaya sekolah. Jalankan sebelum restart build baru.
-- Tabel audit entitas baru dibuat oleh hbm2ddl; kolom audit entitas lama harus
-- ditambah eksplisit karena Envers tidak mengubah tabel audit yang sudah ada.

CREATE TABLE IF NOT EXISTS sekolah.grup_item_biaya_sekolah (
    id bigserial PRIMARY KEY,
    kode varchar(255) NOT NULL,
    nama varchar(255) NOT NULL,
    keterangan varchar(255),
    aktif boolean,
    yayasan_id bigint,
    sekolah_id bigint,
    oleh varchar(255),
    oleh_id varchar(255),
    tanggal_dirubah timestamp without time zone
);

CREATE INDEX IF NOT EXISTS idx_grup_item_biaya_sekolah_sekolah
    ON sekolah.grup_item_biaya_sekolah (sekolah_id, aktif, kode);

ALTER TABLE sekolah.item_biaya_sekolah
    ADD COLUMN IF NOT EXISTS grup_item_biaya_sekolah_id bigint;

CREATE INDEX IF NOT EXISTS idx_item_biaya_sekolah_grup
    ON sekolah.item_biaya_sekolah (grup_item_biaya_sekolah_id);

DO $$
BEGIN
    IF to_regclass('new_audit.item_biaya_sekolah__audit') IS NOT NULL THEN
        ALTER TABLE new_audit.item_biaya_sekolah__audit
            ADD COLUMN IF NOT EXISTS grup_item_biaya_sekolah_id bigint;
    END IF;
END $$;
