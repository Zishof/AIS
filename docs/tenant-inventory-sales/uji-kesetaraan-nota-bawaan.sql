-- =====================================================================================
-- Uji kesetaraan nota piutang yang dibawa sales (migrasi v13)
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v13:
--   psql -h 127.0.0.1 -p 55506 -U uji -d postgres -f uji-kesetaraan-nota-bawaan.sql
--
-- Berkas ini mengandaikan schema tenant bernama nota13.
--
-- SKENARIO
--   Piutang 1.000.000 jatuh tempo 2026-07-31. Sebelum berangkat sudah terbayar 200.000,
--   sehingga sisa saat nota diserahkan = 800.000. Di lapangan tertagih 300.000.
--   Lalu penagihan 300.000 itu DIBALIK.
--
-- LIMA HAL YANG DIUJI
--   1. nilai_tertagih yang DITURUNKAN sama dengan kolom tersimpan legacy.
--   2. Blok 3 intinya: sesudah dibalik, angka turunan memperbaiki diri SENDIRI --
--      sedangkan kolom legacy hanya benar bila pengurangnya tidak terlupa.
--   3. nilai_awal / jatuh_tempo / customer ditarik lewat join, setara salinan legacy.
--   4. saldo_saat_assign adalah POTRET: alokasi sesudahnya tidak boleh mengubahnya.
--   5. Satu piutang tidak dapat ditugaskan dua kali pada SPJ yang sama.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DELETE FROM nota13.surat_perintah_sales_nota;
DELETE FROM nota13.alokasi_penerimaan_piutang;
DELETE FROM nota13.penerimaan_piutang;
DELETE FROM nota13.piutang_customer;
DELETE FROM nota13.sales_trip;
DELETE FROM nota13.surat_perintah_sales;
DELETE FROM nota13.customer;
DELETE FROM nota13.salesperson;
DELETE FROM nota13.gudang;
DELETE FROM nota13.toko;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

-- Nota bawaan legacy: nilai_tertagih adalah KOLOM yang dinaikkan penagihan dan
-- diturunkan pembalikan -- dua penulis untuk satu angka.
CREATE TABLE koperasi.spj_sales_nota (id bigserial PRIMARY KEY, spj bigint,
  piutang_doc bigint, customer bigint, nilai_awal numeric(18,2),
  saldo_saat_assign numeric(18,2), jatuh_tempo date, status varchar(32),
  nilai_tertagih numeric(18,2) DEFAULT 0);

-- ---------- legacy ----------
INSERT INTO koperasi.spj_sales_nota (id, spj, piutang_doc, customer, nilai_awal,
    saldo_saat_assign, jatuh_tempo, status, nilai_tertagih)
  VALUES (1, 1, 900, 5, 1000000, 800000, DATE '2026-07-31', 'CARRIED', 0);

-- ---------- tenant ----------
INSERT INTO nota13.toko (id, nama, kode) VALUES (1, 'Toko Uji', 'T01');
INSERT INTO nota13.gudang (id, kode, nama, toko_id) VALUES (1, 'G01', 'Gudang', 1);
INSERT INTO nota13.customer (id, kode, nama) VALUES (5, 'C05', 'Toko Melati');
INSERT INTO nota13.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO nota13.surat_perintah_sales (id, nomor_dokumen, tanggal, salesperson_id,
    gudang_id, status) VALUES (1, 'SPJ-001', DATE '2026-07-01', 7, 1, 'APPROVED');
INSERT INTO nota13.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
    gudang_id, tanggal_berangkat, status, dibuat_pada, oleh)
  VALUES (1, 'TRIP-001', 1, 7, 1, DATE '2026-07-02', 'ACTIVE', now(), 'uji');
INSERT INTO nota13.piutang_customer (id, customer_id, salesperson_id, nomor_faktur, tanggal,
    jatuh_tempo, nilai, dibuat_pada, oleh)
  VALUES (900, 5, 7, 'INV-900', DATE '2026-07-01', DATE '2026-07-31', 1000000, now(), 'uji');

-- Pembayaran 200.000 SEBELUM nota diserahkan (tidak lewat trip: sales_trip_id NULL).
INSERT INTO nota13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, cara_bayar,
    nilai, status, dibuat_pada, oleh)
  VALUES (10, 'KWT-10', DATE '2026-07-01', 5, 'TUNAI', 200000, 'AKTIF', now(), 'uji');
INSERT INTO nota13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id,
    nilai, dibuat_pada, oleh) VALUES (10, 900, 200000, now(), 'uji');

-- Nota diserahkan: potret sisanya 800.000. TANPA kolom nilai_awal/jatuh_tempo/customer.
INSERT INTO nota13.surat_perintah_sales_nota (id, surat_perintah_sales_id,
    piutang_customer_id, saldo_saat_assign, status, dibuat_pada, oleh)
  VALUES (1, 1, 900, 800000, 'CARRIED', now(), 'uji');

\pset format aligned

\echo ''
\echo '== BLOK 1: medan salinan legacy ditarik lewat join, dan setara ====================='

SELECT
  l.nilai_awal   = p.nilai        AS "nilaiAwal",
  l.jatuh_tempo  = p.jatuh_tempo  AS "jatuhTempo",
  l.customer     = p.customer_id  AS "customer",
  CASE WHEN l.nilai_awal = p.nilai AND l.jatuh_tempo = p.jatuh_tempo
        AND l.customer = p.customer_id
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM koperasi.spj_sales_nota l,
     (SELECT p.nilai, p.jatuh_tempo, p.customer_id
        FROM nota13.surat_perintah_sales_nota n
        JOIN nota13.piutang_customer p ON n.piutang_customer_id = p.id
       WHERE n.id = 1) p
WHERE l.id = 1;

-- =====================================================================================
-- Penagihan di lapangan 300.000 -- penerimaan MENUNJUK tripnya (kolom dari bundel v10)
-- =====================================================================================
INSERT INTO nota13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id,
    salesperson_id, cara_bayar, nilai, status, sales_trip_id, dibuat_pada, oleh)
  VALUES (11, 'KWT-11', DATE '2026-07-05', 5, 7, 'TUNAI', 300000, 'AKTIF', 1, now(), 'uji');
INSERT INTO nota13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id,
    nilai, dibuat_pada, oleh) VALUES (11, 900, 300000, now(), 'uji');

UPDATE koperasi.spj_sales_nota SET nilai_tertagih = 300000, status = 'PARTIAL_COLLECTED'
  WHERE id = 1;

\echo ''
\echo '== BLOK 2: nilai tertagih yang DITURUNKAN setara kolom tersimpan legacy ==========='

SELECT
  (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) AS "legacy (kolom)",
  ten.tertagih                                                       AS "tenant (turunan)",
  CASE WHEN (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) = ten.tertagih
        AND ten.tertagih = 300000
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE((SELECT SUM(a.nilai)
        FROM nota13.alokasi_penerimaan_piutang a
        JOIN nota13.penerimaan_piutang r ON a.penerimaan_piutang_id = r.id
        JOIN nota13.sales_trip t ON r.sales_trip_id = t.id
       WHERE a.piutang_customer_id = n.piutang_customer_id
         AND t.surat_perintah_sales_id = n.surat_perintah_sales_id), 0) AS tertagih
      FROM nota13.surat_perintah_sales_nota n WHERE n.id = 1) ten;

-- =====================================================================================
-- Penagihan DIBALIK: alokasi pembalik bernilai NEGATIF.
-- Kolom legacy SENGAJA tidak diturunkan di sini -- itulah yang diuji blok 3.
-- =====================================================================================
INSERT INTO nota13.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id,
    salesperson_id, cara_bayar, nilai, status, sales_trip_id, pembalik_dari_id,
    dibuat_pada, oleh)
  VALUES (12, 'REV-KWT-11', DATE '2026-07-06', 5, 7, 'TUNAI', -300000, 'REVERSAL', 1, 11,
          now(), 'uji');
INSERT INTO nota13.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id,
    nilai, dibuat_pada, oleh) VALUES (12, 900, -300000, now(), 'uji');

\echo ''
\echo '== BLOK 3 (INTI): sesudah dibalik, angka turunan memperbaiki diri SENDIRI ========='
\echo '   Kolom legacy sengaja TIDAK diturunkan di sini -- meniru pengurang yang terlupa.'
\echo '   Legacy tetap 300.000 (salah); turunan tenant harus sudah 0.'

SELECT
  (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) AS "legacy (terlupa)",
  ten.tertagih                                                       AS "tenant (turunan)",
  CASE WHEN ten.tertagih = 0
        AND (SELECT nilai_tertagih FROM koperasi.spj_sales_nota WHERE id = 1) = 300000
       THEN 'LULUS (turunan benar, kolom yang terlupa salah)'
       ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE((SELECT SUM(a.nilai)
        FROM nota13.alokasi_penerimaan_piutang a
        JOIN nota13.penerimaan_piutang r ON a.penerimaan_piutang_id = r.id
        JOIN nota13.sales_trip t ON r.sales_trip_id = t.id
       WHERE a.piutang_customer_id = n.piutang_customer_id
         AND t.surat_perintah_sales_id = n.surat_perintah_sales_id), 0) AS tertagih
      FROM nota13.surat_perintah_sales_nota n WHERE n.id = 1) ten;

\echo ''
\echo '== BLOK 4: saldo_saat_assign adalah POTRET -- alokasi sesudahnya tidak mengubahnya='
\echo '   Sisa piutang sekarang 800.000 lagi (200rb dibayar, 300rb ditagih, 300rb dibalik),'
\echo '   tetapi yang diuji bukan itu: potretnya harus tetap 800.000 apa pun yang terjadi.'

SELECT
  n.saldo_saat_assign AS "potret saat diserahkan",
  (p.nilai - COALESCE((SELECT SUM(a.nilai) FROM nota13.alokasi_penerimaan_piutang a
                       WHERE a.piutang_customer_id = p.id), 0)) AS "sisa sekarang",
  CASE WHEN n.saldo_saat_assign = 800000 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM nota13.surat_perintah_sales_nota n
JOIN nota13.piutang_customer p ON n.piutang_customer_id = p.id
WHERE n.id = 1;

\echo ''
\echo '== BLOK 5: satu piutang tidak dapat ditugaskan dua kali pada SPJ yang sama ========'
\echo '   Urutan serial dimajukan lebih dulu SUPAYA yang menolak benar-benar batasan unik'
\echo '   uq_..._sps_nota -- bukan primary key yang kebetulan bentrok.'

SELECT setval('nota13.surat_perintah_sales_nota_id_seq', 500, true);

\set ON_ERROR_STOP off
INSERT INTO nota13.surat_perintah_sales_nota (surat_perintah_sales_id, piutang_customer_id,
    saldo_saat_assign, status, dibuat_pada, oleh)
  VALUES (1, 900, 800000, 'ASSIGNED', now(), 'uji');
\set ON_ERROR_STOP on

SELECT COUNT(*) AS "penugasan untuk piutang 900",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS (penugasan ganda ditolak)'
            ELSE 'GAGAL (satu tagihan terhitung dua kali pada rekap)' END AS hasil
FROM nota13.surat_perintah_sales_nota
WHERE surat_perintah_sales_id = 1 AND piutang_customer_id = 900;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
