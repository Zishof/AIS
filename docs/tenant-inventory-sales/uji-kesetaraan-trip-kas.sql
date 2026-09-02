-- =====================================================================================
-- Uji kesetaraan saldo kas sesi, setoran, dan status trip
-- =====================================================================================
--
-- Uji ini lahir dari sebuah CACAT YANG SUDAH TERKIRIM.
--
-- Jalur tenant tripList mula-mula menghitung saldo kas sebagai SUM(sales_trip_setoran.nilai)
-- -- yakni TOTAL SETORAN. Legacy menghitungnya sebagai SUM(nota_sales_kas.nominal), dan pada
-- buku kas itu penjualan tunai bertanda positif sedangkan setoran DINEGATIFKAN. Jadi yang
-- dimaksud legacy adalah KAS YANG MASIH DIPEGANG SALES, bukan yang sudah disetor.
--
-- Untuk trip bertunai 1.000.000 dan setoran 800.000: legacy 200.000, versi keliru 800.000.
-- Empat kali lipat, dan pada kolom yang dibaca sebagai uang di tangan.
--
-- Uji kesetaraan sebelumnya tidak memuat blok kas, sehingga cacat ini lolos. Blok 1 di bawah
-- ada supaya tidak terulang.
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--   psql -h 127.0.0.1 -p 55443 -U uji -d postgres -f uji-kesetaraan-trip-kas.sql
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.nota_sales_session (id bigserial PRIMARY KEY, nomor varchar(64),
  status varchar(32));
-- Satu buku kas untuk dua jenis; setoran disimpan NEGATIF oleh catatKas().
CREATE TABLE koperasi.nota_sales_kas (id bigserial PRIMARY KEY, sesi bigint,
  jenis varchar(32), nominal numeric(18,2), referensi varchar(64), keterangan text);

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.nota_sales_session (id, nomor, status) VALUES (900, 'NSS-1', 'ACTIVE');
INSERT INTO koperasi.nota_sales_kas (sesi, jenis, nominal) VALUES
  (900, 'CASH_SALE',      600000),
  (900, 'CASH_SALE',      400000),
  (900, 'OWNER_DEPOSIT', -800000);   -- dinegatifkan, sesuai catatKas(keluar=true)

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM exp.sales_trip_setoran;
DELETE FROM exp.sales_trip_nota;
DELETE FROM exp.sales_trip;
DELETE FROM exp.customer;
DELETE FROM exp.salesperson;

INSERT INTO exp.salesperson (id, kode, nama) VALUES (5, 'S-1', 'Budi');
INSERT INTO exp.customer (id, kode, nama) VALUES (7, 'C-1', 'Warung');
INSERT INTO exp.sales_trip (id, nomor_dokumen, salesperson_id, tanggal_berangkat, status)
  VALUES (900, 'NSS-1', 5, '2026-03-01', 'ACTIVE');
-- Bagian tunai tiap nota; model tenant memisahkan tunai dan kredit per nota.
INSERT INTO exp.sales_trip_nota (sales_trip_id, nomor_nota, tanggal, customer_id, total, tunai, kredit)
  VALUES (900, 'N-1', '2026-03-01', 7, 600000, 600000, 0),
         (900, 'N-2', '2026-03-01', 7, 400000, 400000, 0);
-- Setoran: tabel khusus, nilainya POSITIF.
INSERT INTO exp.sales_trip_setoran (sales_trip_id, tanggal, cara_bayar, nilai)
  VALUES (900, '2026-03-01', 'TUNAI', 800000);

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. SALDO KAS SESI -- blok yang seharusnya menangkap cacat itu ==='
SELECT
  (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k WHERE k.sesi = 900)
    AS legacy_saldo_kas,
  (COALESCE((SELECT SUM(n.tunai) FROM exp.sales_trip_nota n WHERE n.sales_trip_id = 900),0)
   - COALESCE((SELECT SUM(s.nilai) FROM exp.sales_trip_setoran s WHERE s.sales_trip_id = 900),0))
    AS tenant_saldo_kas,
  (SELECT COALESCE(SUM(s.nilai),0) FROM exp.sales_trip_setoran s WHERE s.sales_trip_id = 900)
    AS versi_keliru_total_setoran,
  CASE WHEN (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k WHERE k.sesi = 900)
            = (COALESCE((SELECT SUM(n.tunai) FROM exp.sales_trip_nota n WHERE n.sales_trip_id = 900),0)
               - COALESCE((SELECT SUM(s.nilai) FROM exp.sales_trip_setoran s WHERE s.sales_trip_id = 900),0))
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo '--- bukti bahwa versi keliru memang berbeda (harus BERBEDA) ---'
SELECT CASE WHEN (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k WHERE k.sesi = 900)
                 <> (SELECT COALESCE(SUM(s.nilai),0) FROM exp.sales_trip_setoran s WHERE s.sales_trip_id = 900)
            THEN 'BERBEDA -- uji ini memang membedakan keduanya'
            ELSE '*** uji tidak membedakan, contohnya kurang tajam ***' END AS penjaga;

\echo ''
\echo '=== 2. Total setoran (angkanya sendiri tetap harus setara) ==='
SELECT
  (SELECT COALESCE(SUM(-k.nominal),0) FROM koperasi.nota_sales_kas k
   WHERE k.sesi = 900 AND k.jenis = 'OWNER_DEPOSIT') AS legacy_setoran,
  (SELECT COALESCE(SUM(s.nilai),0) FROM exp.sales_trip_setoran s WHERE s.sales_trip_id = 900)
    AS tenant_setoran,
  CASE WHEN (SELECT COALESCE(SUM(-k.nominal),0) FROM koperasi.nota_sales_kas k
             WHERE k.sesi = 900 AND k.jenis = 'OWNER_DEPOSIT')
            = (SELECT COALESCE(SUM(s.nilai),0) FROM exp.sales_trip_setoran s
               WHERE s.sales_trip_id = 900)
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo '=== 3. Penjualan tunai sesi ==='
SELECT
  (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k
   WHERE k.sesi = 900 AND k.jenis = 'CASH_SALE') AS legacy_tunai,
  (SELECT COALESCE(SUM(n.tunai),0) FROM exp.sales_trip_nota n WHERE n.sales_trip_id = 900)
    AS tenant_tunai,
  CASE WHEN (SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k
             WHERE k.sesi = 900 AND k.jenis = 'CASH_SALE')
            = (SELECT COALESCE(SUM(n.tunai),0) FROM exp.sales_trip_nota n WHERE n.sales_trip_id = 900)
       THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo '=== 4. Perpindahan status trip ACTIVE -> RETURNED ==='
UPDATE koperasi.nota_sales_session SET status = 'RETURNED' WHERE id = 900 AND status = 'ACTIVE';
UPDATE exp.sales_trip SET status = 'RETURNED' WHERE id = 900 AND status = 'ACTIVE';
SELECT (SELECT status FROM koperasi.nota_sales_session WHERE id = 900) AS legacy_status,
       (SELECT status FROM exp.sales_trip WHERE id = 900) AS tenant_status,
       CASE WHEN (SELECT status FROM koperasi.nota_sales_session WHERE id = 900)
                 = (SELECT status FROM exp.sales_trip WHERE id = 900)
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

\echo ''
\echo 'Selesai. Blok 1-4 harus SETARA, dan penjaga pada blok 1 harus BERBEDA.'
