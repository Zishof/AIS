-- Jalankan sebelum deploy server yang memuat UOM kategori + Purchase UOM.
-- DDL idempoten dan atomik; data lama dipetakan ke kategori legacy tersendiri.

BEGIN;
SET LOCAL lock_timeout = '15s';

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
  ADD COLUMN IF NOT EXISTS satuan_pembelian int8,
  ADD COLUMN IF NOT EXISTS kemasan text;

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
      AND conrelid = 'koperasi.produk'::regclass
  ) THEN
    ALTER TABLE koperasi.produk
      ADD CONSTRAINT fk_produk_satuan_pembelian
      FOREIGN KEY (satuan_pembelian) REFERENCES koperasi.satuan_produk(id);
  END IF;
END $$;

-- Envers tidak menambah kolom baru pada audit table existing.
ALTER TABLE IF EXISTS new_audit.produk__audit
  ADD COLUMN IF NOT EXISTS satuan_pembelian int8,
  ADD COLUMN IF NOT EXISTS kemasan text;

ALTER TABLE IF EXISTS new_audit.satuan_produk__audit
  ADD COLUMN IF NOT EXISTS kategori varchar(50),
  ADD COLUMN IF NOT EXISTS tipe_konversi varchar(20),
  ADD COLUMN IF NOT EXISTS rasio float8,
  ADD COLUMN IF NOT EXISTS presisi_pembulatan float8;

-- Snapshot baris penerimaan. Kolom qty dan hargabelisatuan existing tetap
-- menyimpan nilai dalam UOM dasar agar rumus stok/HPP historis tidak berubah.
ALTER TABLE koperasi.pengadaan_produk
  ADD COLUMN IF NOT EXISTS satuan_input int8,
  ADD COLUMN IF NOT EXISTS qty_input float8,
  ADD COLUMN IF NOT EXISTS faktor_konversi float8,
  ADD COLUMN IF NOT EXISTS harga_beli_satuan_input float8;

UPDATE koperasi.pengadaan_produk pg
SET satuan_input = COALESCE(pg.satuan_input, p.satuan),
    qty_input = COALESCE(pg.qty_input, pg.qty),
    faktor_konversi = CASE WHEN COALESCE(pg.faktor_konversi, 0) <= 0 THEN 1 ELSE pg.faktor_konversi END,
    harga_beli_satuan_input = COALESCE(pg.harga_beli_satuan_input, pg.hargabelisatuan)
FROM koperasi.produk p
WHERE p.id = pg.produk;

CREATE INDEX IF NOT EXISTS idx_pengadaan_produk_satuan_input
  ON koperasi.pengadaan_produk(satuan_input);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_pengadaan_produk_satuan_input'
      AND conrelid = 'koperasi.pengadaan_produk'::regclass
  ) THEN
    ALTER TABLE koperasi.pengadaan_produk
      ADD CONSTRAINT fk_pengadaan_produk_satuan_input
      FOREIGN KEY (satuan_input) REFERENCES koperasi.satuan_produk(id);
  END IF;
END $$;

ALTER TABLE IF EXISTS new_audit.pengadaan_produk__audit
  ADD COLUMN IF NOT EXISTS satuan_input int8,
  ADD COLUMN IF NOT EXISTS qty_input float8,
  ADD COLUMN IF NOT EXISTS faktor_konversi float8,
  ADD COLUMN IF NOT EXISTS harga_beli_satuan_input float8;

COMMIT;
