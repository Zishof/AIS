-- =====================================================================================
-- Uji kesetaraan: setoran kas trip dan transisi status (RETURNED, RECONCILING)
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v11:
--   psql -h 127.0.0.1 -p 55504 -U uji -d postgres -f uji-kesetaraan-trip-setoran-status.sql
--
-- Berkas ini mengandaikan schema tenant bernama setor11.
--
-- EMPAT HAL YANG DIUJI
--   1. Setoran menurunkan saldo kas dengan ARAH yang sama seperti legacy. Legacy mencatat
--      OWNER_DEPOSIT sebagai baris buku kas NEGATIF; tenant sebagai dokumen setoran yang
--      DIKURANGKAN. Blok 2 penjaganya: tanpa pengurangan itu angkanya berbeda.
--   2. Penjaga masuk RECONCILING menangkap produk yang BELUM PUNYA baris hasil sama sekali.
--      Blok 4 penjaganya: INNER JOIN akan melewatkannya, LEFT JOIN tidak.
--   3. qty_hilang legacy dipetakan ke selisih tenant -- alokasi baru habis bila selisih ikut
--      dihitung. Blok 5 membuktikannya.
--   4. tanggal_kembali tidak tertimpa pada transisi berikutnya.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DELETE FROM setor11.sales_trip_setoran;
DELETE FROM setor11.sales_trip_hasil;
DELETE FROM setor11.sales_trip_nota;
DELETE FROM setor11.sales_trip_barang;
DELETE FROM setor11.sales_trip;
DELETE FROM setor11.surat_perintah_sales;
DELETE FROM setor11.produk;
DELETE FROM setor11.salesperson;
DELETE FROM setor11.gudang;
DELETE FROM setor11.toko;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

-- Buku kas sesi legacy: satu tabel, bertanda, sembilan jenis baris.
CREATE TABLE koperasi.nota_sales_kas (id bigserial PRIMARY KEY, sesi bigint,
  jenis varchar(32), nominal numeric(18,2), referensi varchar(64), keterangan text);
CREATE TABLE koperasi.spj_sales_barang (id bigserial PRIMARY KEY, spj bigint,
  nama_produk varchar(255), qty_dimuat numeric(18,4), qty_terjual numeric(18,4),
  qty_kembali numeric(18,4), qty_rusak numeric(18,4), qty_hilang numeric(18,4));

-- =====================================================================================
-- DATA BERSAMA
--   Trip membawa Gula 10 dan Kopi 5. Nota tunai 1.000.000. Setoran ke pemilik 400.000.
--   Kopi berakhir: 2 terjual, 3 HILANG (legacy) / selisih (tenant).
-- =====================================================================================

-- ---------- legacy ----------
INSERT INTO koperasi.nota_sales_kas (sesi, jenis, nominal, referensi) VALUES
  (1, 'CASH_SALE',     1000000, 'NOTA-1'),
  (1, 'OWNER_DEPOSIT', -400000, 'BUKTI-1');
INSERT INTO koperasi.spj_sales_barang
  (spj, nama_produk, qty_dimuat, qty_terjual, qty_kembali, qty_rusak, qty_hilang) VALUES
  (1, 'Gula', 10, 6, 3, 1, 0),
  (1, 'Kopi', 5, 2, 0, 0, 3);

-- ---------- tenant ----------
INSERT INTO setor11.toko (id, nama, kode) VALUES (1, 'Toko Uji', 'T01');
INSERT INTO setor11.gudang (id, kode, nama, toko_id) VALUES (1, 'G01', 'Gudang', 1);
INSERT INTO setor11.salesperson (id, kode, nama) VALUES (7, 'S07', 'Budi');
INSERT INTO setor11.produk (id, kode, nama) VALUES (100, 'P100', 'Gula'), (101, 'P101', 'Kopi');
INSERT INTO setor11.surat_perintah_sales (id, nomor_dokumen, tanggal, salesperson_id, gudang_id,
                                        status)
  VALUES (1, 'SPJ-001', DATE '2026-05-01', 7, 1, 'ACTIVE');
INSERT INTO setor11.sales_trip (id, nomor_dokumen, surat_perintah_sales_id, salesperson_id,
                              gudang_id, tanggal_berangkat, status, dibuat_pada, oleh)
  VALUES (1, 'TRIP-001', 1, 7, 1, DATE '2026-05-01', 'ACTIVE', now(), 'uji');
INSERT INTO setor11.sales_trip_barang (sales_trip_id, produk_id, kuantitas_bawa, dibuat_pada, oleh)
  VALUES (1, 100, 10, now(), 'uji'), (1, 101, 5, now(), 'uji');
INSERT INTO setor11.sales_trip_nota (sales_trip_id, nomor_nota, tanggal, total, tunai, kredit,
                                   dibuat_pada, oleh)
  VALUES (1, 'NOTA-1', DATE '2026-05-02', 1000000, 1000000, 0, now(), 'uji');
-- setoran: persis yang ditulis sisipSetoran()
INSERT INTO setor11.sales_trip_setoran (sales_trip_id, tanggal, cara_bayar, nomor_bukti, nilai,
                                      status, dibuat_pada, oleh)
  VALUES (1, DATE '2026-05-02', 'TUNAI', 'BUKTI-1', 400000, 'AKTIF', now(), 'uji');

\pset format aligned

-- CATATAN: schema berkas ini SENGAJA tidak dibagi dengan berkas uji lain. Empat berkas
-- sempat sama-sama memakai rev11 tanpa satu pun membersihkannya, sehingga datanya
-- saling menimpa dan berkas yang berjalan terakhir membaca sisa berkas lain. Schema
-- sendiri menghapus kopling urutan itu sepenuhnya.

\echo ''
\echo '== BLOK 1: saldo kas sesudah setoran -- setara legacy =============================='

SELECT leg.saldo AS "saldo legacy", ten.saldo AS "saldo tenant",
       CASE WHEN leg.saldo = ten.saldo AND ten.saldo = 600000
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM (SELECT COALESCE(SUM(k.nominal),0) AS saldo FROM koperasi.nota_sales_kas k
      WHERE k.sesi = 1) leg,
     (SELECT COALESCE((SELECT SUM(n.tunai) FROM setor11.sales_trip_nota n
                       WHERE n.sales_trip_id = 1), 0)
           - COALESCE((SELECT SUM(t.nilai) FROM setor11.sales_trip_setoran t
                       WHERE t.sales_trip_id = 1), 0) AS saldo) ten;

\echo ''
\echo '== BLOK 2 (PENJAGA): setorannya memang harus DIKURANGKAN =========================='
\echo '   Tanpa pengurangan itu saldonya 1.000.000, bukan 600.000 -- contohnya membedakan.'

SELECT COALESCE((SELECT SUM(n.tunai) FROM setor11.sales_trip_nota n WHERE n.sales_trip_id = 1),0)
         AS "tanpa dikurangi setoran",
       CASE WHEN COALESCE((SELECT SUM(n.tunai) FROM setor11.sales_trip_nota n
                           WHERE n.sales_trip_id = 1),0) = 1000000
            THEN 'LULUS (contoh benar-benar membedakan)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 3: sebelum hasil diisi, KEDUA produk tertangkap belum teralokasi =========='

SELECT COUNT(*) AS "produk belum habis",
       CASE WHEN COUNT(*) = 2 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM setor11.sales_trip_barang b
LEFT JOIN setor11.sales_trip_hasil h
       ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id
WHERE b.sales_trip_id = 1
  AND ABS(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)
          - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)
          - COALESCE(h.selisih,0)) > 0.001;

\echo ''
\echo '== BLOK 4 (PENJAGA): INNER JOIN akan MELEWATKAN keduanya =========================='
\echo '   Produk yang belum punya baris hasil sama sekali harus tetap tertangkap.'

SELECT COUNT(*) AS "tertangkap dgn INNER JOIN",
       CASE WHEN COUNT(*) = 0 THEN 'LULUS (LEFT JOIN memang perlu)'
            ELSE 'GAGAL (penjaga tidak membuktikan apa-apa)' END AS hasil
FROM setor11.sales_trip_barang b
JOIN setor11.sales_trip_hasil h
       ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id
WHERE b.sales_trip_id = 1
  AND ABS(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)
          - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)
          - COALESCE(h.selisih,0)) > 0.001;

-- Hasil dilengkapi: Kopi 2 terjual + 3 selisih (padanan qty_hilang legacy).
INSERT INTO setor11.sales_trip_hasil (sales_trip_id, produk_id, kuantitas_terjual,
    kuantitas_kembali, kuantitas_rusak, selisih, dibuat_pada, oleh)
  VALUES (1, 100, 6, 3, 1, 0, now(), 'uji'),
         (1, 101, 2, 0, 0, 3, now(), 'uji');

\echo ''
\echo '== BLOK 5: alokasi habis di KEDUA jalur -- qty_hilang legacy = selisih tenant ====='

SELECT
  (SELECT COUNT(*) FROM koperasi.spj_sales_barang b WHERE b.spj = 1
     AND ABS(b.qty_dimuat - b.qty_terjual - b.qty_kembali - b.qty_rusak - b.qty_hilang) > 0.001)
    AS "sisa legacy",
  (SELECT COUNT(*) FROM setor11.sales_trip_barang b
     LEFT JOIN setor11.sales_trip_hasil h
            ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id
    WHERE b.sales_trip_id = 1
      AND ABS(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)
              - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)
              - COALESCE(h.selisih,0)) > 0.001)
    AS "sisa tenant",
  CASE WHEN (SELECT COUNT(*) FROM koperasi.spj_sales_barang b WHERE b.spj = 1
               AND ABS(b.qty_dimuat - b.qty_terjual - b.qty_kembali - b.qty_rusak
                       - b.qty_hilang) > 0.001) = 0
        AND (SELECT COUNT(*) FROM setor11.sales_trip_barang b
               LEFT JOIN setor11.sales_trip_hasil h
                      ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id
              WHERE b.sales_trip_id = 1
                AND ABS(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)
                        - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)
                        - COALESCE(h.selisih,0)) > 0.001) = 0
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

-- =====================================================================================
-- BLOK 6: transisi status -- tanggal kembali diisi sekali, lalu TIDAK tertimpa
-- =====================================================================================
UPDATE setor11.sales_trip SET status = 'RETURNED',
       tanggal_kembali = COALESCE(DATE '2026-05-03', tanggal_kembali),
       oleh = 'uji', tanggal_dirubah = now() WHERE id = 1;
UPDATE setor11.surat_perintah_sales SET status = 'RETURNED', tanggal_dirubah = now(),
       oleh = 'uji' WHERE id = 1;
UPDATE setor11.sales_trip SET status = 'RECONCILING',
       tanggal_kembali = COALESCE(NULL, tanggal_kembali),
       oleh = 'uji', tanggal_dirubah = now() WHERE id = 1;
UPDATE setor11.surat_perintah_sales SET status = 'RECONCILING', tanggal_dirubah = now(),
       oleh = 'uji' WHERE id = 1;

\echo ''
\echo '== BLOK 6: status maju, tanggal kembali BERTAHAN, SPJ ikut ========================'

SELECT t.status AS "status trip", t.tanggal_kembali AS "tanggal kembali",
       j.status AS "status SPJ",
       CASE WHEN t.status = 'RECONCILING' AND t.tanggal_kembali = DATE '2026-05-03'
             AND j.status = 'RECONCILING'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM setor11.sales_trip t JOIN setor11.surat_perintah_sales j
  ON t.surat_perintah_sales_id = j.id
WHERE t.id = 1;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
