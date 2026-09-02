-- =====================================================================================
-- Uji kesetaraan penagihan piutang dan pembalikannya (collectionCreate/collectionReverse)
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v13:
--   psql -h 127.0.0.1 -p 55507 -U uji -d postgres -f uji-kesetaraan-penagihan.sql
--
-- Berkas ini mengandaikan schema tenant bernama coll13.
--
-- SKENARIO
--   Piutang 1.000.000 dibawa sales dalam trip. Ditagih LUNAS di lapangan (tunai),
--   sehingga order asalnya menjadi LUNAS. Lalu penagihan itu DIBALIK seluruhnya.
--
-- LIMA HAL YANG DIUJI
--   1. Penagihan menurunkan sisa, menaikkan kas trip, dan memutakhirkan status nota.
--   2. Pelunasan penuh memajukan status order menjadi LUNAS.
--   3. Pembalikan memulihkan SEMUANYA: sisa, kas, status nota, dan status order.
--   4. Blok 5 intinya: nilai tertagih turunan memperbaiki diri sendiri, sedangkan
--      kolom legacy hanya benar bila pengurangnya tidak terlupa.
--   5. Blok 6 penjaganya: turunan itu memang terikat pada TRIP -- penerimaan kantor
--      yang tidak menunjuk trip tidak boleh ikut terhitung sebagai tertagih lapangan.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DELETE FROM coll13.reversal_log;
DELETE FROM coll13.sales_trip_kas;
DELETE FROM coll13.surat_perintah_sales_nota;
DELETE FROM coll13.alokasi_penerimaan_piutang;
DELETE FROM coll13.penerimaan_piutang;
DELETE FROM coll13.piutang_customer;
DELETE FROM coll13.faktur_penjualan;
DELETE FROM coll13.sales_order;
DELETE FROM coll13.sales_trip;
DELETE FROM coll13.surat_perintah_sales;
DELETE FROM coll13.customer;
DELETE FROM coll13.salesperson;
DELETE FROM coll13.gudang;
DELETE FROM coll13.toko;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

-- Nota bawaan legacy: nilai_tertagih adalah KOLOM dengan dua penulis.
CREATE TABLE koperasi.spj_sales_nota (id bigserial PRIMARY KEY, spj bigint,
  piutang_doc bigint, status varchar(32), nilai_tertagih numeric(18,2) DEFAULT 0);
INSERT INTO koperasi.spj_sales_nota (id, spj, piutang_doc, status, nilai_tertagih)
  VALUES (1, 1, 900, 'CARRIED', 0);

-- ---------- tenant ----------
INSERT INTO coll13.toko (id, nama, kode) VALUES (1, 'Toko Uji', 'T01');
INSERT INTO coll13.gudang (id, kode, nama, toko_id) VALUES (1, 'G01', 'Gudang', 1);
INSERT INTO coll13.customer (id, kode, nama) VALUES (5, 'C05', 'Toko Melati');
INSERT INTO coll13.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO coll13.surat_perintah_sales (id, nomor_dokumen, tanggal, salesperson_id, gudang_id,
    status, uang_muka_operasional)
  VALUES (1, 'SPJ-001', DATE '2026-08-01', 7, 1, 'ACTIVE', 0);
INSERT INTO coll13.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
    gudang_id, tanggal_berangkat, status, dibuat_pada, oleh)
  VALUES (1, 'TRIP-001', 1, 7, 1, DATE '2026-08-01', 'ACTIVE', now(), 'uji');
INSERT INTO coll13.sales_order (id, nomor_dokumen, tanggal, customer_id, salesperson_id, toko_id,
    total, status, dibuat_pada, oleh)
  VALUES (1, 'SO-001', DATE '2026-08-01', 5, 7, 1, 1000000, 'SIAP_TAGIH', now(), 'uji');
INSERT INTO coll13.faktur_penjualan (id, nomor_dokumen, nomor_faktur, tanggal, customer_id,
    salesperson_id, sales_order_id, toko_id, total, status, dibuat_pada, oleh)
  VALUES (1, 'INV-1', 'INV-1', DATE '2026-08-01', 5, 7, 1, 1, 1000000, 'AKTIF', now(), 'uji');
INSERT INTO coll13.piutang_customer (id, customer_id, salesperson_id, faktur_penjualan_id,
    nomor_faktur, tanggal, jatuh_tempo, nilai, terbayar, sisa, status, dibuat_pada, oleh)
  VALUES (900, 5, 7, 1, 'INV-1', DATE '2026-08-01', DATE '2026-08-31', 1000000, 0, 1000000,
          'TERBUKA', now(), 'uji');
INSERT INTO coll13.surat_perintah_sales_nota (id, surat_perintah_sales_id, piutang_customer_id,
    saldo_saat_assign, status, dibuat_pada, oleh)
  VALUES (1, 1, 900, 1000000, 'CARRIED', now(), 'uji');

\pset format aligned

-- =====================================================================================
-- PENAGIHAN LUNAS 1.000.000 TUNAI -- persis urutan collectionCreateTenant
-- =====================================================================================
INSERT INTO coll13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
    cara_bayar, nilai, keterangan, idempotency_key, sales_trip_id, status, dibuat_pada, oleh)
  VALUES (101, 'KWT-1-000101', DATE '2026-08-05', 5, 7, 'TUNAI', 1000000, 'tagih lapangan',
          'KWT-UNIK-1', 1, 'AKTIF', now(), 'uji');
INSERT INTO coll13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
    dibuat_pada, oleh) VALUES (101, 900, 1000000, now(), 'uji');
INSERT INTO coll13.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, keterangan,
    idempotency_key, waktu, dibuat_pada, oleh)
  VALUES (1, 'COLLECTION_CASH', 1000000, 'KWT-1-000101', 'Penagihan tunai', 'KAS-KWT-101',
          now(), now(), 'uji');
UPDATE coll13.surat_perintah_sales_nota SET status = 'PAID' WHERE id = 1;
UPDATE coll13.sales_order SET status = 'LUNAS', oleh = 'uji' WHERE id = 1;
UPDATE koperasi.spj_sales_nota SET nilai_tertagih = 1000000, status = 'PAID' WHERE id = 1;

\echo ''
\echo '== BLOK 1: sesudah penagihan lunas -- sisa nol, kas naik, nota PAID ==============='

SELECT
  (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
       FROM coll13.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)
     FROM coll13.piutang_customer d WHERE d.id = 900)                     AS "sisa piutang",
  (SELECT COALESCE(SUM(k.nominal),0) FROM coll13.sales_trip_kas k
    WHERE k.sales_trip_id = 1)                                            AS "saldo kas",
  (SELECT status FROM coll13.surat_perintah_sales_nota WHERE id = 1)      AS "status nota",
  CASE WHEN (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
               FROM coll13.alokasi_penerimaan_piutang a
              WHERE a.piutang_customer_id = d.id),0)
             FROM coll13.piutang_customer d WHERE d.id = 900) = 0
        AND (SELECT COALESCE(SUM(k.nominal),0) FROM coll13.sales_trip_kas k
              WHERE k.sales_trip_id = 1) = 1000000
        AND (SELECT status FROM coll13.surat_perintah_sales_nota WHERE id = 1) = 'PAID'
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 2: pelunasan penuh memajukan order menjadi LUNAS =========================='

SELECT status AS "status order",
       CASE WHEN status = 'LUNAS' THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM coll13.sales_order WHERE id = 1;

-- =====================================================================================
-- PEMBALIKAN -- persis urutan collectionReverseTenant
-- Kolom legacy SENGAJA tidak diturunkan: itulah yang diuji blok 5.
-- =====================================================================================
INSERT INTO coll13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, salesperson_id,
    cara_bayar, nilai, keterangan, idempotency_key, pembalik_dari_id, sales_trip_id, status,
    dibuat_pada, oleh)
  VALUES (102, 'REV-KWT-1-000101', DATE '2026-08-06', 5, 7, 'TUNAI', -1000000,
          'REVERSAL kwitansi KWT-1-000101: salah customer', 'REV-KWT-101', 101, 1, 'REVERSAL',
          now(), 'uji');
INSERT INTO coll13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
    dibuat_pada, oleh)
  SELECT 102, a.piutang_customer_id, -a.nilai, now(), 'uji'
    FROM coll13.alokasi_penerimaan_piutang a WHERE a.penerimaan_piutang_id = 101;
INSERT INTO coll13.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, keterangan,
    idempotency_key, waktu, dibuat_pada, oleh)
  VALUES (1, 'REVERSAL', -1000000, 'REV-KWT-101', 'Reversal penagihan tunai: salah customer',
          'REV-KWT-101', now(), now(), 'uji');
UPDATE coll13.surat_perintah_sales_nota SET status = 'PARTIAL_COLLECTED' WHERE id = 1;
UPDATE coll13.sales_order SET status = 'SIAP_TAGIH', oleh = 'uji'
  WHERE id = 1 AND status = 'LUNAS';
UPDATE coll13.penerimaan_piutang SET status = 'DIBATALKAN', dibatalkan = true,
  dibatalkan_pada = now(), alasan_batal = 'salah customer' WHERE id = 101;

\echo ''
\echo '== BLOK 3: pembalikan memulihkan sisa, kas, dan status dokumen asal ==============='

SELECT
  (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
       FROM coll13.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)
     FROM coll13.piutang_customer d WHERE d.id = 900)                  AS "sisa piutang",
  (SELECT COALESCE(SUM(k.nominal),0) FROM coll13.sales_trip_kas k
    WHERE k.sales_trip_id = 1)                                         AS "saldo kas",
  (SELECT status FROM coll13.penerimaan_piutang WHERE id = 101)        AS "status asal",
  CASE WHEN (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
               FROM coll13.alokasi_penerimaan_piutang a
              WHERE a.piutang_customer_id = d.id),0)
             FROM coll13.piutang_customer d WHERE d.id = 900) = 1000000
        AND (SELECT COALESCE(SUM(k.nominal),0) FROM coll13.sales_trip_kas k
              WHERE k.sales_trip_id = 1) = 0
        AND (SELECT status FROM coll13.penerimaan_piutang WHERE id = 101) = 'DIBATALKAN'
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 4: order MUNDUR dari LUNAS ke SIAP_TAGIH =================================='
\echo '   Jalur yang tidak teruji pada jalan SQL sebelumnya: di sana ordernya memang belum lunas.'

SELECT status AS "status order",
       CASE WHEN status = 'SIAP_TAGIH' THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM coll13.sales_order WHERE id = 1;

\echo ''
\echo '== BLOK 5 (INTI): nilai tertagih turunan memperbaiki diri SENDIRI ================='
\echo '   Kolom legacy sengaja TIDAK diturunkan -- meniru pengurang yang terlupa.'
\echo '   Legacy tetap 1.000.000 (salah); turunan tenant harus sudah 0.'

SELECT
  (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) AS "legacy (terlupa)",
  ten.tertagih                                                       AS "tenant (turunan)",
  CASE WHEN ten.tertagih = 0
        AND (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) = 1000000
       THEN 'LULUS (turunan benar, kolom yang terlupa salah)'
       ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE((SELECT SUM(a.nilai)
        FROM coll13.alokasi_penerimaan_piutang a
        JOIN coll13.penerimaan_piutang r ON a.penerimaan_piutang_id = r.id
        JOIN coll13.sales_trip t ON r.sales_trip_id = t.id
       WHERE a.piutang_customer_id = n.piutang_customer_id
         AND t.surat_perintah_sales_id = n.surat_perintah_sales_id), 0) AS tertagih
      FROM coll13.surat_perintah_sales_nota n WHERE n.id = 1) ten;

\echo ''
\echo '== BLOK 6 (PENJAGA): turunannya memang terikat TRIP =============================='
\echo '   Penerimaan kantor (sales_trip_id NULL) tidak boleh terhitung tertagih lapangan.'

INSERT INTO coll13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, cara_bayar,
    nilai, idempotency_key, status, dibuat_pada, oleh)
  VALUES (103, 'KWT-KANTOR', DATE '2026-08-07', 5, 'TRANSFER', 250000, 'KWT-UNIK-3', 'AKTIF',
          now(), 'uji');
INSERT INTO coll13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai,
    dibuat_pada, oleh) VALUES (103, 900, 250000, now(), 'uji');

SELECT
  (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
       FROM coll13.alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)
     FROM coll13.piutang_customer d WHERE d.id = 900)  AS "sisa piutang (turun 250rb)",
  ten.tertagih                                          AS "tertagih lapangan (tetap 0)",
  CASE WHEN ten.tertagih = 0
        AND (SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)
               FROM coll13.alokasi_penerimaan_piutang a
              WHERE a.piutang_customer_id = d.id),0)
             FROM coll13.piutang_customer d WHERE d.id = 900) = 750000
       THEN 'LULUS (pembayaran kantor tidak diakui sebagai hasil lapangan)'
       ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE((SELECT SUM(a.nilai)
        FROM coll13.alokasi_penerimaan_piutang a
        JOIN coll13.penerimaan_piutang r ON a.penerimaan_piutang_id = r.id
        JOIN coll13.sales_trip t ON r.sales_trip_id = t.id
       WHERE a.piutang_customer_id = n.piutang_customer_id
         AND t.surat_perintah_sales_id = n.surat_perintah_sales_id), 0) AS tertagih
      FROM coll13.surat_perintah_sales_nota n WHERE n.id = 1) ten;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
