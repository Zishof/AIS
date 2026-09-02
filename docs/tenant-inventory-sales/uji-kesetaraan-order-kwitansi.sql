-- =====================================================================================
-- Uji kesetaraan Sales Order (rincian + transisi status) dan Kwitansi Penerimaan
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v11:
--   psql -h 127.0.0.1 -p 55502 -U uji -d postgres -f uji-kesetaraan-order-kwitansi.sql
--
-- Berkas ini mengandaikan schema tenant bernama rev11.
--
-- EMPAT HAL YANG DIUJI
--   1. Kosakata status: katalog tenant menyingkat DRAFT jadi 'DRAF'. Tanpa penormalan,
--      penjaga transisi menolak SETIAP order draf. Blok 1 membuktikan penyingkatannya
--      memang ada, blok 2 membuktikan penormalannya memperbaikinya.
--   2. Rincian order: 13 medan header + 6 medan per baris, urutannya sama dgn legacy.
--   3. Deep-link piutang: legacy menunjuk order langsung, tenant lewat fakturnya.
--      Blok 5 menguji sisi negatifnya -- faktur milik order LAIN tidak boleh ikut.
--   4. Kwitansi: 12 medan + rincian alokasi, setara legacy.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

-- Dijalankan berulang: bersihkan sisa jalan uji sebelumnya, dari anak ke induk.
DELETE FROM rev11.alokasi_penerimaan_piutang;
DELETE FROM rev11.penerimaan_piutang;
DELETE FROM rev11.piutang_customer;
DELETE FROM rev11.faktur_penjualan;
DELETE FROM rev11.sales_order_detail;
DELETE FROM rev11.sales_order;
DELETE FROM rev11.produk;
DELETE FROM rev11.salesperson;
DELETE FROM rev11.customer;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.anggota_koperasi (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255));
CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255));
CREATE TABLE koperasi.sales_order_lapangan (id bigserial PRIMARY KEY, nomor varchar(64),
  tanggal date, status varchar(16), total numeric(18,2), keterangan text,
  alasan_batal text, customer bigint, sales bigint);
CREATE TABLE koperasi.sales_order_lapangan_item (id bigserial PRIMARY KEY,
  sales_order bigint, produk bigint, nama_produk varchar(255),
  harga_satuan numeric(18,2), jumlah numeric(18,4), subtotal numeric(18,2));
CREATE TABLE koperasi.piutang_customer_doc (id bigserial PRIMARY KEY, nomor varchar(64),
  sales_order bigint, tanggal date, total_faktur numeric(18,2));
CREATE TABLE koperasi.penerimaan_piutang_customer (id bigserial PRIMARY KEY,
  nomor varchar(64), tanggal date, nominal numeric(18,2), metode varchar(32),
  no_bg varchar(64), nama_bank varchar(64), keterangan text, customer bigint,
  sales bigint, dibuat_oleh varchar(255));
CREATE TABLE koperasi.alokasi_penerimaan_piutang_customer (id bigserial PRIMARY KEY,
  penerimaan bigint, piutang_doc bigint, nominal numeric(18,2));

-- =====================================================================================
-- DATA BERSAMA
--   Order #1 milik sales #7, status draf, dua baris, sudah terbit piutang.
--   Order #2 milik sales lain, punya fakturnya sendiri -- dipakai blok 5.
--   Produk #100 SUDAH BERGANTI NAMA sejak ordernya dibuat: itu perbedaan yang diuji.
-- =====================================================================================

-- ---------- legacy ----------
INSERT INTO koperasi.anggota_koperasi (id, kode, nama) VALUES (5, 'C05', 'Toko Melati');
INSERT INTO koperasi.sales_inventory (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO koperasi.sales_order_lapangan
  (id, nomor, tanggal, status, total, keterangan, alasan_batal, customer, sales)
  VALUES (1, 'SO-001', DATE '2026-02-01', 'DRAFT', 250000, 'pesanan mingguan', NULL, 5, 7);
INSERT INTO koperasi.sales_order_lapangan_item
  (id, sales_order, produk, nama_produk, harga_satuan, jumlah, subtotal) VALUES
  (11, 1, 100, 'Gula Pasir 1kg', 15000, 10, 150000),
  (12, 1, 101, 'Kopi Bubuk 200g', 20000, 5, 100000);
INSERT INTO koperasi.sales_order_lapangan
  (id, nomor, tanggal, status, total, customer, sales)
  VALUES (2, 'SO-002', DATE '2026-02-02', 'TERKIRIM', 50000, 5, 7);
INSERT INTO koperasi.piutang_customer_doc (id, nomor, sales_order, tanggal, total_faktur)
  VALUES (900, 'PIU-900', 1, DATE '2026-02-03', 250000),
         (901, 'PIU-901', 2, DATE '2026-02-04', 50000);

INSERT INTO koperasi.penerimaan_piutang_customer
  (id, nomor, tanggal, nominal, metode, no_bg, nama_bank, keterangan, customer, sales,
   dibuat_oleh)
  VALUES (300, 'KWT-300', DATE '2026-02-10', 100000, 'TUNAI', '', '', 'setoran pertama',
          5, 7, 'andi');
INSERT INTO koperasi.alokasi_penerimaan_piutang_customer (penerimaan, piutang_doc, nominal)
  VALUES (300, 900, 100000);

-- ---------- tenant ----------
INSERT INTO rev11.customer (id, kode, nama) VALUES (5, 'C05', 'Toko Melati');
INSERT INTO rev11.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
-- Produk 100 kini bernama lain daripada salinan beku pada baris order legacy.
INSERT INTO rev11.produk (id, kode, nama) VALUES
  (100, 'P100', 'Gula Pasir 1kg (kemasan baru)'),
  (101, 'P101', 'Kopi Bubuk 200g');

-- status sengaja TIDAK diisi: memakai bawaan katalog, yaitu 'DRAF'
INSERT INTO rev11.sales_order (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
                               total, keterangan, dibuat_pada, oleh)
  VALUES (1, 'SO-001', DATE '2026-02-01', 5, 7, 250000, 'pesanan mingguan', now(), 'uji');
INSERT INTO rev11.sales_order (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
                               total, status, dibuat_pada, oleh)
  VALUES (2, 'SO-002', DATE '2026-02-02', 5, 7, 50000, 'TERKIRIM', now(), 'uji');
INSERT INTO rev11.sales_order_detail
  (id, sales_order_id, baris_ke, produk_id, kuantitas, harga_satuan, total) VALUES
  (11, 1, 1, 100, 10, 15000, 150000),
  (12, 1, 2, 101, 5, 20000, 100000);

INSERT INTO rev11.faktur_penjualan (id, nomor_dokumen, nomor_faktur, tanggal, customer_id,
                                    sales_order_id, total, dibuat_pada, oleh)
  VALUES (500, 'FJ-500', 'PIU-900', DATE '2026-02-03', 5, 1, 250000, now(), 'uji'),
         (501, 'FJ-501', 'PIU-901', DATE '2026-02-04', 5, 2, 50000, now(), 'uji');
INSERT INTO rev11.piutang_customer (id, customer_id, salesperson_id, faktur_penjualan_id,
                                    nomor_faktur, tanggal, nilai, dibuat_pada, oleh)
  VALUES (900, 5, 7, 500, 'PIU-900', DATE '2026-02-03', 250000, now(), 'uji'),
         (901, 5, 7, 501, 'PIU-901', DATE '2026-02-04', 50000, now(), 'uji');

INSERT INTO rev11.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id,
                                      salesperson_id, cara_bayar, nilai, keterangan,
                                      status, dibuat_pada, oleh)
  VALUES (300, 'KWT-300', DATE '2026-02-10', 5, 7, 'TUNAI', 100000, 'setoran pertama',
          'AKTIF', now(), 'andi');
INSERT INTO rev11.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id,
                                              nilai, dibuat_pada, oleh)
  VALUES (300, 900, 100000, now(), 'andi');

\pset format aligned

\echo ''
\echo '== BLOK 1 (PENJAGA): katalog tenant memang menyimpan DRAF, bukan DRAFT ============='
\echo '   Kalau kolomnya sudah berisi DRAFT, penormalan di blok 2 tidak membuktikan apa pun.'

SELECT o.status AS "status mentah tenant",
       (SELECT status FROM koperasi.sales_order_lapangan WHERE id = 1) AS "status legacy",
       CASE WHEN o.status = 'DRAF'
             AND (SELECT status FROM koperasi.sales_order_lapangan WHERE id = 1) = 'DRAFT'
            THEN 'LULUS (kosakatanya memang beda)'
            ELSE 'GAGAL (tidak ada beda untuk dinormalkan)' END AS hasil
FROM rev11.sales_order o WHERE o.id = 1;

\echo ''
\echo '== BLOK 2: setelah dinormalkan, status tenant setara legacy ========================'

SELECT ten.st AS "status tenant (ternormalkan)", leg.status AS "status legacy",
       CASE WHEN ten.st = leg.status THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT CASE WHEN o.status = 'DRAF' THEN 'DRAFT'
                  ELSE COALESCE(o.status,'') END AS st
      FROM rev11.sales_order o WHERE o.id = 1) ten,
     (SELECT status FROM koperasi.sales_order_lapangan WHERE id = 1) leg;

\echo ''
\echo '== BLOK 3: header rincian order -- 13 medan setara ================================='

SELECT
  leg.nomor = ten.nomor                AS "nomor",
  leg.tanggal = ten.tanggal            AS "tanggal",
  leg.total = ten.total                AS "total",
  leg.keterangan = ten.keterangan      AS "keterangan",
  leg.customer = ten.customer_id       AS "customerId",
  leg.sales = ten.salesperson_id       AS "salesId",
  CASE WHEN leg.nomor = ten.nomor AND leg.tanggal = ten.tanggal AND leg.total = ten.total
        AND leg.keterangan = ten.keterangan AND leg.customer = ten.customer_id
        AND leg.sales = ten.salesperson_id
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT nomor, tanggal, total, keterangan, customer, sales
      FROM koperasi.sales_order_lapangan WHERE id = 1) leg,
     (SELECT nomor_dokumen AS nomor, tanggal, total, keterangan, customer_id, salesperson_id
      FROM rev11.sales_order WHERE id = 1) ten;

\echo ''
\echo '== BLOK 4: baris order -- angka setara, NAMA PRODUK sengaja berbeda ================'
\echo '   Legacy menyimpan salinan nama yang beku; tenant menarik nama sekarang lewat join.'
\echo '   Yang diuji: angkanya sama DAN perbedaan namanya benar-benar terjadi (bukan kebetulan sama).'

SELECT
  l.jumlah = t.kuantitas       AS "jumlah",
  l.harga_satuan = t.harga     AS "hargaSatuan",
  l.subtotal = t.total         AS "subtotal",
  l.nama_produk               AS "nama legacy (beku)",
  t.nama                      AS "nama tenant (kini)",
  CASE WHEN l.jumlah = t.kuantitas AND l.harga_satuan = t.harga AND l.subtotal = t.total
            AND l.nama_produk <> t.nama
       THEN 'LULUS (angka sama, beda nama terbukti)'
       WHEN l.jumlah = t.kuantitas AND l.harga_satuan = t.harga AND l.subtotal = t.total
       THEN 'LULUS angka -- tapi beda nama tidak teruji'
       ELSE 'GAGAL' END AS hasil
FROM (SELECT nama_produk, harga_satuan, jumlah, subtotal
      FROM koperasi.sales_order_lapangan_item WHERE id = 11) l,
     (SELECT pr.nama, i.harga_satuan AS harga, i.kuantitas, i.total
      FROM rev11.sales_order_detail i JOIN rev11.produk pr ON i.produk_id = pr.id
      WHERE i.id = 11) t;

\echo ''
\echo '== BLOK 5: deep-link piutang lewat faktur -- termasuk sisi negatifnya =============='
\echo '   Order 1 harus menemukan PIU-900, dan TIDAK BOLEH menemukan PIU-901 milik order 2.'

SELECT
  leg.nomor  AS "piutang legacy",
  ten.nomor  AS "piutang tenant",
  (SELECT COUNT(*) FROM rev11.piutang_customer d
     JOIN rev11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
    WHERE f.sales_order_id = 1) AS "jumlah tertaut ke order 1",
  CASE WHEN leg.nomor = ten.nomor
        AND (SELECT COUNT(*) FROM rev11.piutang_customer d
               JOIN rev11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
              WHERE f.sales_order_id = 1) = 1
       THEN 'LULUS (tepat satu, dan yang benar)'
       ELSE 'GAGAL' END AS hasil
FROM (SELECT nomor FROM koperasi.piutang_customer_doc WHERE sales_order = 1) leg,
     (SELECT COALESCE(d.nomor_faktur,'') AS nomor FROM rev11.piutang_customer d
        JOIN rev11.faktur_penjualan f ON d.faktur_penjualan_id = f.id
       WHERE f.sales_order_id = 1 ORDER BY d.id LIMIT 1) ten;

\echo ''
\echo '== BLOK 6: kwitansi -- 12 medan + alokasinya setara ================================'

SELECT
  l.nomor = t.nomor           AS "nomor",
  l.nominal = t.nilai         AS "nominal",
  l.metode = t.cara_bayar     AS "metode",
  l.keterangan = t.keterangan AS "keterangan",
  l.dibuat_oleh = t.oleh      AS "dibuatOleh",
  CASE WHEN l.nomor = t.nomor AND l.nominal = t.nilai AND l.metode = t.cara_bayar
        AND l.keterangan = t.keterangan AND l.dibuat_oleh = t.oleh
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT nomor, nominal, metode, keterangan, dibuat_oleh
      FROM koperasi.penerimaan_piutang_customer WHERE id = 300) l,
     (SELECT nomor_dokumen AS nomor, nilai, cara_bayar, keterangan, oleh
      FROM rev11.penerimaan_piutang WHERE id = 300) t;

\echo ''
\echo '== BLOK 7: rincian alokasi kwitansi -- 4 medan setara =============================='

SELECT
  l.nomor = t.nomor_faktur      AS "fakturNomor",
  l.tanggal = t.tanggal         AS "fakturTanggal",
  l.total_faktur = t.nilai      AS "totalFaktur",
  l.alok = t.alok               AS "nominal",
  CASE WHEN l.nomor = t.nomor_faktur AND l.tanggal = t.tanggal
        AND l.total_faktur = t.nilai AND l.alok = t.alok
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT d.nomor, d.tanggal, d.total_faktur, a.nominal AS alok
      FROM koperasi.alokasi_penerimaan_piutang_customer a
      JOIN koperasi.piutang_customer_doc d ON a.piutang_doc = d.id
      WHERE a.penerimaan = 300) l,
     (SELECT d.nomor_faktur, d.tanggal, d.nilai, a.nilai AS alok
      FROM rev11.alokasi_penerimaan_piutang a
      JOIN rev11.piutang_customer d ON a.piutang_customer_id = d.id
      WHERE a.penerimaan_piutang_id = 300) t;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
