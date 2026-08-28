-- Jalankan sebelum deploy server yang memuat UOM kategori + Purchase UOM.
-- DDL idempoten; data lama dipetakan ke kategori UNIT dan Reference 1:1.

ALTER TABLE koperasi.satuan_produk
  ADD COLUMN IF NOT EXISTS kategori varchar(50),
  ADD COLUMN IF NOT EXISTS tipe_konversi varchar(20),
  ADD COLUMN IF NOT EXISTS rasio float8,
  ADD COLUMN IF NOT EXISTS presisi_pembulatan float8;

-- Data lama belum mempunyai informasi dimensi/rasio. Setiap UOM ditempatkan
-- pada kategori legacy tersendiri agar sistem tidak menebak bahwa Kg, Liter,
-- Pcs, dan Dus saling dapat dikonversi. Admin kemudian memindahkannya secara
-- eksplisit ke UNIT/BERAT/VOLUME dan menentukan satu Reference per kategori.
UPDATE koperasi.satuan_produk
SET kategori = COALESCE(NULLIF(TRIM(kategori), ''), 'LEGACY_' || id::text),
    tipe_konversi = COALESCE(NULLIF(TRIM(tipe_konversi), ''), 'REFERENCE'),
    rasio = CASE WHEN COALESCE(rasio, 0) <= 0 THEN 1 ELSE rasio END,
    presisi_pembulatan = CASE WHEN COALESCE(presisi_pembulatan, 0) <= 0 THEN 0.01 ELSE presisi_pembulatan END;

ALTER TABLE koperasi.produk
  ADD COLUMN IF NOT EXISTS satuan_pembelian int8;

UPDATE koperasi.produk
SET satuan_pembelian = satuan
WHERE satuan_pembelian IS NULL AND satuan IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_produk_satuan_pembelian
  ON koperasi.produk(satuan_pembelian);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_produk_satuan_pembelian'
  ) THEN
    ALTER TABLE koperasi.produk
      ADD CONSTRAINT fk_produk_satuan_pembelian
      FOREIGN KEY (satuan_pembelian) REFERENCES koperasi.satuan_produk(id);
  END IF;
END $$;

-- Envers tidak menambah kolom baru pada audit table existing.
ALTER TABLE new_audit.produk__audit
  ADD COLUMN IF NOT EXISTS satuan_pembelian int8;

ALTER TABLE new_audit.satuan_produk__audit
  ADD COLUMN IF NOT EXISTS kategori varchar(50),
  ADD COLUMN IF NOT EXISTS tipe_konversi varchar(20),
  ADD COLUMN IF NOT EXISTS rasio float8,
  ADD COLUMN IF NOT EXISTS presisi_pembulatan float8;
