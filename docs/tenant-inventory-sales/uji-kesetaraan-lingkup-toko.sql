-- =====================================================================================
-- Uji kesetaraan §16: lingkup toko pada model tenant adalah lingkup GUDANG
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v18:
--   psql -h 127.0.0.1 -p 55514 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-lingkup-toko.sql
--
-- Berkas ini mengandaikan schema tenant bernama scope18.
--
-- SKENARIO
--   Dua toko, masing-masing satu gudang. Gula distok di KEDUA toko (30 di T1, 500 di T2).
--   Kopi hanya di T2 (70). Teh tidak pernah distok di mana pun.
--
-- EMPAT HAL YANG DIUJI
--   1. Lingkup toko membatasi DAFTAR barisnya: T1 hanya melihat Gula.
--   2. Lingkup toko membatasi ANGKAnya: Gula di T1 bernilai 30, bukan 530.
--   3. Blok 3 penjaganya, dan inilah inti rancangannya: membatasi daftar TANPA membatasi
--      angka membuat produk toko ini tampil dengan stok se-tenant -- lebih besar dari
--      kenyataan di raknya.
--   4. Produk bersaldo NOL tetap muncul. Itu sebabnya daftarnya dibatasi lewat saldo_stok,
--      bukan lewat mutasi_stok: memakai mutasi akan menyembunyikan produk yang habis,
--      padahal justru itu yang ingin dilihat.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

\echo ''
\echo '== BLOK 1: daftar dibatasi gudang toko ============================================'
\echo '   T1 hanya menangani Gula; Kopi milik T2 dan Teh tak pernah distok.'

SELECT COUNT(*) AS "produk terlihat T1",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM scope18.produk p
WHERE COALESCE(p.aktif,true) = true
  AND EXISTS (SELECT 1 FROM scope18.saldo_stok ss
                JOIN scope18.gudang g ON ss.gudang_id = g.id
               WHERE ss.produk_id = p.id AND g.toko_id = 1);

\echo ''
\echo '== BLOK 2: angkanya juga dibatasi gudang toko ====================================='
\echo '   Gula: 30 di rak T1, 500 di rak T2, 530 se-tenant.'

SELECT
  COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m
             WHERE m.produk_id = 100),0) AS "se-tenant",
  COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m
             WHERE m.produk_id = 100
               AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g WHERE g.toko_id = 1)),0)
    AS "rak T1",
  CASE WHEN COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m
                       WHERE m.produk_id = 100),0) = 530
        AND COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m
                       WHERE m.produk_id = 100
                         AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g
                                              WHERE g.toko_id = 1)),0) = 30
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 3 (PENJAGA): membatasi DAFTAR saja tidak cukup ============================'
\echo '   Kalau angkanya tidak ikut dibatasi, Gula tampil 530 pada layar toko yang punya 30.'
\echo '   Angka di bawah HARUS 530 -- kalau ia sudah 30, penjaga ini tidak membuktikan apa pun.'

SELECT
  COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m WHERE m.produk_id = p.id),0)
    AS "daftar dibatasi, angka tidak",
  CASE WHEN COALESCE((SELECT SUM(m.kuantitas) FROM scope18.mutasi_stok m
                       WHERE m.produk_id = p.id),0) = 530
       THEN 'LULUS (kedua pembatas memang diperlukan)'
       ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
FROM scope18.produk p
WHERE EXISTS (SELECT 1 FROM scope18.saldo_stok ss
                JOIN scope18.gudang g ON ss.gudang_id = g.id
               WHERE ss.produk_id = p.id AND g.toko_id = 1);

\echo ''
\echo '== BLOK 4: produk bersaldo NOL tetap terlihat ====================================='
\echo '   Gula T1 dikosongkan; ia harus TETAP muncul, sebab baris saldo_stok-nya masih ada.'

INSERT INTO scope18.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas)
  VALUES (100, 11, DATE '2026-12-20', 'PENJUALAN', -1, 30);

SELECT
  (SELECT COUNT(*) FROM scope18.produk p
    WHERE EXISTS (SELECT 1 FROM scope18.saldo_stok ss
                    JOIN scope18.gudang g ON ss.gudang_id = g.id
                   WHERE ss.produk_id = p.id AND g.toko_id = 1)) AS "terlihat lewat saldo_stok",
  COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM scope18.mutasi_stok m
             WHERE m.produk_id = 100
               AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g WHERE g.toko_id = 1)),0)
    AS "stok T1 sekarang",
  CASE WHEN (SELECT COUNT(*) FROM scope18.produk p
               WHERE EXISTS (SELECT 1 FROM scope18.saldo_stok ss
                               JOIN scope18.gudang g ON ss.gudang_id = g.id
                              WHERE ss.produk_id = p.id AND g.toko_id = 1)) = 1
        AND COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM scope18.mutasi_stok m
                       WHERE m.produk_id = 100
                         AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g
                                              WHERE g.toko_id = 1)),0) = 0
       THEN 'LULUS (habis, tetapi tetap terlihat)' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 5 (PENJAGA): kalau daftarnya dibatasi lewat MUTASI, produk habis pun hilang'
\echo '   Bukan hilang di sini -- mutasinya masih ada. Yang diuji: jumlah baris berbeda bila'
\echo '   dasarnya "punya mutasi bersaldo > 0", yaitu NOL. Itulah yang dihindari.'

SELECT
  (SELECT COUNT(*) FROM scope18.produk p
    WHERE COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM scope18.mutasi_stok m
                     WHERE m.produk_id = p.id
                       AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g
                                            WHERE g.toko_id = 1)),0) > 0) AS "bila dasarnya saldo>0",
  CASE WHEN (SELECT COUNT(*) FROM scope18.produk p
               WHERE COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM scope18.mutasi_stok m
                                WHERE m.produk_id = p.id
                                  AND m.gudang_id IN (SELECT g.id FROM scope18.gudang g
                                                       WHERE g.toko_id = 1)),0) > 0) = 0
       THEN 'LULUS (dasar yang keliru memang menyembunyikannya)'
       ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
