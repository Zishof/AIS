-- =====================================================================================
-- Uji kesetaraan rincian sesi trip (tripDetail)
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v16:
--   psql -h 127.0.0.1 -p 55511 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-rincian-trip.sql
--
-- Berkas ini mengandaikan schema tenant bernama det16.
--
-- SKENARIO
--   Panjar 500.000. Jual tunai 300.000. Tagih tunai 200.000. Biaya tunai 100.000.
--   Bayar pemasok 200.000. Setor ke pemilik 400.000. Kas seharusnya = 300.000.
--   Barang: Gula 10 dibawa (6 terjual, 3 kembali, 1 rusak); Kopi 5 DIRENCANAKAN tetapi
--   tidak jadi dimuat.
--
-- EMPAT HAL YANG DIUJI
--   1. Rumus kas tenant setara penjumlahan bertanda buku kas legacy.
--   2. Blok 2 penjaganya: memilah per jenis tanpa OPENING_ADVANCE menghasilkan angka
--      lain -- jadi contohnya benar-benar membedakan.
--   3. Barang yang DIRENCANAKAN tetapi tidak dimuat harus tetap muncul (LEFT JOIN).
--      Blok 4 penjaganya: INNER JOIN akan menghilangkannya.
--   4. nilaiTertagih nota bawaan diturunkan, bukan dibaca dari kolom.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

-- Buku kas sesi legacy: satu tabel, bertanda, sembilan jenis.
CREATE TABLE koperasi.nota_sales_kas (id bigserial PRIMARY KEY, sesi bigint,
  jenis varchar(32), nominal numeric(18,2));
INSERT INTO koperasi.nota_sales_kas (sesi, jenis, nominal) VALUES
  (1, 'OPENING_ADVANCE',   500000),
  (1, 'CASH_SALE',         300000),
  (1, 'COLLECTION_CASH',   200000),
  (1, 'EXPENSE_CASH',     -100000),
  (1, 'PURCHASE_PAYMENT', -200000),
  (1, 'OWNER_DEPOSIT',    -400000);

\pset format aligned

\echo ''
\echo '== BLOK 1: kas fisik seharusnya -- setara buku kas legacy ========================='

SELECT leg.saldo AS "legacy", ten.saldo AS "tenant",
       CASE WHEN leg.saldo = ten.saldo AND ten.saldo = 300000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE(SUM(nominal),0) AS saldo FROM koperasi.nota_sales_kas WHERE sesi = 1) leg,
     (SELECT COALESCE(SUM(nominal),0) AS saldo FROM det16.sales_trip_kas
       WHERE sales_trip_id = 1) ten;

\echo ''
\echo '== BLOK 2 (PENJAGA): contohnya membedakan -- tanpa panjar angkanya lain =========='
\echo '   Rumus yang melupakan OPENING_ADVANCE menghasilkan -200.000, bukan 300.000.'

SELECT COALESCE(SUM(nominal),0) AS "tanpa panjar",
       CASE WHEN COALESCE(SUM(nominal),0) = -200000
            THEN 'LULUS (contoh benar-benar membedakan)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
FROM det16.sales_trip_kas
WHERE sales_trip_id = 1 AND jenis <> 'OPENING_ADVANCE';

\echo ''
\echo '== BLOK 3: pemilahan per jenis setara legacy ======================================'

SELECT
  t.penjualan_tunai, t.biaya_tunai, t.bayar_beli_tunai, t.setoran,
  CASE WHEN t.penjualan_tunai = l.penjualan_tunai AND t.biaya_tunai = l.biaya_tunai
        AND t.bayar_beli_tunai = l.bayar_beli_tunai AND t.setoran = l.setoran
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM
 (SELECT COALESCE(SUM(CASE WHEN jenis='CASH_SALE' THEN nominal ELSE 0 END),0) AS penjualan_tunai,
         COALESCE(SUM(CASE WHEN jenis='EXPENSE_CASH' THEN -nominal ELSE 0 END),0) AS biaya_tunai,
         COALESCE(SUM(CASE WHEN jenis='PURCHASE_PAYMENT' THEN -nominal ELSE 0 END),0)
           AS bayar_beli_tunai,
         COALESCE(SUM(CASE WHEN jenis='OWNER_DEPOSIT' THEN -nominal ELSE 0 END),0) AS setoran
    FROM det16.sales_trip_kas WHERE sales_trip_id = 1) t,
 (SELECT COALESCE(SUM(CASE WHEN jenis='CASH_SALE' THEN nominal ELSE 0 END),0) AS penjualan_tunai,
         COALESCE(SUM(CASE WHEN jenis='EXPENSE_CASH' THEN -nominal ELSE 0 END),0) AS biaya_tunai,
         COALESCE(SUM(CASE WHEN jenis='PURCHASE_PAYMENT' THEN -nominal ELSE 0 END),0)
           AS bayar_beli_tunai,
         COALESCE(SUM(CASE WHEN jenis='OWNER_DEPOSIT' THEN -nominal ELSE 0 END),0) AS setoran
    FROM koperasi.nota_sales_kas WHERE sesi = 1) l;

\echo ''
\echo '== BLOK 4: barang DIRENCANAKAN tetapi tidak dimuat tetap muncul ==================='
\echo '   Kopi direncanakan 5 dan tidak jadi dimuat; ia harus tampil dengan qtyDimuat nol.'

SELECT COUNT(*) AS "jumlah baris barang",
       COALESCE(SUM(CASE WHEN b.id IS NULL THEN 1 ELSE 0 END),0) AS "yang belum dimuat",
       CASE WHEN COUNT(*) = 2
             AND COALESCE(SUM(CASE WHEN b.id IS NULL THEN 1 ELSE 0 END),0) = 1
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM det16.surat_perintah_sales_detail d
LEFT JOIN det16.sales_trip_barang b
       ON b.sales_trip_id = 1 AND b.produk_id = d.produk_id
WHERE d.surat_perintah_sales_id = 1;

\echo ''
\echo '== BLOK 5 (PENJAGA): INNER JOIN akan MENGHILANGKAN barang yang belum dimuat ======='

SELECT COUNT(*) AS "baris dgn INNER JOIN",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS (LEFT JOIN memang menentukan)'
            ELSE 'GAGAL (penjaga tidak membuktikan apa-apa)' END AS hasil
FROM det16.surat_perintah_sales_detail d
JOIN det16.sales_trip_barang b
       ON b.sales_trip_id = 1 AND b.produk_id = d.produk_id
WHERE d.surat_perintah_sales_id = 1;

\echo ''
\echo '== BLOK 6: nilai tertagih nota bawaan DITURUNKAN dari alokasi ====================='

SELECT ten.tertagih AS "tertagih (turunan)",
       (SELECT COALESCE(SUM(nilai),0) FROM det16.alokasi_penerimaan_piutang
         WHERE piutang_customer_id = 900) AS "seluruh alokasi piutang",
       CASE WHEN ten.tertagih = 200000 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE((SELECT SUM(a.nilai)
        FROM det16.alokasi_penerimaan_piutang a
        JOIN det16.penerimaan_piutang r ON a.penerimaan_piutang_id = r.id
        JOIN det16.sales_trip t ON r.sales_trip_id = t.id
       WHERE a.piutang_customer_id = n.piutang_customer_id
         AND t.surat_perintah_sales_id = n.surat_perintah_sales_id), 0) AS tertagih
      FROM det16.surat_perintah_sales_nota n WHERE n.id = 1) ten;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
