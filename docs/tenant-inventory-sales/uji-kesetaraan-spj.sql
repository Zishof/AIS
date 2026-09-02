-- =====================================================================================
-- Uji kesetaraan kelompok SPJ (Surat Perintah Sales Jalan)
-- =====================================================================================
--
-- Menjawab: untuk data yang SAMA, apakah kedua model menghasilkan daftar SPJ, rinci SPJ,
-- dan baris barangnya yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55441" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump spj spj__audit > spj.sql
--   psql -h 127.0.0.1 -p 55441 -U uji -d postgres -v ON_ERROR_STOP=1 -f spj.sql
--   psql -h 127.0.0.1 -p 55441 -U uji -d postgres -f uji-kesetaraan-spj.sql
--
-- TIGA CELAH MODEL YANG DIUJI DI SINI
--   1. Lingkup: legacy menyaring toko, tenant berlingkup gudang (gudang.toko_id menjembatani).
--   2. rute dan uang_muka_operasional tidak punya padanan -- dikosongkan, bukan ditebak.
--   3. Penugasan piutang ke SPJ tidak ada padanannya; daftar nota selalu kosong.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.toko (id bigserial PRIMARY KEY, nama varchar(255));
CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255), aktif boolean DEFAULT true);
CREATE TABLE koperasi.produk (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.surat_perintah_sales_jalan (id bigserial PRIMARY KEY, nomor varchar(64),
  status varchar(32), tanggal_berangkat_rencana date, rute varchar(255),
  kendaraan varchar(64), uang_muka_operasional numeric(18,2), catatan text,
  sales bigint, toko bigint, kode_unik varchar(128));
CREATE TABLE koperasi.spj_sales_barang (id bigserial PRIMARY KEY, spj bigint, produk bigint,
  nama_produk varchar(255), qty_rencana numeric(18,4));
CREATE TABLE koperasi.spj_sales_nota (id bigserial PRIMARY KEY, spj bigint, piutang_doc bigint);
CREATE TABLE koperasi.nota_sales_session (id bigserial PRIMARY KEY, spj bigint, nomor varchar(64));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.toko (id, nama) VALUES (1, 'Toko A');
INSERT INTO koperasi.sales_inventory (id, kode, nama) VALUES (5, 'S-1', 'Budi');
INSERT INTO koperasi.produk (id, kode, nama) VALUES (11, 'P-1', 'Kopi'), (12, 'P-2', 'Gula');
INSERT INTO koperasi.surat_perintah_sales_jalan
    (id, nomor, status, tanggal_berangkat_rencana, rute, kendaraan, uang_muka_operasional,
     catatan, sales, toko)
  VALUES (100, 'SPJ-1-100', 'APPROVED', '2026-03-01', 'Bandung-Cimahi', 'B 1234 XY',
          500000, 'bawa contoh produk baru', 5, 1);
INSERT INTO koperasi.spj_sales_barang (spj, produk, nama_produk, qty_rencana) VALUES
  (100, 11, 'Kopi', 20),
  (100, 12, 'Gula', 15.5);
INSERT INTO koperasi.nota_sales_session (id, spj, nomor) VALUES (900, 100, 'TRIP-1');

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM spj.surat_perintah_sales_detail;
DELETE FROM spj.sales_trip;
DELETE FROM spj.surat_perintah_sales;
DELETE FROM spj.produk;
DELETE FROM spj.satuan;
DELETE FROM spj.salesperson;
DELETE FROM spj.gudang;
DELETE FROM spj.toko;

INSERT INTO spj.toko (id, nama) VALUES (1, 'Toko A');
-- Lingkup: SPJ tenant menempel pada GUDANG, dan gudang inilah yang menautkannya ke toko.
INSERT INTO spj.gudang (id, kode, nama, toko_id) VALUES (9, 'G-1', 'Gudang A', 1);
INSERT INTO spj.salesperson (id, kode, nama) VALUES (5, 'S-1', 'Budi');
INSERT INTO spj.satuan (id, kode, nama) VALUES (1, 'PCS', 'PCS');
INSERT INTO spj.produk (id, kode, nama, satuan_id) VALUES (11, 'P-1', 'Kopi', 1), (12, 'P-2', 'Gula', 1);
INSERT INTO spj.surat_perintah_sales
    (id, nomor_dokumen, tanggal, salesperson_id, gudang_id, keterangan, status)
  VALUES (100, 'SPJ-1-100', '2026-03-01', 5, 9, 'bawa contoh produk baru', 'APPROVED');
INSERT INTO spj.surat_perintah_sales_detail (surat_perintah_sales_id, produk_id, kuantitas) VALUES
  (100, 11, 20),
  (100, 12, 15.5);
-- Kendaraan melekat pada trip di model tenant, bukan pada SPJ.
INSERT INTO spj.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
    gudang_id, tanggal_berangkat, kendaraan)
  VALUES (900, 'TRIP-1', 100, 5, 9, '2026-03-01', 'B 1234 XY');

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Rinci SPJ: medan yang punya padanan ==='
WITH l AS (
  SELECT j.id, j.nomor, j.status, j.tanggal_berangkat_rencana AS tanggal,
         COALESCE(j.kendaraan,'') AS kendaraan, COALESCE(j.catatan,'') AS catatan,
         j.sales AS sales_id, COALESCE(s.nama,'') AS sales_nama, j.toko AS toko_id,
         (SELECT ns.id FROM koperasi.nota_sales_session ns WHERE ns.spj = j.id LIMIT 1) AS sesi
  FROM koperasi.surat_perintah_sales_jalan j
  LEFT JOIN koperasi.sales_inventory s ON j.sales = s.id
), t AS (
  SELECT j.id, COALESCE(j.nomor_dokumen,'') AS nomor, COALESCE(j.status,'') AS status,
         j.tanggal,
         COALESCE((SELECT tr.kendaraan FROM spj.sales_trip tr
                   WHERE tr.surat_perintah_sales_id = j.id ORDER BY tr.id DESC LIMIT 1),'') AS kendaraan,
         COALESCE(j.keterangan,'') AS catatan,
         j.salesperson_id AS sales_id, COALESCE(s.nama,'') AS sales_nama,
         COALESCE((SELECT g.toko_id FROM spj.gudang g WHERE g.id = j.gudang_id),0) AS toko_id,
         (SELECT tr2.id FROM spj.sales_trip tr2
          WHERE tr2.surat_perintah_sales_id = j.id ORDER BY tr2.id DESC LIMIT 1) AS sesi
  FROM spj.surat_perintah_sales j
  LEFT JOIN spj.salesperson s ON j.salesperson_id = s.id
)
SELECT l.nomor, l.status AS legacy_status, t.status AS tenant_status,
       l.kendaraan AS legacy_kendaraan, t.kendaraan AS tenant_kendaraan,
       l.toko_id AS legacy_toko, t.toko_id AS tenant_toko,
       l.sesi AS legacy_sesi, t.sesi AS tenant_sesi,
       CASE WHEN l.nomor = t.nomor AND l.status = t.status AND l.tanggal = t.tanggal
                 AND l.kendaraan = t.kendaraan AND l.catatan = t.catatan
                 AND l.sales_id = t.sales_id AND l.sales_nama = t.sales_nama
                 AND l.toko_id = t.toko_id AND l.sesi = t.sesi
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.nomor;

\echo ''
\echo '=== 2. Baris barang SPJ ==='
WITH l AS (
  SELECT b.produk AS pid, COALESCE(p.kode,'') AS kode, COALESCE(p.nama,'') AS nama,
         b.qty_rencana AS qty
  FROM koperasi.spj_sales_barang b JOIN koperasi.produk p ON b.produk = p.id
  WHERE b.spj = 100
), t AS (
  SELECT d.produk_id AS pid, COALESCE(p.kode,'') AS kode, COALESCE(p.nama,'') AS nama,
         d.kuantitas AS qty
  FROM spj.surat_perintah_sales_detail d JOIN spj.produk p ON d.produk_id = p.id
  WHERE d.surat_perintah_sales_id = 100
)
SELECT l.pid, l.kode AS legacy_kode, t.kode AS tenant_kode,
       l.nama AS legacy_nama, t.nama AS tenant_nama,
       l.qty AS legacy_qty, t.qty AS tenant_qty,
       CASE WHEN l.kode = t.kode AND l.nama = t.nama AND l.qty = t.qty
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

\echo ''
\echo '=== 3. Saringan lingkup toko: legacy langsung vs tenant lewat gudang ==='
-- Yang diuji: SPJ yang sama harus terlihat oleh aktor bertoko 1, dan TIDAK terlihat
-- oleh aktor bertoko 2. Saringan lingkup yang hilang berarti sales satu toko melihat
-- perjalanan toko lain.
SELECT
  (SELECT COUNT(*) FROM koperasi.surat_perintah_sales_jalan j WHERE j.toko = 1) AS legacy_toko1,
  (SELECT COUNT(*) FROM spj.surat_perintah_sales j
   WHERE EXISTS (SELECT 1 FROM spj.gudang g WHERE g.id = j.gudang_id AND g.toko_id = 1)) AS tenant_toko1,
  (SELECT COUNT(*) FROM koperasi.surat_perintah_sales_jalan j WHERE j.toko = 2) AS legacy_toko2,
  (SELECT COUNT(*) FROM spj.surat_perintah_sales j
   WHERE EXISTS (SELECT 1 FROM spj.gudang g WHERE g.id = j.gudang_id AND g.toko_id = 2)) AS tenant_toko2,
  CASE WHEN (SELECT COUNT(*) FROM koperasi.surat_perintah_sales_jalan j WHERE j.toko = 1)
            = (SELECT COUNT(*) FROM spj.surat_perintah_sales j
               WHERE EXISTS (SELECT 1 FROM spj.gudang g WHERE g.id = j.gudang_id AND g.toko_id = 1))
        AND (SELECT COUNT(*) FROM koperasi.surat_perintah_sales_jalan j WHERE j.toko = 2)
            = (SELECT COUNT(*) FROM spj.surat_perintah_sales j
               WHERE EXISTS (SELECT 1 FROM spj.gudang g WHERE g.id = j.gudang_id AND g.toko_id = 2))
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

-- =====================================================================================
-- Perbedaan yang DISENGAJA
-- =====================================================================================
\echo ''
\echo '=== Perbedaan disengaja: tiga medan tanpa padanan ==='
SELECT (SELECT rute FROM koperasi.surat_perintah_sales_jalan WHERE id = 100) AS legacy_rute,
       '' AS tenant_rute,
       (SELECT uang_muka_operasional FROM koperasi.surat_perintah_sales_jalan WHERE id = 100)
         AS legacy_uang_muka,
       NULL AS tenant_uang_muka,
       (SELECT COUNT(*) FROM koperasi.spj_sales_nota WHERE spj = 100) AS legacy_jumlah_nota,
       0 AS tenant_jumlah_nota,
       'rute, uang muka, dan penugasan piutang ke SPJ tidak ada di model tenant' AS catatan;

\echo ''
\echo 'Selesai. Blok 1-3 harus SETARA.'
