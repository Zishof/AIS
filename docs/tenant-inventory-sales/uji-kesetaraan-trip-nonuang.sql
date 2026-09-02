-- =====================================================================================
-- Uji kesetaraan kelompok trip non-uang: mulai trip dan hasil barang
-- =====================================================================================
--
-- Menjawab: untuk peristiwa yang SAMA, apakah kedua model menghasilkan barang yang dibawa
-- dan hasil barang yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55442" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump trp trp__audit > trp.sql
--   psql -h 127.0.0.1 -p 55442 -U uji -d postgres -v ON_ERROR_STOP=1 -f trp.sql
--   psql -h 127.0.0.1 -p 55442 -U uji -d postgres -f uji-kesetaraan-trip-nonuang.sql
--
-- PERBEDAAN RANCANGAN YANG DIUJI
--   Legacy menyimpan rencana DAN hasil pada satu baris spj_sales_barang.
--   Model tenant memisahkannya: sales_trip_barang (yang dibawa) dan
--   sales_trip_hasil (yang terjadi). Pemisahan itu membuat rencana tidak tertimpa hasil.
--   qty_hilang legacy dipetakan ke selisih: keduanya kuantitas yang tidak kembali dan
--   tidak terjual.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.toko (id bigserial PRIMARY KEY, nama varchar(255));
CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.produk (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.surat_perintah_sales_jalan (id bigserial PRIMARY KEY, nomor varchar(64),
  status varchar(32), tanggal_berangkat_rencana date, sales bigint, toko bigint,
  uang_muka_operasional numeric(18,2));
CREATE TABLE koperasi.spj_sales_barang (id bigserial PRIMARY KEY, spj bigint, produk bigint,
  qty_rencana numeric(18,4), qty_dimuat numeric(18,4), qty_terjual numeric(18,4),
  qty_kembali numeric(18,4), qty_rusak numeric(18,4), qty_hilang numeric(18,4),
  status varchar(32));
CREATE TABLE koperasi.nota_sales_session (id bigserial PRIMARY KEY, spj bigint, nomor varchar(64),
  status varchar(32), waktu_mulai timestamp, saldo_kas_awal numeric(18,2));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.toko (id, nama) VALUES (1, 'Toko A');
INSERT INTO koperasi.sales_inventory (id, kode, nama) VALUES (5, 'S-1', 'Budi');
INSERT INTO koperasi.produk (id, kode, nama) VALUES (11, 'P-1', 'Kopi'), (12, 'P-2', 'Gula');
INSERT INTO koperasi.surat_perintah_sales_jalan
    (id, nomor, status, tanggal_berangkat_rencana, sales, toko, uang_muka_operasional)
  VALUES (100, 'SPJ-1-100', 'ACTIVE', '2026-03-01', 5, 1, 500000);
-- Sesudah tripStart: qty_dimuat = qty_rencana, status LOADED.
-- Sesudah tripBarangUpdate: hasilnya terisi.
INSERT INTO koperasi.spj_sales_barang
    (spj, produk, qty_rencana, qty_dimuat, qty_terjual, qty_kembali, qty_rusak, qty_hilang, status)
  VALUES (100, 11, 20, 20, 12, 5, 2, 1, 'LOADED'),
         (100, 12, 15.5, 15.5, 10, 5.5, 0, 0, 'LOADED');
INSERT INTO koperasi.nota_sales_session (id, spj, nomor, status, waktu_mulai, saldo_kas_awal)
  VALUES (900, 100, 'NSS-1-900', 'ACTIVE', '2026-03-01 07:00', 500000);

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM trp.sales_trip_hasil;
DELETE FROM trp.sales_trip_barang;
DELETE FROM trp.sales_trip;
DELETE FROM trp.surat_perintah_sales_detail;
DELETE FROM trp.surat_perintah_sales;
DELETE FROM trp.produk;
DELETE FROM trp.satuan;
DELETE FROM trp.salesperson;
DELETE FROM trp.gudang;
DELETE FROM trp.toko;

INSERT INTO trp.toko (id, nama) VALUES (1, 'Toko A');
INSERT INTO trp.gudang (id, kode, nama, toko_id) VALUES (9, 'G-1', 'Gudang A', 1);
INSERT INTO trp.salesperson (id, kode, nama) VALUES (5, 'S-1', 'Budi');
INSERT INTO trp.satuan (id, kode, nama) VALUES (1, 'PCS', 'PCS');
INSERT INTO trp.produk (id, kode, nama, satuan_id) VALUES (11, 'P-1', 'Kopi', 1), (12, 'P-2', 'Gula', 1);
INSERT INTO trp.surat_perintah_sales
    (id, nomor_dokumen, tanggal, salesperson_id, gudang_id, status)
  VALUES (100, 'SPJ-1-100', '2026-03-01', 5, 9, 'ACTIVE');
INSERT INTO trp.surat_perintah_sales_detail (surat_perintah_sales_id, produk_id, kuantitas)
  VALUES (100, 11, 20), (100, 12, 15.5);
INSERT INTO trp.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
    gudang_id, tanggal_berangkat, status)
  VALUES (900, 'NSS-1-900', 100, 5, 9, '2026-03-01', 'ACTIVE');
-- Yang dibawa: hasil salinan dari rencana SPJ.
INSERT INTO trp.sales_trip_barang (sales_trip_id, produk_id, kuantitas_bawa)
  VALUES (900, 11, 20), (900, 12, 15.5);
-- Yang terjadi: baris terpisah, sehingga rencana tidak tertimpa.
INSERT INTO trp.sales_trip_hasil
    (sales_trip_id, produk_id, kuantitas_terjual, kuantitas_kembali, kuantitas_rusak, selisih)
  VALUES (900, 11, 12, 5, 2, 1),
         (900, 12, 10, 5.5, 0, 0);

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Barang yang DIBAWA (hasil tripStart menyalin rencana SPJ) ==='
WITH l AS (
  SELECT b.produk AS pid, b.qty_dimuat AS bawa
  FROM koperasi.spj_sales_barang b WHERE b.spj = 100
), t AS (
  SELECT b.produk_id AS pid, b.kuantitas_bawa AS bawa
  FROM trp.sales_trip_barang b WHERE b.sales_trip_id = 900
)
SELECT l.pid, l.bawa AS legacy_bawa, t.bawa AS tenant_bawa,
       CASE WHEN l.bawa = t.bawa THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

\echo ''
\echo '=== 2. HASIL barang (terjual / kembali / rusak / hilang-selisih) ==='
WITH l AS (
  SELECT b.produk AS pid, COALESCE(b.qty_terjual,0) AS terjual,
         COALESCE(b.qty_kembali,0) AS kembali, COALESCE(b.qty_rusak,0) AS rusak,
         COALESCE(b.qty_hilang,0) AS hilang
  FROM koperasi.spj_sales_barang b WHERE b.spj = 100
), t AS (
  SELECT h.produk_id AS pid, COALESCE(h.kuantitas_terjual,0) AS terjual,
         COALESCE(h.kuantitas_kembali,0) AS kembali, COALESCE(h.kuantitas_rusak,0) AS rusak,
         COALESCE(h.selisih,0) AS hilang
  FROM trp.sales_trip_hasil h WHERE h.sales_trip_id = 900
)
SELECT l.pid, l.terjual AS legacy_terjual, t.terjual AS tenant_terjual,
       l.kembali AS legacy_kembali, t.kembali AS tenant_kembali,
       l.rusak AS legacy_rusak, t.rusak AS tenant_rusak,
       l.hilang AS legacy_hilang, t.hilang AS tenant_selisih,
       CASE WHEN l.terjual = t.terjual AND l.kembali = t.kembali
                 AND l.rusak = t.rusak AND l.hilang = t.hilang
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

\echo ''
\echo '=== 3. Keseimbangan barang: bawa = terjual + kembali + rusak + hilang ==='
-- Yang dijaga: pemisahan bawa/hasil pada model tenant tidak boleh merusak keseimbangan.
WITH l AS (
  SELECT b.produk AS pid,
         b.qty_dimuat AS bawa,
         COALESCE(b.qty_terjual,0) + COALESCE(b.qty_kembali,0)
         + COALESCE(b.qty_rusak,0) + COALESCE(b.qty_hilang,0) AS jumlah_hasil
  FROM koperasi.spj_sales_barang b WHERE b.spj = 100
), t AS (
  SELECT b.produk_id AS pid, b.kuantitas_bawa AS bawa,
         COALESCE(h.kuantitas_terjual,0) + COALESCE(h.kuantitas_kembali,0)
         + COALESCE(h.kuantitas_rusak,0) + COALESCE(h.selisih,0) AS jumlah_hasil
  FROM trp.sales_trip_barang b
  LEFT JOIN trp.sales_trip_hasil h
    ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id
  WHERE b.sales_trip_id = 900
)
SELECT l.pid, l.bawa AS legacy_bawa, l.jumlah_hasil AS legacy_hasil,
       t.bawa AS tenant_bawa, t.jumlah_hasil AS tenant_hasil,
       CASE WHEN l.bawa = l.jumlah_hasil AND t.bawa = t.jumlah_hasil
                 AND l.bawa = t.bawa AND l.jumlah_hasil = t.jumlah_hasil
            THEN 'SETARA & SEIMBANG' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

-- =====================================================================================
-- Perbedaan yang DISENGAJA
-- =====================================================================================
\echo ''
\echo '=== Perbedaan disengaja: uang muka operasional ==='
-- Legacy memulai trip dengan saldo kas awal dari uang muka SPJ -- kas mengambang yang
-- diperhitungkan saat rekonsiliasi. Model tenant tidak punya keduanya, dan rekonsiliasinya
-- memang tidak memakai kas mengambang: sales_trip_rekonsiliasi menimbang nilai barang bawa,
-- barang kembali, penjualan, biaya, dan setoran.
--
-- Jadi ini bukan medan yang hilang, melainkan cara rekonsiliasi yang berbeda.
SELECT (SELECT uang_muka_operasional FROM koperasi.surat_perintah_sales_jalan WHERE id = 100)
         AS legacy_uang_muka,
       (SELECT saldo_kas_awal FROM koperasi.nota_sales_session WHERE id = 900)
         AS legacy_saldo_kas_awal,
       NULL AS tenant_saldo_kas_awal,
       'model tenant merekonsiliasi tanpa kas mengambang' AS catatan;

\echo ''
\echo 'Selesai. Blok 1-3 harus SETARA.'
