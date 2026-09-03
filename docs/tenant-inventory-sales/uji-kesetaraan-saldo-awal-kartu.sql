-- =====================================================================================
-- Uji kesetaraan §21: saldo awal kartu stok -- kebocoran lintas-schema yang diam
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19 DAN schema koperasi tiruan:
--   psql -h 127.0.0.1 -p 55524 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-saldo-awal-kartu.sql
--
-- Berkas ini mengandaikan schema tenant lf19 dan schema bersama koperasi.
--
-- LATAR
--   inventoryLedger bercabang tenant/legacy saat MEMBANGUN kartunya, lalu menutup cabang itu.
--   Perhitungan SALDO AWAL berada SESUDAH penutup cabang, sehingga kedua jalur melewatinya --
--   dan isinya ekspresi legacy atas koperasi.*.
--
--   Akibatnya pada tenant: kartunya dibangun dari mutasi_stok tenant, tetapi saldo awalnya
--   dihitung dari tabel instalasi BERSAMA, disaring dengan id produk MILIK TENANT. Id itu
--   bertabrakan antar-schema, jadi hasilnya bukan galat melainkan angka milik data lain.
--   Dan karena tiap baris kartu ditampilkan sebagai saldoAwal + saldo berjalan, SELURUH kolom
--   saldo ikut salah tanpa satu pun tanda.
--
--   sqlSaldoAwal() sudah ada di SalesInventoryStokTenant sejak awal, tetapi tidak pernah
--   dipanggil. Yang hilang bukan kuerinya, melainkan sambungannya.
--
-- TIGA HAL YANG DIUJI
--   1. Id produk memang bertabrakan antar-schema -- itu sebabnya kekeliruannya diam.
--   2. Ekspresi lama memberi angka instalasi bersama; yang baru memberi angka tenant.
--   3. Blok 3 penjaganya: keduanya HARUS berbeda. Kalau sama, contohnya tidak membuktikan apa
--      pun tentang kebocorannya.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

-- ---------------------------------------------------------------------------------------
-- Berkas ini MENYIAPKAN SENDIRI schema bersama tiruannya berikut isi tenantnya. Sempat
-- tidak begitu: keduanya disemai tangan saat batch §21 ditulis, dan pelari kumpulan uji
-- membuktikan berkasnya lalu berhenti pada "relasi koperasi.pengadaan_produk tidak ada".
-- Uji yang mengandaikan lingkungan yang tidak ia buat sendiri tidak menjaga apa pun.
--
-- Tabel koperasi di bawah TIRUAN, dan sengaja hanya berkolom seperlunya: yang diuji adalah
-- bahwa ekspresi legacy MENUNJUK ke sana, bukan bentuk penuh tabel legacynya.
-- ---------------------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS koperasi;
CREATE TABLE IF NOT EXISTS koperasi.pengadaan_produk (
    id bigserial PRIMARY KEY, produk bigint, qty numeric, waktupengadaan date,
    nomorfaktur text);
CREATE TABLE IF NOT EXISTS koperasi.stok_opname (
    id bigserial PRIMARY KEY, produk bigint, selisih numeric, waktuopname date);
CREATE TABLE IF NOT EXISTS koperasi.pembelian (
    id bigserial PRIMARY KEY, produk bigint, qty numeric, waktu date);

TRUNCATE lf19.mutasi_stok, lf19.produk, lf19.gudang, lf19.toko RESTART IDENTITY CASCADE;
INSERT INTO lf19.toko (id, nama) VALUES (1, 'Toko');
INSERT INTO lf19.gudang (id, kode, nama, toko_id) VALUES (1, 'G1', 'Gudang', 1);
INSERT INTO lf19.produk (id, kode, nama, status, aktif, dibuat_pada, oleh)
     VALUES (55, 'P55', 'Produk Tenant', 'AKTIF', true, now(), 'uji');

TRUNCATE koperasi.pengadaan_produk, koperasi.stok_opname, koperasi.pembelian RESTART IDENTITY;
TRUNCATE lf19.mutasi_stok RESTART IDENTITY CASCADE;

-- Instalasi BERSAMA: produk 55 pernah menerima 900 unit.
INSERT INTO koperasi.pengadaan_produk (produk, qty, waktupengadaan)
     VALUES (55, 900, DATE '2026-01-05');
-- Tenant: produk ber-id SAMA, tetapi saldo awalnya 40.
INSERT INTO lf19.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas)
     VALUES (55, 1, DATE '2026-01-10', 'PENGADAAN', 1, 40);

\echo ''
\echo '== BLOK 1: id produk memang bertabrakan antar-schema =============================='
\echo '   Inilah yang membuat kekeliruannya diam: kuerinya tetap mengembalikan baris.'

SELECT (SELECT COUNT(*) FROM koperasi.pengadaan_produk WHERE produk = 55) AS "di koperasi",
       (SELECT COUNT(*) FROM lf19.produk WHERE id = 55)                   AS "di tenant",
       CASE WHEN (SELECT COUNT(*) FROM koperasi.pengadaan_produk WHERE produk = 55) > 0
             AND (SELECT COUNT(*) FROM lf19.produk WHERE id = 55) > 0
            THEN 'LULUS (id yang sama hidup di dua schema)' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 2: saldo awal menurut kedua ekspresi ======================================'
\echo '   LAMA = ekspresiMasukPid/OpnamePid/KeluarPid atas koperasi.*'
\echo '   BARU = SalesInventoryStokTenant.sqlSaldoAwal atas mutasi_stok tenant'

SELECT (COALESCE((SELECT SUM(x.qty) FROM koperasi.pengadaan_produk x
                   WHERE x.produk = 55 AND x.waktupengadaan < DATE '2026-02-01'),0)
      + COALESCE((SELECT SUM(x.selisih) FROM koperasi.stok_opname x
                   WHERE x.produk = 55 AND x.waktuopname < DATE '2026-02-01'),0)
      - COALESCE((SELECT SUM(x.qty) FROM koperasi.pembelian x
                   WHERE x.produk = 55 AND x.waktu < DATE '2026-02-01'),0)) AS "saldo awal LAMA",
       (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM lf19.mutasi_stok m
         WHERE m.produk_id = 55 AND m.tanggal < DATE '2026-02-01')          AS "saldo awal BARU",
       CASE WHEN (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM lf19.mutasi_stok m
                   WHERE m.produk_id = 55 AND m.tanggal < DATE '2026-02-01') = 40
            THEN 'LULUS (yang baru memberi angka tenant)' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 3 (PENJAGA): keduanya HARUS berbeda ======================================='
\echo '   Kalau angkanya kebetulan sama, contoh ini tidak membuktikan apa pun tentang'
\echo '   kebocorannya. Selisihnya di bawah harus bukan nol.'

SELECT ((COALESCE((SELECT SUM(x.qty) FROM koperasi.pengadaan_produk x
                    WHERE x.produk = 55 AND x.waktupengadaan < DATE '2026-02-01'),0))
        - (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM lf19.mutasi_stok m
            WHERE m.produk_id = 55 AND m.tanggal < DATE '2026-02-01')) AS "selisih",
       CASE WHEN ((COALESCE((SELECT SUM(x.qty) FROM koperasi.pengadaan_produk x
                              WHERE x.produk = 55 AND x.waktupengadaan < DATE '2026-02-01'),0))
                  - (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM lf19.mutasi_stok m
                      WHERE m.produk_id = 55 AND m.tanggal < DATE '2026-02-01')) <> 0
            THEN 'LULUS (kebocorannya memang mengubah angka)'
            ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil;

\echo ''
\echo '== BLOK 4: akibatnya menjalar ke SELURUH baris kartu =============================='
\echo '   Tiap baris ditampilkan sebagai saldoAwal + saldo berjalan, jadi satu saldo awal'
\echo '   yang salah menggeser seluruh kolom saldo -- bukan satu angka saja.'

INSERT INTO lf19.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas)
     VALUES (55, 1, DATE '2026-02-05', 'PENJUALAN', -1, 10),
            (55, 1, DATE '2026-02-06', 'PENGADAAN',  1, 25);

WITH kartu AS (
  SELECT m.tanggal,
         SUM(m.arah * m.kuantitas) OVER (ORDER BY m.tanggal, m.id) AS berjalan
    FROM lf19.mutasi_stok m
   WHERE m.produk_id = 55 AND m.tanggal BETWEEN DATE '2026-02-01' AND DATE '2026-02-28')
SELECT k.tanggal,
       (40 + k.berjalan)  AS "saldo dgn awal BENAR",
       (900 + k.berjalan) AS "saldo dgn awal BOCOR",
       CASE WHEN (40 + k.berjalan) <> (900 + k.berjalan) THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM kartu k ORDER BY k.tanggal;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
