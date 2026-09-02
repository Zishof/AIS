-- =====================================================================================
-- Uji kesetaraan Piutang: sisa, umur, lingkup toko, dan riwayat penagihan
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v10:
--   psql -h 127.0.0.1 -p 55445 -U uji -d postgres -f uji-kesetaraan-piutang.sql
--
-- TIGA PERBEDAAN BENTUK YANG DIUJI
--   1. Sisa piutang dihitung dari alokasi, bukan dibaca dari kolom d.sisa.
--   2. dibayar_awal legacy tidak punya kolom di tenant -- uang muka sudah berupa
--      alokasi penerimaan biasa, jadi tercakup penjumlahan alokasi.
--   3. Piutang tenant TIDAK menyimpan toko; lingkupnya ditegakkan lewat fakturnya.
--      Blok 3 menguji sisi negatifnya: toko lain harus melihat NOL.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.anggota_koperasi (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.piutang_customer_doc (id bigserial PRIMARY KEY, nomor varchar(64),
  customer bigint, sales bigint, toko bigint, sales_order bigint, tanggal date,
  jatuh_tempo date, total_faktur numeric(18,2), dibayar_awal numeric(18,2),
  status varchar(16), keterangan text);
CREATE TABLE koperasi.alokasi_penerimaan_piutang_customer (id bigserial PRIMARY KEY,
  piutang_doc bigint, penerimaan bigint, nominal numeric(18,2));
CREATE TABLE koperasi.penerimaan_piutang_customer (id bigserial PRIMARY KEY, nomor varchar(64),
  customer bigint, sales bigint, tanggal date, nominal numeric(18,2), metode varchar(32),
  no_bg varchar(64), nama_bank varchar(64), keterangan text, status_dok varchar(16),
  status_bg varchar(16));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.anggota_koperasi (id, kode, nama) VALUES (7, 'C-1', 'Warung Melati');
INSERT INTO koperasi.sales_inventory (id, kode, nama) VALUES (5, 'S-1', 'Budi');
-- F-1 toko 1: total 800.000, DP 100.000, dibayar 250.000 -> sisa 450.000, jatuh tempo lewat
INSERT INTO koperasi.piutang_customer_doc
    (id, nomor, customer, sales, toko, tanggal, jatuh_tempo, total_faktur, dibayar_awal, status)
  VALUES (50, 'INV-1', 7, 5, 1, '2026-01-05', '2026-02-04', 800000, 100000, 'AKTIF');
INSERT INTO koperasi.alokasi_penerimaan_piutang_customer (piutang_doc, penerimaan, nominal)
  VALUES (50, 900, 250000);
-- F-2 toko 2: milik toko LAIN. Harus tidak terlihat oleh aktor bertoko 1.
INSERT INTO koperasi.piutang_customer_doc
    (id, nomor, customer, sales, toko, tanggal, jatuh_tempo, total_faktur, dibayar_awal, status)
  VALUES (51, 'INV-2', 7, 5, 2, '2026-01-05', '2026-02-04', 500000, 0, 'AKTIF');
INSERT INTO koperasi.penerimaan_piutang_customer
    (id, nomor, customer, sales, tanggal, nominal, metode, no_bg, nama_bank, status_dok)
  VALUES (900, 'RCV-1', 7, 5, '2026-02-10', 250000, 'TRANSFER', '', '', 'AKTIF');

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM ar.alokasi_penerimaan_piutang;
DELETE FROM ar.penerimaan_piutang;
DELETE FROM ar.piutang_customer;
DELETE FROM ar.faktur_penjualan;
DELETE FROM ar.customer;
DELETE FROM ar.salesperson;
DELETE FROM ar.toko;

INSERT INTO ar.toko (id, nama) VALUES (1, 'Toko A'), (2, 'Toko B');
INSERT INTO ar.customer (id, kode, nama) VALUES (7, 'C-1', 'Warung Melati');
INSERT INTO ar.salesperson (id, kode, nama) VALUES (5, 'S-1', 'Budi');
-- Faktur menyimpan tokonya; piutang menautkan diri ke faktur.
INSERT INTO ar.faktur_penjualan (id, nomor_dokumen, nomor_faktur, tanggal, customer_id,
    salesperson_id, toko_id, total)
  VALUES (60, 'FJ-1', 'INV-1', '2026-01-05', 7, 5, 1, 800000),
         (61, 'FJ-2', 'INV-2', '2026-01-05', 7, 5, 2, 500000);
INSERT INTO ar.piutang_customer (id, customer_id, salesperson_id, faktur_penjualan_id,
    nomor_faktur, tanggal, jatuh_tempo, nilai, status)
  VALUES (50, 7, 5, 60, 'INV-1', '2026-01-05', '2026-02-04', 800000, 'AKTIF'),
         (51, 7, 5, 61, 'INV-2', '2026-01-05', '2026-02-04', 500000, 'AKTIF');
-- Uang muka legacy (100.000) menjadi alokasi penerimaan biasa, bersama pembayaran 250.000.
INSERT INTO ar.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
    nilai, cara_bayar, status)
  VALUES (899, 'DP-1', '2026-01-05', 7, 5, 100000, 'TUNAI', 'AKTIF'),
         (900, 'RCV-1', '2026-02-10', 7, 5, 250000, 'TRANSFER', 'AKTIF');
INSERT INTO ar.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai)
  VALUES (899, 50, 100000), (900, 50, 250000);

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Sisa piutang per dokumen ==='
WITH l AS (
  SELECT d.id, d.nomor, COALESCE(d.total_faktur,0) AS total,
         (COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0)
          - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                      WHERE a.piutang_doc = d.id),0)) AS sisa
  FROM koperasi.piutang_customer_doc d
), t AS (
  SELECT d.id, COALESCE(d.nomor_faktur,'') AS nomor, COALESCE(d.nilai,0) AS total,
         (COALESCE(d.nilai,0)
          - COALESCE((SELECT SUM(a.nilai) FROM ar.alokasi_penerimaan_piutang a
                      WHERE a.piutang_customer_id = d.id),0)) AS sisa
  FROM ar.piutang_customer d
)
SELECT l.nomor, l.total AS legacy_total, t.total AS tenant_total,
       l.sisa AS legacy_sisa, t.sisa AS tenant_sisa,
       CASE WHEN l.total = t.total AND l.sisa = t.sisa
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.nomor;

\echo ''
\echo '--- catatan: uang muka 100.000 legacy jadi alokasi penerimaan di tenant ---'
SELECT (SELECT dibayar_awal FROM koperasi.piutang_customer_doc WHERE id = 50) AS legacy_dp_kolom,
       (SELECT COUNT(*) FROM koperasi.alokasi_penerimaan_piutang_customer WHERE piutang_doc = 50)
         AS legacy_jumlah_alokasi,
       (SELECT COUNT(*) FROM ar.alokasi_penerimaan_piutang WHERE piutang_customer_id = 50)
         AS tenant_jumlah_alokasi,
       'DP tenant punya dokumen penerimaan sendiri -- jejaknya lebih lengkap' AS catatan;

\echo ''
\echo '=== 2. Umur piutang (acuan 2026-03-01) ==='
WITH l AS (
  SELECT d.nomor,
         CASE WHEN d.jatuh_tempo IS NULL OR d.jatuh_tempo >= DATE '2026-03-01' THEN 'BELUM'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 30 THEN 'B1_30'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 60 THEN 'B31_60'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 90 THEN 'B61_90'
              ELSE 'B90' END AS bucket
  FROM koperasi.piutang_customer_doc d
), t AS (
  SELECT COALESCE(d.nomor_faktur,'') AS nomor,
         CASE WHEN d.jatuh_tempo IS NULL OR d.jatuh_tempo >= DATE '2026-03-01' THEN 'BELUM'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 30 THEN 'B1_30'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 60 THEN 'B31_60'
              WHEN (DATE '2026-03-01' - d.jatuh_tempo) <= 90 THEN 'B61_90'
              ELSE 'B90' END AS bucket
  FROM ar.piutang_customer d
)
SELECT l.nomor, l.bucket AS legacy_bucket, t.bucket AS tenant_bucket,
       CASE WHEN l.bucket = t.bucket THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.nomor = l.nomor ORDER BY l.nomor;

\echo ''
\echo '=== 3. LINGKUP TOKO: legacy langsung vs tenant lewat faktur ==='
-- Piutang tenant tidak menyimpan toko. Yang diuji: aktor bertoko 1 melihat piutang yang
-- SAMA, dan aktor bertoko 2 melihat piutang toko 2 saja -- bukan keduanya.
SELECT
  (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.toko = 1) AS legacy_toko1,
  (SELECT COUNT(*) FROM ar.piutang_customer d
   LEFT JOIN ar.faktur_penjualan f ON d.faktur_penjualan_id = f.id
   WHERE f.toko_id = 1) AS tenant_toko1,
  (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.toko = 2) AS legacy_toko2,
  (SELECT COUNT(*) FROM ar.piutang_customer d
   LEFT JOIN ar.faktur_penjualan f ON d.faktur_penjualan_id = f.id
   WHERE f.toko_id = 2) AS tenant_toko2,
  CASE WHEN (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.toko = 1)
            = (SELECT COUNT(*) FROM ar.piutang_customer d
               LEFT JOIN ar.faktur_penjualan f ON d.faktur_penjualan_id = f.id WHERE f.toko_id = 1)
        AND (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.toko = 2)
            = (SELECT COUNT(*) FROM ar.piutang_customer d
               LEFT JOIN ar.faktur_penjualan f ON d.faktur_penjualan_id = f.id WHERE f.toko_id = 2)
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo '--- penjaga: kedua toko harus melihat jumlah BERBEDA, kalau tidak ujinya tumpul ---'
SELECT CASE WHEN (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.toko = 1)
                 <> (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d)
            THEN 'TAJAM -- saringan toko memang menyaring'
            ELSE '*** tumpul: seluruh piutang milik satu toko ***' END AS penjaga;

\echo ''
\echo '=== 4. Ringkasan per sales (jumlah dokumen, total, tertagih, sisa) ==='
SELECT
  (SELECT COUNT(*) FROM koperasi.piutang_customer_doc d WHERE d.status='AKTIF') AS legacy_dok,
  (SELECT COUNT(*) FROM ar.piutang_customer d WHERE d.status='AKTIF') AS tenant_dok,
  (SELECT COALESCE(SUM(COALESCE(d.dibayar_awal,0)
     + COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                 WHERE a.piutang_doc = d.id),0)),0)
   FROM koperasi.piutang_customer_doc d WHERE d.status='AKTIF') AS legacy_tertagih,
  (SELECT COALESCE(SUM(COALESCE((SELECT SUM(a.nilai) FROM ar.alokasi_penerimaan_piutang a
                 WHERE a.piutang_customer_id = d.id),0)),0)
   FROM ar.piutang_customer d WHERE d.status='AKTIF') AS tenant_tertagih,
  CASE WHEN (SELECT COALESCE(SUM(COALESCE(d.dibayar_awal,0)
       + COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                   WHERE a.piutang_doc = d.id),0)),0)
     FROM koperasi.piutang_customer_doc d WHERE d.status='AKTIF')
     = (SELECT COALESCE(SUM(COALESCE((SELECT SUM(a.nilai) FROM ar.alokasi_penerimaan_piutang a
                   WHERE a.piutang_customer_id = d.id),0)),0)
        FROM ar.piutang_customer d WHERE d.status='AKTIF')
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo 'Selesai. Blok 1-4 harus SETARA, dan penjaga blok 3 harus TAJAM.'
