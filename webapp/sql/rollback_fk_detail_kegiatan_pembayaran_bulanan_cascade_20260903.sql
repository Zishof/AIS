-- Rollback migrasi_fk_detail_kegiatan_pembayaran_bulanan_cascade_20260903.sql.
-- Mengembalikan perilaku protektif NO ACTION. Data DetailKegiatan yang telah
-- terhapus oleh operasi DELETE selama CASCADE aktif tidak dapat dipulihkan oleh
-- rollback skema ini; gunakan backup/audit bila pemulihan data diperlukan.

BEGIN;

ALTER TABLE public.detail_kegiatan
    DROP CONSTRAINT IF EXISTS fk_detail_kegiatan_pengaturan_pembayaran_bulanan;

ALTER TABLE public.detail_kegiatan
    ADD CONSTRAINT fk_detail_kegiatan_pengaturan_pembayaran_bulanan
    FOREIGN KEY (pengaturan_pembayaran_bulanan)
    REFERENCES public.pengaturan_pembayaran_bulanan(id)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;

COMMIT;
