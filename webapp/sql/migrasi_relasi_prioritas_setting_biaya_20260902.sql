-- Menegaskan rantai relasi tagihan:
-- SettingBiaya -> DetailSettingBiaya -> DetailBiaya -> PengaturanPembayaranBulanan
-- SettingBiaya -> SettingBiayaDetail -> DetailBiaya (khusus mahasiswa/calon)
--
-- Aman dijalankan berulang. Backfill hanya dilakukan jika kedua jalur turunan
-- tidak konflik. Baris konflik dilaporkan pada query audit di bagian akhir.

BEGIN;

WITH sumber AS (
    SELECT db.id,
           sbd.setting_biaya AS dari_individual,
           dsb.setting_biaya AS dari_rincian,
           CASE
               WHEN sbd.setting_biaya IS NOT NULL
                    AND (dsb.setting_biaya IS NULL OR dsb.setting_biaya = sbd.setting_biaya)
                   THEN sbd.setting_biaya
               WHEN sbd.setting_biaya IS NULL AND dsb.setting_biaya IS NOT NULL
                   THEN dsb.setting_biaya
               ELSE NULL
           END AS setting_biaya_kanonis
      FROM detail_biaya db
      LEFT JOIN setting_biaya_detail sbd ON sbd.id = db.setting_biaya_detail
      LEFT JOIN detail_setting_biaya dsb ON dsb.id = db.detail_setting_biaya
)
UPDATE detail_biaya db
   SET setting_biaya = sumber.setting_biaya_kanonis
  FROM sumber
 WHERE db.id = sumber.id
   AND db.setting_biaya IS NULL
   AND sumber.setting_biaya_kanonis IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_setting_biaya_pemilihan_prioritas
    ON setting_biaya (jenis_kegiatan, ta, prioritas, id);
CREATE INDEX IF NOT EXISTS idx_setting_biaya_detail_induk_mahasiswa
    ON setting_biaya_detail (setting_biaya, mahasiswa);
CREATE INDEX IF NOT EXISTS idx_setting_biaya_detail_induk_calon
    ON setting_biaya_detail (setting_biaya, biodata_calon_mahasiswa);
CREATE INDEX IF NOT EXISTS idx_detail_setting_biaya_induk_item
    ON detail_setting_biaya (setting_biaya, item_biaya);
CREATE INDEX IF NOT EXISTS idx_detail_biaya_induk_item_semester
    ON detail_biaya (setting_biaya, item_biaya, semester);
CREATE INDEX IF NOT EXISTS idx_detail_biaya_rincian
    ON detail_biaya (detail_setting_biaya);
CREATE INDEX IF NOT EXISTS idx_detail_biaya_individual
    ON detail_biaya (setting_biaya_detail);
CREATE INDEX IF NOT EXISTS idx_pembayaran_bulanan_detail
    ON pengaturan_pembayaran_bulanan (detail_biaya);

-- FK dibuat NOT VALID: semua data baru langsung dijaga konsisten, sedangkan
-- data lama yang orphan masih dapat diaudit dan dibersihkan sebelum VALIDATE.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_dsb_setting_biaya') THEN
        ALTER TABLE detail_setting_biaya
            ADD CONSTRAINT fk_dsb_setting_biaya
            FOREIGN KEY (setting_biaya) REFERENCES setting_biaya(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_sbd_setting_biaya') THEN
        ALTER TABLE setting_biaya_detail
            ADD CONSTRAINT fk_sbd_setting_biaya
            FOREIGN KEY (setting_biaya) REFERENCES setting_biaya(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_db_setting_biaya') THEN
        ALTER TABLE detail_biaya
            ADD CONSTRAINT fk_db_setting_biaya
            FOREIGN KEY (setting_biaya) REFERENCES setting_biaya(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_db_detail_setting_biaya') THEN
        ALTER TABLE detail_biaya
            ADD CONSTRAINT fk_db_detail_setting_biaya
            FOREIGN KEY (detail_setting_biaya) REFERENCES detail_setting_biaya(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_db_setting_biaya_detail') THEN
        ALTER TABLE detail_biaya
            ADD CONSTRAINT fk_db_setting_biaya_detail
            FOREIGN KEY (setting_biaya_detail) REFERENCES setting_biaya_detail(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_ppb_detail_biaya') THEN
        ALTER TABLE pengaturan_pembayaran_bulanan
            ADD CONSTRAINT fk_ppb_detail_biaya
            FOREIGN KEY (detail_biaya) REFERENCES detail_biaya(id) NOT VALID;
    END IF;
END $$;

COMMIT;

-- AUDIT 1: harus 0. Salah satu jalur relasi menunjuk SettingBiaya berbeda.
SELECT db.id AS detail_biaya_id,
       db.setting_biaya AS setting_langsung,
       sbd.setting_biaya AS setting_individual,
       dsb.setting_biaya AS setting_rincian
  FROM detail_biaya db
  LEFT JOIN setting_biaya_detail sbd ON sbd.id = db.setting_biaya_detail
  LEFT JOIN detail_setting_biaya dsb ON dsb.id = db.detail_setting_biaya
 WHERE (sbd.setting_biaya IS NOT NULL AND dsb.setting_biaya IS NOT NULL
        AND sbd.setting_biaya IS DISTINCT FROM dsb.setting_biaya)
    OR (db.setting_biaya IS NOT NULL AND sbd.setting_biaya IS NOT NULL
        AND db.setting_biaya IS DISTINCT FROM sbd.setting_biaya)
    OR (db.setting_biaya IS NOT NULL AND dsb.setting_biaya IS NOT NULL
        AND db.setting_biaya IS DISTINCT FROM dsb.setting_biaya);

-- AUDIT 2: harus 0 untuk tagihan hasil SettingBiaya. Baris manual memang boleh
-- tidak mempunyai ketiga relasi dan perlu dinilai berdasarkan konteks bisnisnya.
SELECT db.id AS detail_biaya_id,
       db.item_biaya,
       db.semester
  FROM detail_biaya db
 WHERE db.setting_biaya IS NULL
   AND (db.detail_setting_biaya IS NOT NULL OR db.setting_biaya_detail IS NOT NULL);

-- AUDIT 3: harus 0. Jadwal bulanan wajib mempunyai DetailBiaya induk.
SELECT ppb.id AS pembayaran_bulanan_id
  FROM pengaturan_pembayaran_bulanan ppb
  LEFT JOIN detail_biaya db ON db.id = ppb.detail_biaya
 WHERE db.id IS NULL;

-- Setelah ketiga audit di atas bersih, jalankan VALIDATE berikut pada jadwal
-- maintenance. Sengaja tidak dijalankan otomatis oleh migrasi ini.
-- ALTER TABLE detail_setting_biaya VALIDATE CONSTRAINT fk_dsb_setting_biaya;
-- ALTER TABLE setting_biaya_detail VALIDATE CONSTRAINT fk_sbd_setting_biaya;
-- ALTER TABLE detail_biaya VALIDATE CONSTRAINT fk_db_setting_biaya;
-- ALTER TABLE detail_biaya VALIDATE CONSTRAINT fk_db_detail_setting_biaya;
-- ALTER TABLE detail_biaya VALIDATE CONSTRAINT fk_db_setting_biaya_detail;
-- ALTER TABLE pengaturan_pembayaran_bulanan VALIDATE CONSTRAINT fk_ppb_detail_biaya;
