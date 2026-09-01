-- =====================================================================================
-- Uji kesetaraan Persediaan: model legacy (koperasi, 8 suku) vs model tenant (mutasi_stok)
-- =====================================================================================
--
-- Menjawab satu pertanyaan: untuk peristiwa bisnis yang SAMA, apakah kedua model
-- menghasilkan Awal / Masuk / Keluar / Opname yang sama?
--
-- Tanpa uji ini, perbedaan tanda pada `arah` atau salah-petakan satu tabel baru ketahuan
-- sebagai selisih stok di toko sungguhan -- tempat termahal untuk menemukannya.
--
-- CARA PAKAI (klaster sekali-pakai, JANGAN pada basis data sungguhan):
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55433" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump kes kes__audit > kes.sql
--   psql -h 127.0.0.1 -p 55433 -U uji -d postgres -v ON_ERROR_STOP=1 -f kes.sql
--   psql -h 127.0.0.1 -p 55433 -U uji -d postgres -f uji-kesetaraan-stok.sql
--
-- Skrip ini membuat schema `koperasi` tiruan berisi HANYA kolom yang dipakai
-- SalesInventoryStokHelper. Ia bukan replika lengkap AIS dan tidak dimaksudkan begitu.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.satuan_produk (id bigserial PRIMARY KEY, nama varchar(64));
CREATE TABLE koperasi.produk (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255),
  barcode varchar(64), satuan bigint, hargabeli numeric(18,2), hargajual numeric(18,2),
  stok_minimum numeric(18,4), toko bigint, aktif boolean DEFAULT true);
CREATE TABLE koperasi.pengadaan_produk (id bigserial PRIMARY KEY, produk bigint,
  qty numeric(18,4), waktupengadaan timestamp, nomorfaktur varchar(64));
-- CATATAN: tabel legacy bernama `pembelian` menyimpan PENJUALAN, bukan pembelian.
CREATE TABLE koperasi.pembelian (id bigserial PRIMARY KEY, produk bigint,
  qty numeric(18,4), waktu timestamp);
CREATE TABLE koperasi.pemakaian_bahan_baku (id bigserial PRIMARY KEY, produk bigint,
  qty numeric(18,4), waktu timestamp);
CREATE TABLE koperasi.retur_penjualan (id bigserial PRIMARY KEY, produk bigint,
  qty numeric(18,4), waktu timestamp, kembalikan_ke_stok boolean);
CREATE TABLE koperasi.retur_pembelian (id bigserial PRIMARY KEY, produk bigint,
  qty numeric(18,4), waktu timestamp);
CREATE TABLE koperasi.mutasi_stok_toko (id bigserial PRIMARY KEY, produk_asal bigint,
  produk_tujuan bigint, qty numeric(18,4), waktu timestamp);
CREATE TABLE koperasi.stok_opname (id bigserial PRIMARY KEY, produk bigint,
  selisih numeric(18,4), waktuopname timestamp);

-- ------------------------------------------------------------------ data uji: LEGACY
INSERT INTO koperasi.satuan_produk (id, nama) VALUES (1, 'PCS');
INSERT INTO koperasi.produk (id, kode, nama, barcode, satuan, hargabeli, hargajual, stok_minimum, toko)
  VALUES (1, 'P-001', 'Kopi Bubuk', '8991', 1, 2500, 4000, 10, 1);
-- Produk kedua TANPA pergerakan sama sekali: kedua model harus sama-sama nol.
INSERT INTO koperasi.produk (id, kode, nama, barcode, satuan, hargabeli, hargajual, stok_minimum, toko)
  VALUES (2, 'P-002', 'Gula Pasir', '8992', 1, 1200, 1800, 5, 1);

-- SEBELUM rentang -> membentuk saldo awal
INSERT INTO koperasi.pengadaan_produk (produk, qty, waktupengadaan, nomorfaktur)
  VALUES (1, 50, '2026-01-05 09:00', 'F-1');
INSERT INTO koperasi.pembelian (produk, qty, waktu) VALUES (1, 8, '2026-01-06 10:00');

-- DI DALAM rentang 2026-02-01 .. 2026-02-28
INSERT INTO koperasi.pengadaan_produk (produk, qty, waktupengadaan, nomorfaktur)
  VALUES (1, 100.5, '2026-02-03 08:00', 'F-2');
INSERT INTO koperasi.pembelian (produk, qty, waktu) VALUES (1, 30, '2026-02-05 11:00');
INSERT INTO koperasi.pemakaian_bahan_baku (produk, qty, waktu) VALUES (1, 5, '2026-02-06 12:00');
INSERT INTO koperasi.retur_penjualan (produk, qty, waktu, kembalikan_ke_stok)
  VALUES (1, 7, '2026-02-07 13:00', true);
-- Retur yang TIDAK kembali ke stok: peristiwa uang, bukan stok. Tidak boleh terhitung.
INSERT INTO koperasi.retur_penjualan (produk, qty, waktu, kembalikan_ke_stok)
  VALUES (1, 3, '2026-02-08 13:00', false);
INSERT INTO koperasi.retur_pembelian (produk, qty, waktu) VALUES (1, 4, '2026-02-09 14:00');
INSERT INTO koperasi.mutasi_stok_toko (produk_tujuan, qty, waktu) VALUES (1, 12, '2026-02-10 15:00');
INSERT INTO koperasi.mutasi_stok_toko (produk_asal, qty, waktu) VALUES (1, 6, '2026-02-11 16:00');
INSERT INTO koperasi.stok_opname (produk, selisih, waktuopname) VALUES (1, 9, '2026-02-12 17:00');
INSERT INTO koperasi.stok_opname (produk, selisih, waktuopname) VALUES (1, -2, '2026-02-13 17:00');

-- ------------------------------------------------------------------ data uji: TENANT
DELETE FROM kes.mutasi_stok;
DELETE FROM kes.produk;
DELETE FROM kes.satuan;
INSERT INTO kes.satuan (id, kode, nama) VALUES (1, 'PCS', 'PCS');
INSERT INTO kes.produk (id, kode, nama, barcode, satuan_id, harga_beli_terakhir,
                        harga_jual_standar, stok_minimum)
  VALUES (1, 'P-001', 'Kopi Bubuk', '8991', 1, 2500, 4000, 10),
         (2, 'P-002', 'Gula Pasir', '8992', 1, 1200, 1800, 5);
INSERT INTO kes.mutasi_stok (produk_id, tanggal, jenis, arah, kuantitas) VALUES
  (1, '2026-01-05', 'PENGADAAN',        1, 50),
  (1, '2026-01-06', 'PENJUALAN',       -1, 8),
  (1, '2026-02-03', 'PENGADAAN',        1, 100.5),
  (1, '2026-02-05', 'PENJUALAN',       -1, 30),
  (1, '2026-02-06', 'PEMAKAIAN_BAHAN', -1, 5),
  (1, '2026-02-07', 'RETUR_PENJUALAN',  1, 7),
  (1, '2026-02-09', 'RETUR_PEMBELIAN', -1, 4),
  (1, '2026-02-10', 'MUTASI_MASUK',     1, 12),
  (1, '2026-02-11', 'MUTASI_KELUAR',   -1, 6),
  (1, '2026-02-12', 'OPNAME',           1, 9),
  (1, '2026-02-13', 'OPNAME',          -1, 2);

\set QUIET off

-- =====================================================================================
-- Perbandingan
-- =====================================================================================
\echo ''
\echo '=== Kesetaraan Awal / Masuk / Keluar / Opname per produk ==='

WITH legacy AS (
  SELECT p.id AS produk_id,
    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk x
              WHERE x.produk = p.id AND x.waktupengadaan < DATE '2026-02-01'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.retur_penjualan x
              WHERE x.produk = p.id AND x.kembalikan_ke_stok = true
                AND x.waktu < DATE '2026-02-01'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko x
              WHERE x.produk_tujuan = p.id AND x.waktu < DATE '2026-02-01'), 0)
    + COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname x
              WHERE x.produk = p.id AND x.waktuopname < DATE '2026-02-01'), 0)
    - (COALESCE((SELECT SUM(qty) FROM koperasi.pembelian x
              WHERE x.produk = p.id AND x.waktu < DATE '2026-02-01'), 0)
     + COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku x
              WHERE x.produk = p.id AND x.waktu < DATE '2026-02-01'), 0)
     + COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko x
              WHERE x.produk_asal = p.id AND x.waktu < DATE '2026-02-01'), 0)
     + COALESCE((SELECT SUM(qty) FROM koperasi.retur_pembelian x
              WHERE x.produk = p.id AND x.waktu < DATE '2026-02-01'), 0)) AS awal,
    COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk x
              WHERE x.produk = p.id AND x.waktupengadaan
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.retur_penjualan x
              WHERE x.produk = p.id AND x.kembalikan_ke_stok = true
                AND x.waktu BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko x
              WHERE x.produk_tujuan = p.id AND x.waktu
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS masuk,
    COALESCE((SELECT SUM(qty) FROM koperasi.pembelian x
              WHERE x.produk = p.id AND x.waktu
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku x
              WHERE x.produk = p.id AND x.waktu
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.mutasi_stok_toko x
              WHERE x.produk_asal = p.id AND x.waktu
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0)
    + COALESCE((SELECT SUM(qty) FROM koperasi.retur_pembelian x
              WHERE x.produk = p.id AND x.waktu
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS keluar,
    COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname x
              WHERE x.produk = p.id AND x.waktuopname
              BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS opname
  FROM koperasi.produk p
), tenant AS (
  SELECT p.id AS produk_id,
    COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM kes.mutasi_stok m
              WHERE m.produk_id = p.id AND m.tanggal < DATE '2026-02-01'), 0) AS awal,
    COALESCE((SELECT SUM(m.kuantitas) FROM kes.mutasi_stok m
              WHERE m.produk_id = p.id AND m.arah = 1 AND m.jenis <> 'OPNAME'
                AND m.tanggal BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS masuk,
    COALESCE((SELECT SUM(m.kuantitas) FROM kes.mutasi_stok m
              WHERE m.produk_id = p.id AND m.arah = -1 AND m.jenis <> 'OPNAME'
                AND m.tanggal BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS keluar,
    COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM kes.mutasi_stok m
              WHERE m.produk_id = p.id AND m.jenis = 'OPNAME'
                AND m.tanggal BETWEEN DATE '2026-02-01' AND DATE '2026-02-28'), 0) AS opname
  FROM kes.produk p
)
SELECT l.produk_id,
       l.awal   AS legacy_awal,   t.awal   AS tenant_awal,
       l.masuk  AS legacy_masuk,  t.masuk  AS tenant_masuk,
       l.keluar AS legacy_keluar, t.keluar AS tenant_keluar,
       l.opname AS legacy_opname, t.opname AS tenant_opname,
       CASE WHEN l.awal = t.awal AND l.masuk = t.masuk
                 AND l.keluar = t.keluar AND l.opname = t.opname
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM legacy l JOIN tenant t ON t.produk_id = l.produk_id
ORDER BY l.produk_id;

-- =====================================================================================
-- Perbedaan yang DISENGAJA: batas rentang
-- =====================================================================================
-- Kondisi legacy yang sesungguhnya adalah BETWEEN dari AND (sampai + INTERVAL '1 day')
-- atas kolom timestamp, sehingga peristiwa tepat pukul 00:00:00 pada H+1 IKUT terhitung
-- ke rentang sebelumnya. mutasi_stok.tanggal bertipe date dan tidak dapat meniru itu.
--
-- Bagian ini MEMPERAGAKAN selisihnya, bukan menggagalkannya: jalur tenant sengaja tidak
-- mewarisi salah-hitung batas tersebut.
\echo ''
\echo '=== Perbedaan disengaja: peristiwa tepat 00:00:00 pada H+1 ==='

INSERT INTO koperasi.pengadaan_produk (produk, qty, waktupengadaan, nomorfaktur)
  VALUES (1, 1000, '2026-03-01 00:00:00', 'F-BATAS');
INSERT INTO kes.mutasi_stok (produk_id, tanggal, jenis, arah, kuantitas)
  VALUES (1, '2026-03-01', 'PENGADAAN', 1, 1000);

SELECT
  (SELECT COALESCE(SUM(qty), 0) FROM koperasi.pengadaan_produk x
   WHERE x.produk = 1 AND x.waktupengadaan
   BETWEEN DATE '2026-02-01' AND (DATE '2026-02-28' + INTERVAL '1 day')) AS legacy_menyerap_h1,
  (SELECT COALESCE(SUM(m.kuantitas), 0) FROM kes.mutasi_stok m
   WHERE m.produk_id = 1 AND m.jenis = 'PENGADAAN'
     AND m.tanggal BETWEEN DATE '2026-02-01' AND DATE '2026-02-28') AS tenant_tidak,
  'selisih ini DISENGAJA -- legacy salah hitung batas' AS catatan;

\echo ''
\echo 'Selesai. Baris hasil harus SETARA untuk kedua produk.'
