-- =====================================================================================
-- Uji kesetaraan Finance: model legacy (koperasi + akunting) vs model tenant
-- =====================================================================================
--
-- Menjawab: untuk peristiwa yang SAMA, apakah kedua model menghasilkan bagan akun,
-- kas/jurnal, laba kotor, laba-rugi, dan rinciannya yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55437" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump fin fin__audit > fin.sql
--   psql -h 127.0.0.1 -p 55437 -U uji -d postgres -v ON_ERROR_STOP=1 -f fin.sql
--   psql -h 127.0.0.1 -p 55437 -U uji -d postgres -f uji-kesetaraan-finance.sql
--
-- TIGA PEMETAAN YANG PALING MENENTUKAN ANGKA
--   1. HPP ada di FAKTUR (faktur_penjualan_detail.harga_beli), bukan di order.
--      sales_order_detail tenant tidak punya kolom harga pokok sama sekali.
--   2. Penjualan tunai = faktur TANPA baris piutang; tenant tidak punya penanda CASH_SALE.
--   3. Kategori biaya adalah kolom langsung pada sales_trip_biaya, bukan tabel tertaut.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
DROP SCHEMA IF EXISTS akunting CASCADE;
CREATE SCHEMA koperasi;
CREATE SCHEMA akunting;

CREATE TABLE akunting.akun (id bigserial PRIMARY KEY, kode varchar(32), nama varchar(255),
  keterangan text, debit_credit int, parent bigint);
CREATE TABLE akunting.transaksi (id bigserial PRIMARY KEY, kode varchar(64),
  jenis_jurnal varchar(32), tanggal_transaksi date, keterangan text, akun bigint,
  debet numeric(18,2), kredit numeric(18,2), tanggal_posting timestamp);

CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255), aktif boolean DEFAULT true);
CREATE TABLE koperasi.anggota_koperasi (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255));
CREATE TABLE koperasi.sales_order_lapangan (id bigserial PRIMARY KEY, nomor varchar(64),
  tanggal date, customer bigint, sales bigint, toko bigint, status varchar(32));
CREATE TABLE koperasi.sales_order_lapangan_item (id bigserial PRIMARY KEY, sales_order bigint,
  produk bigint, nama_produk varchar(255), jumlah numeric(18,4), harga_satuan numeric(18,2),
  subtotal numeric(18,2), hpp_snapshot numeric(18,2));
CREATE TABLE koperasi.piutang_customer_doc (id bigserial PRIMARY KEY, nomor varchar(64),
  sales_order bigint, tanggal date, sales bigint, toko bigint, total_faktur numeric(18,2),
  dibayar_awal numeric(18,2), status varchar(16));
CREATE TABLE koperasi.alokasi_penerimaan_piutang_customer (id bigserial PRIMARY KEY,
  piutang_doc bigint, nominal numeric(18,2));
CREATE TABLE koperasi.nota_sales_kas (id bigserial PRIMARY KEY, jenis varchar(32),
  nominal numeric(18,2), waktu date);
CREATE TABLE koperasi.kategori_biaya_sales (id bigserial PRIMARY KEY, nama varchar(64));
CREATE TABLE koperasi.nota_sales_biaya (id bigserial PRIMARY KEY, kategori bigint,
  nilai numeric(18,2), tanggal date);

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO akunting.akun (id, kode, nama, keterangan, debit_credit, parent) VALUES
  (1, '1000', 'AKTIVA', 'induk', 1, NULL),
  (2, '1100', 'Kas',    'anak',  1, 1);
INSERT INTO akunting.transaksi (id, kode, jenis_jurnal, tanggal_transaksi, keterangan, akun,
    debet, kredit, tanggal_posting) VALUES
  (1, 'JV-001', 'UMUM', '2026-02-10', 'setoran kas', 2, 500000, 0, '2026-02-10 09:00'),
  (2, 'JV-001', 'UMUM', '2026-02-10', 'lawan',       1, 0, 500000, '2026-02-10 09:00');

INSERT INTO koperasi.sales_inventory (id, kode, nama) VALUES (5, 'SLS-1', 'Budi');
INSERT INTO koperasi.anggota_koperasi (id, kode, nama) VALUES (7, 'CUS-1', 'Warung Melati');

-- Order 1: KREDIT -> melahirkan piutang. 2 baris.
INSERT INTO koperasi.sales_order_lapangan (id, nomor, tanggal, customer, sales, toko, status)
  VALUES (1, 'SO-1', '2026-02-05', 7, 5, 3, 'LUNAS');
INSERT INTO koperasi.sales_order_lapangan_item
    (sales_order, produk, nama_produk, jumlah, harga_satuan, subtotal, hpp_snapshot) VALUES
  (1, 11, 'Kopi Bubuk', 10, 4000, 40000, 2500),
  (1, 12, 'Gula Pasir',  5, 1000,  5000, 1200);   -- baris RUGI disengaja (1000 < 1200)
INSERT INTO koperasi.piutang_customer_doc
    (id, nomor, sales_order, tanggal, sales, toko, total_faktur, dibayar_awal, status)
  VALUES (100, 'INV-1', 1, '2026-02-05', 5, 3, 45000, 0, 'AKTIF');
INSERT INTO koperasi.alokasi_penerimaan_piutang_customer (piutang_doc, nominal)
  VALUES (100, 20000);

-- Order 2: TUNAI -> tidak melahirkan piutang; omzetnya lewat ledger kas.
INSERT INTO koperasi.sales_order_lapangan (id, nomor, tanggal, customer, sales, toko, status)
  VALUES (2, 'SO-2', '2026-02-08', 7, 5, 3, 'LUNAS');
INSERT INTO koperasi.sales_order_lapangan_item
    (sales_order, produk, nama_produk, jumlah, harga_satuan, subtotal, hpp_snapshot) VALUES
  (2, 11, 'Kopi Bubuk', 3, 4000, 12000, 2500);
INSERT INTO koperasi.nota_sales_kas (jenis, nominal, waktu)
  VALUES ('CASH_SALE', 12000, '2026-02-08');

INSERT INTO koperasi.kategori_biaya_sales (id, nama) VALUES (1, 'BBM'), (2, 'Makan');
INSERT INTO koperasi.nota_sales_biaya (kategori, nilai, tanggal) VALUES
  (1, 150000, '2026-02-06'),
  (2,  75000, '2026-02-07');

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM fin.alokasi_penerimaan_piutang;
DELETE FROM fin.penerimaan_piutang;
DELETE FROM fin.piutang_customer;
DELETE FROM fin.faktur_penjualan_detail;
DELETE FROM fin.faktur_penjualan;
DELETE FROM fin.sales_trip_biaya;
DELETE FROM fin.sales_trip;
DELETE FROM fin.jurnal_detail;
DELETE FROM fin.jurnal;
DELETE FROM fin.akun;
DELETE FROM fin.produk;
DELETE FROM fin.satuan;
DELETE FROM fin.customer;
DELETE FROM fin.salesperson;
DELETE FROM fin.toko;

INSERT INTO fin.akun (id, kode, nama, tipe, saldo_normal, induk_id) VALUES
  (1, '1000', 'AKTIVA', 'ASET', 'DEBIT', NULL),
  (2, '1100', 'Kas',    'ASET', 'DEBIT', 1);
INSERT INTO fin.jurnal (id, nomor_dokumen, tanggal, sumber_tipe, keterangan, diposting_pada)
  VALUES (1, 'JV-001', '2026-02-10', 'UMUM', 'setoran kas', '2026-02-10 09:00');
INSERT INTO fin.jurnal_detail (jurnal_id, baris_ke, akun_id, debit, kredit, keterangan) VALUES
  (1, 1, 2, 500000, 0, 'setoran kas'),
  (1, 2, 1, 0, 500000, 'lawan');

INSERT INTO fin.toko (id, nama) VALUES (3, 'Toko Pusat');
INSERT INTO fin.salesperson (id, kode, nama) VALUES (5, 'SLS-1', 'Budi');
INSERT INTO fin.customer (id, kode, nama) VALUES (7, 'CUS-1', 'Warung Melati');
INSERT INTO fin.satuan (id, kode, nama) VALUES (1, 'PCS', 'PCS');
INSERT INTO fin.produk (id, kode, nama, satuan_id) VALUES
  (11, 'P-001', 'Kopi Bubuk', 1),
  (12, 'P-002', 'Gula Pasir', 1);

-- Faktur 1: KREDIT -> melahirkan piutang.
INSERT INTO fin.faktur_penjualan (id, nomor_dokumen, nomor_faktur, tanggal, customer_id,
    salesperson_id, toko_id, total, hpp) VALUES
  (1, 'SO-1', 'INV-1', '2026-02-05', 7, 5, 3, 45000, 31000);
INSERT INTO fin.faktur_penjualan_detail
    (faktur_penjualan_id, baris_ke, produk_id, kuantitas, harga_satuan, harga_beli, total) VALUES
  (1, 1, 11, 10, 4000, 2500, 40000),
  (1, 2, 12,  5, 1000, 1200,  5000);
INSERT INTO fin.piutang_customer (id, customer_id, salesperson_id, faktur_penjualan_id,
    nomor_faktur, tanggal, nilai, status)
  VALUES (100, 7, 5, 1, 'INV-1', '2026-02-05', 45000, 'AKTIF');
INSERT INTO fin.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, nilai)
  VALUES (900, 'RCV-1', '2026-02-09', 7, 20000);
INSERT INTO fin.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai)
  VALUES (900, 100, 20000);

-- Faktur 2: TUNAI -> TIDAK melahirkan piutang. Itulah penanda tunai pada model tenant.
INSERT INTO fin.faktur_penjualan (id, nomor_dokumen, nomor_faktur, tanggal, customer_id,
    salesperson_id, toko_id, total, hpp) VALUES
  (2, 'SO-2', 'INV-2', '2026-02-08', 7, 5, 3, 12000, 7500);
INSERT INTO fin.faktur_penjualan_detail
    (faktur_penjualan_id, baris_ke, produk_id, kuantitas, harga_satuan, harga_beli, total) VALUES
  (2, 1, 11, 3, 4000, 2500, 12000);

INSERT INTO fin.sales_trip (id, nomor_dokumen, salesperson_id, tanggal_berangkat)
  VALUES (1, 'TRIP-1', 5, '2026-02-06');
INSERT INTO fin.sales_trip_biaya (sales_trip_id, kategori, nilai, tanggal) VALUES
  (1, 'BBM',   150000, '2026-02-06'),
  (1, 'Makan',  75000, '2026-02-07');

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Bagan akun ==='
WITH l AS (
  SELECT a.id, a.kode, a.nama, COALESCE(p.kode,'') AS induk_kode
  FROM akunting.akun a LEFT JOIN akunting.akun p ON a.parent = p.id
), t AS (
  SELECT a.id, a.kode, a.nama, COALESCE(p.kode,'') AS induk_kode
  FROM fin.akun a LEFT JOIN fin.akun p ON a.induk_id = p.id
)
SELECT l.kode, l.nama AS legacy_nama, t.nama AS tenant_nama,
       l.induk_kode AS legacy_induk, t.induk_kode AS tenant_induk,
       CASE WHEN l.nama = t.nama AND l.induk_kode = t.induk_kode
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.kode;

\echo ''
\echo '=== 2. Kas / Jurnal (jumlah baris dan totalnya) ==='
SELECT (SELECT COUNT(*) FROM akunting.transaksi) AS legacy_baris,
       (SELECT COUNT(*) FROM fin.jurnal_detail)  AS tenant_baris,
       (SELECT COALESCE(SUM(debet),0) FROM akunting.transaksi) AS legacy_debet,
       (SELECT COALESCE(SUM(debit),0) FROM fin.jurnal_detail)  AS tenant_debet,
       (SELECT COALESCE(SUM(kredit),0) FROM akunting.transaksi) AS legacy_kredit,
       (SELECT COALESCE(SUM(kredit),0) FROM fin.jurnal_detail)  AS tenant_kredit,
       CASE WHEN (SELECT COUNT(*) FROM akunting.transaksi) = (SELECT COUNT(*) FROM fin.jurnal_detail)
             AND (SELECT COALESCE(SUM(debet),0) FROM akunting.transaksi)
                 = (SELECT COALESCE(SUM(debit),0) FROM fin.jurnal_detail)
             AND (SELECT COALESCE(SUM(kredit),0) FROM akunting.transaksi)
                 = (SELECT COALESCE(SUM(kredit),0) FROM fin.jurnal_detail)
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo '=== 3. Laba kotor per produk (qty, omzet, HPP) ==='
-- Bagian paling mudah salah: HPP tenant ada di baris FAKTUR, bukan di baris order.
WITH l AS (
  SELECT i.produk AS pid, SUM(i.jumlah) AS qty, SUM(i.subtotal) AS omzet,
         SUM(i.hpp_snapshot * i.jumlah) AS hpp
  FROM koperasi.sales_order_lapangan_item i
  JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id
  WHERE o.status IN ('SIAP_TAGIH','LUNAS')
  GROUP BY i.produk
), t AS (
  SELECT i.produk_id AS pid, SUM(i.kuantitas) AS qty, SUM(i.total) AS omzet,
         SUM(COALESCE(i.harga_beli,0) * COALESCE(i.kuantitas,0)) AS hpp
  FROM fin.faktur_penjualan_detail i
  JOIN fin.faktur_penjualan o ON i.faktur_penjualan_id = o.id
  WHERE COALESCE(o.dibatalkan,false) = false
  GROUP BY i.produk_id
)
SELECT l.pid, l.qty AS legacy_qty, t.qty AS tenant_qty,
       l.omzet AS legacy_omzet, t.omzet AS tenant_omzet,
       l.hpp AS legacy_hpp, t.hpp AS tenant_hpp,
       CASE WHEN l.qty = t.qty AND l.omzet = t.omzet AND l.hpp = t.hpp
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

\echo ''
\echo '=== 4. Laba-Rugi: omzet kredit, omzet tunai, HPP, beban ==='
WITH l AS (
  SELECT
    (SELECT COALESCE(SUM(d.total_faktur),0) FROM koperasi.piutang_customer_doc d
     WHERE d.status = 'AKTIF') AS omzet_kredit,
    (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k
     WHERE k.jenis = 'CASH_SALE') AS omzet_tunai,
    (SELECT COALESCE(SUM(i.hpp_snapshot * i.jumlah),0)
     FROM koperasi.sales_order_lapangan_item i
     JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id
     WHERE o.status IN ('SIAP_TAGIH','LUNAS')) AS hpp,
    (SELECT COALESCE(SUM(b.nilai),0) FROM koperasi.nota_sales_biaya b) AS beban
), t AS (
  SELECT
    (SELECT COALESCE(SUM(d.nilai),0) FROM fin.piutang_customer d
     WHERE d.status = 'AKTIF') AS omzet_kredit,
    -- Penanda tunai pada model tenant: faktur yang TIDAK melahirkan piutang.
    (SELECT COALESCE(SUM(f.total),0) FROM fin.faktur_penjualan f
     LEFT JOIN fin.piutang_customer p ON p.faktur_penjualan_id = f.id
     WHERE p.id IS NULL AND COALESCE(f.dibatalkan,false) = false) AS omzet_tunai,
    (SELECT COALESCE(SUM(COALESCE(i.harga_beli,0) * COALESCE(i.kuantitas,0)),0)
     FROM fin.faktur_penjualan_detail i
     JOIN fin.faktur_penjualan o ON i.faktur_penjualan_id = o.id
     WHERE COALESCE(o.dibatalkan,false) = false) AS hpp,
    (SELECT COALESCE(SUM(b.nilai),0) FROM fin.sales_trip_biaya b) AS beban
)
SELECT l.omzet_kredit AS legacy_kredit, t.omzet_kredit AS tenant_kredit,
       l.omzet_tunai AS legacy_tunai, t.omzet_tunai AS tenant_tunai,
       l.hpp AS legacy_hpp, t.hpp AS tenant_hpp,
       l.beban AS legacy_beban, t.beban AS tenant_beban,
       CASE WHEN l.omzet_kredit = t.omzet_kredit AND l.omzet_tunai = t.omzet_tunai
                 AND l.hpp = t.hpp AND l.beban = t.beban
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l, t;

\echo ''
\echo '=== 5. Beban per kategori (join lenyap di tenant) ==='
WITH l AS (
  SELECT kb.nama AS kategori, COALESCE(SUM(b.nilai),0) AS nilai
  FROM koperasi.nota_sales_biaya b
  JOIN koperasi.kategori_biaya_sales kb ON b.kategori = kb.id
  GROUP BY kb.nama
), t AS (
  SELECT COALESCE(NULLIF(b.kategori,''),'(tanpa kategori)') AS kategori,
         COALESCE(SUM(b.nilai),0) AS nilai
  FROM fin.sales_trip_biaya b GROUP BY 1
)
SELECT l.kategori, l.nilai AS legacy_nilai, t.nilai AS tenant_nilai,
       CASE WHEN l.nilai = t.nilai THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.kategori = l.kategori ORDER BY l.kategori;

\echo ''
\echo '=== 6. Rincian per baris: laba dan sisa piutang ==='
-- Termasuk baris RUGI (harga jual di bawah harga pokok) yang sengaja dimasukkan.
WITH l AS (
  SELECT i.produk AS pid, i.subtotal AS omzet,
         (i.subtotal - i.hpp_snapshot * i.jumlah) AS laba,
         (COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0)
          - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                      WHERE a.piutang_doc = d.id),0)) AS sisa
  FROM koperasi.sales_order_lapangan_item i
  JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id
  LEFT JOIN koperasi.piutang_customer_doc d ON d.sales_order = o.id
  WHERE o.id = 1
), t AS (
  SELECT i.produk_id AS pid, i.total AS omzet,
         (COALESCE(i.total,0) - COALESCE(i.harga_beli,0) * COALESCE(i.kuantitas,0)) AS laba,
         (COALESCE(d.nilai,0)
          - COALESCE((SELECT SUM(a.nilai) FROM fin.alokasi_penerimaan_piutang a
                      WHERE a.piutang_customer_id = d.id),0)) AS sisa
  FROM fin.faktur_penjualan_detail i
  JOIN fin.faktur_penjualan o ON i.faktur_penjualan_id = o.id
  LEFT JOIN fin.piutang_customer d ON d.faktur_penjualan_id = o.id
  WHERE o.id = 1
)
SELECT l.pid, l.omzet AS legacy_omzet, t.omzet AS tenant_omzet,
       l.laba AS legacy_laba, t.laba AS tenant_laba,
       l.sisa AS legacy_sisa, t.sisa AS tenant_sisa,
       CASE WHEN l.omzet = t.omzet AND l.laba = t.laba AND l.sisa = t.sisa
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.pid = l.pid ORDER BY l.pid;

\echo ''
\echo 'Selesai. Seluruh baris hasil harus SETARA.'
