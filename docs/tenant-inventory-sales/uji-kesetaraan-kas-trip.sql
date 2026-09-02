-- =====================================================================================
-- Uji kesetaraan buku kas trip (migrasi v12) -- celah C-11
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v12:
--   psql -h 127.0.0.1 -p 55505 -U uji -d postgres -f uji-kesetaraan-kas-trip.sql
--
-- Berkas ini mengandaikan schema tenant bernama kas12.
--
-- SKENARIO -- persis angka yang dipakai catatan C-11:
--   panjar 500.000, penjualan tunai 300.000, penagihan tunai 200.000,
--   biaya tunai 100.000, setoran ke pemilik 400.000.
--   Saldo kas yang benar: 500 + 300 + 200 - 100 - 400 = 500.000.
--
-- ENAM HAL YANG DIUJI (delapan blok, tiga di antaranya penjaga)
--   1. Buku kas tenant menghasilkan saldo yang SAMA dengan buku kas legacy.
--   2. Blok 2 penjaganya, dan inilah alasan v12 ada: rumus LAMA (nota tunai - setoran)
--      menghasilkan -100.000. Bukan selisih kecil -- BEDA TANDA.
--   3. Uang muka awal DITURUNKAN dari bukunya, bukan disimpan sebagai kolom kedua.
--   4. Kunci idempotensi buku kas benar-benar mengikat.
--   5. Biaya trip dapat dibalik berpasangan, dan pembalikan biaya TUNAI mengembalikan kas.
--   6. Setoran tercatat di dua tempat (dokumen + buku) tetapi hanya DIHITUNG SEKALI.
--      Blok 7 penjaganya: rumus yang menggandakannya menghasilkan 100.000.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DELETE FROM kas12.sales_trip_kas;
DELETE FROM kas12.sales_trip_biaya;
DELETE FROM kas12.sales_trip_setoran;
DELETE FROM kas12.sales_trip_nota;
DELETE FROM kas12.sales_trip;
DELETE FROM kas12.surat_perintah_sales;
DELETE FROM kas12.salesperson;
DELETE FROM kas12.gudang;
DELETE FROM kas12.toko;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

-- Buku kas sesi legacy: satu tabel, bertanda, sembilan jenis.
CREATE TABLE koperasi.nota_sales_kas (id bigserial PRIMARY KEY, sesi bigint,
  jenis varchar(32), nominal numeric(18,2), referensi varchar(64), keterangan text);

-- ---------- legacy ----------
INSERT INTO koperasi.nota_sales_kas (sesi, jenis, nominal, referensi) VALUES
  (1, 'OPENING_ADVANCE',  500000, 'SPJ-001'),
  (1, 'CASH_SALE',        300000, 'NOTA-1'),
  (1, 'COLLECTION_CASH',  200000, 'KWT-1'),
  (1, 'EXPENSE_CASH',    -100000, 'BIAYA-1'),
  (1, 'OWNER_DEPOSIT',   -400000, 'BUKTI-1');

-- ---------- tenant ----------
INSERT INTO kas12.toko (id, nama, kode) VALUES (1, 'Toko Uji', 'T01');
INSERT INTO kas12.gudang (id, kode, nama, toko_id) VALUES (1, 'G01', 'Gudang', 1);
INSERT INTO kas12.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO kas12.surat_perintah_sales (id, nomor_dokumen, tanggal, salesperson_id, gudang_id,
                                        status, uang_muka_operasional)
  VALUES (1, 'SPJ-001', DATE '2026-06-01', 7, 1, 'ACTIVE', 500000);
INSERT INTO kas12.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
                              gudang_id, tanggal_berangkat, status, dibuat_pada, oleh)
  VALUES (1, 'TRIP-001', 1, 7, 1, DATE '2026-06-01', 'ACTIVE', now(), 'uji');

-- Nota dan setoran tetap ada: rumus LAMA membacanya, dan blok 2 memakainya.
INSERT INTO kas12.sales_trip_nota (sales_trip_id, nomor_nota, tanggal, total, tunai, kredit,
                                   dibuat_pada, oleh)
  VALUES (1, 'NOTA-1', DATE '2026-06-02', 300000, 300000, 0, now(), 'uji');
INSERT INTO kas12.sales_trip_setoran (sales_trip_id, tanggal, cara_bayar, nomor_bukti, nilai,
                                      status, dibuat_pada, oleh)
  VALUES (1, DATE '2026-06-03', 'TUNAI', 'BUKTI-1', 400000, 'AKTIF', now(), 'uji');

-- Buku kas tenant: nominal SUDAH bertanda, sebagaimana kontrak TenantKasTrip.
INSERT INTO kas12.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, waktu,
                                  dibuat_pada, oleh) VALUES
  (1, 'OPENING_ADVANCE',  500000, 'SPJ-001',  now(), now(), 'uji'),
  (1, 'CASH_SALE',        300000, 'NOTA-1',   now(), now(), 'uji'),
  (1, 'COLLECTION_CASH',  200000, 'KWT-1',    now(), now(), 'uji'),
  (1, 'EXPENSE_CASH',    -100000, 'BIAYA-1',  now(), now(), 'uji'),
  (1, 'OWNER_DEPOSIT',   -400000, 'BUKTI-1',  now(), now(), 'uji');

\pset format aligned

\echo ''
\echo '== BLOK 1: saldo kas -- buku tenant setara buku legacy ============================='

SELECT leg.saldo AS "saldo legacy", ten.saldo AS "saldo tenant",
       CASE WHEN leg.saldo = ten.saldo AND ten.saldo = 500000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE(SUM(k.nominal),0) AS saldo FROM koperasi.nota_sales_kas k
      WHERE k.sesi = 1) leg,
     (SELECT COALESCE(SUM(k.nominal),0) AS saldo FROM kas12.sales_trip_kas k
      WHERE k.sales_trip_id = 1) ten;

\echo ''
\echo '== BLOK 2 (PENJAGA): inilah sebab v12 ada ========================================='
\echo '   Rumus LAMA (nota tunai - setoran) mengabaikan panjar, penagihan tunai, dan biaya.'
\echo '   Ia harus menghasilkan -100.000: bukan selisih kecil, melainkan BEDA TANDA.'

SELECT
  (COALESCE((SELECT SUM(n.tunai) FROM kas12.sales_trip_nota n WHERE n.sales_trip_id = 1),0)
 - COALESCE((SELECT SUM(t.nilai) FROM kas12.sales_trip_setoran t WHERE t.sales_trip_id = 1),0))
    AS "saldo rumus lama",
  500000 AS "saldo benar",
  CASE WHEN (COALESCE((SELECT SUM(n.tunai) FROM kas12.sales_trip_nota n
                       WHERE n.sales_trip_id = 1),0)
           - COALESCE((SELECT SUM(t.nilai) FROM kas12.sales_trip_setoran t
                       WHERE t.sales_trip_id = 1),0)) = -100000
       THEN 'LULUS (rumus lama memang salah tanda -- v12 diperlukan)'
       ELSE 'GAGAL (contoh tidak membuktikan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 3: uang muka awal DITURUNKAN dari bukunya, bukan kolom kedua =============='
\echo '   Legacy menyimpannya dua kali (kolom saldoKasAwal + baris OPENING_ADVANCE).'
\echo '   Tenant hanya punya bukunya, dan angkanya harus tetap sama dengan sumber SPJ.'

SELECT
  (SELECT COALESCE(SUM(k.nominal),0) FROM kas12.sales_trip_kas k
    WHERE k.sales_trip_id = 1 AND k.jenis = 'OPENING_ADVANCE')  AS "diturunkan dari buku",
  (SELECT uang_muka_operasional FROM kas12.surat_perintah_sales WHERE id = 1)
                                                                AS "sumber pada SPJ",
  CASE WHEN (SELECT COALESCE(SUM(k.nominal),0) FROM kas12.sales_trip_kas k
               WHERE k.sales_trip_id = 1 AND k.jenis = 'OPENING_ADVANCE')
          = (SELECT uang_muka_operasional FROM kas12.surat_perintah_sales WHERE id = 1)
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 4: buku kas idempoten -- kunci kembar ditolak indeks unik v12 ============='

INSERT INTO kas12.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, waktu,
                                  idempotency_key, dibuat_pada, oleh)
  VALUES (1, 'CASH_SALE', 50000, 'NOTA-9', now(), 'KAS-UNIK-1', now(), 'uji');
\set ON_ERROR_STOP off
INSERT INTO kas12.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, waktu,
                                  idempotency_key, dibuat_pada, oleh)
  VALUES (1, 'CASH_SALE', 50000, 'NOTA-9', now(), 'KAS-UNIK-1', now(), 'uji');
\set ON_ERROR_STOP on

SELECT COUNT(*) AS "baris berkunci sama",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS (kembar ditolak)'
            ELSE 'GAGAL (satu penjualan tunai terbukukan dua kali)' END AS hasil
FROM kas12.sales_trip_kas WHERE idempotency_key = 'KAS-UNIK-1';

-- bersihkan supaya blok berikutnya tidak terpengaruh
DELETE FROM kas12.sales_trip_kas WHERE idempotency_key = 'KAS-UNIK-1';

\echo ''
\echo '== BLOK 5: biaya trip kini dapat dibalik BERPASANGAN =============================='
\echo '   Sebelum v12 tabel biaya tidak punya pembalik_dari_id: dua baris bernilai lawan'
\echo '   yang tidak dapat dipasangkan kembali -- totalnya benar, asal-usulnya hilang.'

INSERT INTO kas12.sales_trip_biaya (id, sales_trip_id, kategori, keterangan, nilai, tanggal,
                                    cara_bayar, status, dibuat_pada, oleh)
  VALUES (1, 1, 'BBM', 'isi bensin', 100000, DATE '2026-06-02', 'TUNAI', 'AKTIF', now(), 'uji');
INSERT INTO kas12.sales_trip_biaya (id, sales_trip_id, kategori, keterangan, nilai, tanggal,
                                    cara_bayar, status, pembalik_dari_id, dibuat_pada, oleh)
  VALUES (2, 1, 'BBM', 'REVERSAL biaya #1: salah trip', -100000, DATE '2026-06-04', 'TUNAI',
          'REVERSAL', 1, now(), 'uji');
UPDATE kas12.sales_trip_biaya SET status = 'DIBATALKAN' WHERE id = 1;

SELECT
  (SELECT COALESCE(SUM(nilai),0) FROM kas12.sales_trip_biaya WHERE sales_trip_id = 1)
    AS "total biaya bersih",
  (SELECT pembalik_dari_id FROM kas12.sales_trip_biaya WHERE id = 2) AS "pembalik menunjuk",
  (SELECT status FROM kas12.sales_trip_biaya WHERE id = 1)           AS "status asal",
  CASE WHEN (SELECT COALESCE(SUM(nilai),0) FROM kas12.sales_trip_biaya
             WHERE sales_trip_id = 1) = 0
        AND (SELECT pembalik_dari_id FROM kas12.sales_trip_biaya WHERE id = 2) = 1
        AND (SELECT status FROM kas12.sales_trip_biaya WHERE id = 1) = 'DIBATALKAN'
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

-- =====================================================================================
-- BLOK 6: setoran punya DOKUMEN sendiri, tetapi hanya BUKUNYA yang dihitung
-- =====================================================================================
-- Sejak aksi tripDeposit dipindahkan, setoran menulis DUA baris: dokumen
-- sales_trip_setoran (nomor buktinya) dan baris buku kas OWNER_DEPOSIT. Kalau saldo
-- kas ikut membaca dokumennya, setorannya akan terhitung DUA KALI.

\echo ''
\echo '== BLOK 6: setoran tercatat dua tempat, tetapi hanya dihitung SEKALI =============='

SELECT
  (SELECT COUNT(*) FROM kas12.sales_trip_setoran WHERE sales_trip_id = 1) AS "dokumen setoran",
  (SELECT COUNT(*) FROM kas12.sales_trip_kas
    WHERE sales_trip_id = 1 AND jenis = 'OWNER_DEPOSIT')                  AS "baris buku kas",
  (SELECT COALESCE(SUM(nominal),0) FROM kas12.sales_trip_kas WHERE sales_trip_id = 1)
                                                                          AS "saldo kas",
  CASE WHEN (SELECT COUNT(*) FROM kas12.sales_trip_setoran WHERE sales_trip_id = 1) = 1
        AND (SELECT COUNT(*) FROM kas12.sales_trip_kas
              WHERE sales_trip_id = 1 AND jenis = 'OWNER_DEPOSIT') = 1
        AND (SELECT COALESCE(SUM(nominal),0) FROM kas12.sales_trip_kas
              WHERE sales_trip_id = 1) = 500000
       THEN 'LULUS (tidak tergandakan)'
       ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 7 (PENJAGA): kalau dokumen setoran IKUT dikurangkan, saldonya salah ======='
\echo '   Rumus yang keliru akan menghasilkan 100.000, bukan 500.000.'

SELECT
  ((SELECT COALESCE(SUM(nominal),0) FROM kas12.sales_trip_kas WHERE sales_trip_id = 1)
   - (SELECT COALESCE(SUM(nilai),0) FROM kas12.sales_trip_setoran WHERE sales_trip_id = 1))
     AS "saldo bila digandakan",
  CASE WHEN ((SELECT COALESCE(SUM(nominal),0) FROM kas12.sales_trip_kas
              WHERE sales_trip_id = 1)
           - (SELECT COALESCE(SUM(nilai),0) FROM kas12.sales_trip_setoran
              WHERE sales_trip_id = 1)) = 100000
       THEN 'LULUS (penggandaan memang terdeteksi contoh ini)'
       ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

-- =====================================================================================
-- BLOK 8: pembalikan biaya tunai MENGEMBALIKAN kas
-- =====================================================================================
INSERT INTO kas12.sales_trip_kas (sales_trip_id, jenis, nominal, referensi, keterangan,
                                  idempotency_key, waktu, dibuat_pada, oleh)
  VALUES (1, 'REVERSAL', 100000, 'REV-BIAYA-1', 'Reversal biaya tunai: salah trip',
          'REV-BIAYA-1', now(), now(), 'uji');

\echo ''
\echo '== BLOK 8: biaya tunai dibalik -- kas naik kembali sebesar biayanya =============='
\echo '   Sebelum pembalikan 500.000; biayanya 100.000; sesudah dibalik harus 600.000.'

SELECT COALESCE(SUM(nominal),0) AS "saldo sesudah pembalikan",
       CASE WHEN COALESCE(SUM(nominal),0) = 600000 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM kas12.sales_trip_kas WHERE sales_trip_id = 1;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
