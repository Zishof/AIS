-- =====================================================================================
-- Uji kesetaraan §24: riwayat BELI.DBF/JUAL.DBF menjadi MUTASI, bukan dokumen karangan
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v19:
--   psql -h 127.0.0.1 -p 55528 -U uji -d postgres -v ON_ERROR_STOP=1 \
--        -f uji-kesetaraan-impor-riwayat.sql
--
-- Berkas ini mengandaikan schema tenant bernama r24.
--
-- LATAR
--   Godaan yang wajar: menerbitkan pembelian ber-kepala/detail dan faktur_penjualan supaya
--   riwayat lama tampak seperti dokumen tenant biasa. ITU AKAN MENGARANG DATA. Baris DBF-nya
--   tidak membawa supplier maupun customer sebagai relasi -- hanya teks kode -- dan tidak
--   membawa termin, jatuh tempo, pajak, maupun status dokumen. Dokumen yang dibentuk dari situ
--   akan punya kepala berisi tebakan, lalu ikut masuk ke umur hutang dan piutang seolah-olah
--   tagihan sungguhan.
--
--   Yang benar-benar dibawa berkas itu hanya: barang berpindah, sekian banyak, pada tanggal
--   sekian, seharga sekian. Pada model tenant itu persis satu baris mutasi_stok.
--
-- EMPAT HAL YANG DIUJI
--   1. Pembelian masuk (arah +1) dan penjualan keluar (arah -1); saldonya selisih keduanya.
--   2. Nilai baris = kuantitas x harga satuan.
--   3. Blok 3 penjaganya: mengimpor ulang berkas yang SAMA tidak menggandakan pergerakan.
--      Penjaganya indeks unik idempotency_key (bundel v11), bukan pembacaan lebih dulu --
--      blok ini membuktikan indeksnya memang menolak, bukan sekadar kodenya yang berhati-hati.
--   4. TIDAK ADA dokumen yang lahir: pembelian, faktur_penjualan, hutang, piutang tetap kosong.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\pset format aligned

TRUNCATE r24.mutasi_stok, r24.produk, r24.gudang, r24.toko RESTART IDENTITY CASCADE;
INSERT INTO r24.toko (id, nama) VALUES (1, 'Toko');
INSERT INTO r24.gudang (id, kode, nama, toko_id) VALUES (1, 'G1', 'Gudang', 1);
INSERT INTO r24.produk (id, kode, nama, status, aktif, dibuat_pada, oleh)
     VALUES (10, 'P10', 'Produk Uji', 'AKTIF', true, now(), 'uji');

-- Persis pernyataan yang dikeluarkan sisipMutasiRiwayat(): BELI 100 @ 8.000, JUAL 30 @ 12.500.
INSERT INTO r24.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas, harga_satuan,
                             nilai, nomor_dokumen, keterangan, idempotency_key, dibuat_pada, oleh)
     VALUES (10, 1, DATE '2026-01-05', 'PENGADAAN', 1, 100, 8000, 800000, 'FB-001',
             'Migrasi BELI.DBF; supplier=SUP1; batch=B1; ED=', 'LEGACY-BELI-FB-001-P10-B1-20260105',
             now(), 'uji');
INSERT INTO r24.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas, harga_satuan,
                             nilai, nomor_dokumen, keterangan, idempotency_key, dibuat_pada, oleh)
     VALUES (10, 1, DATE '2026-01-20', 'PENJUALAN', -1, 30, 12500, 375000, 'FJ-007',
             'Migrasi JUAL.DBF; customer=C9; sales=S5; batch=B1', 'LEGACY-JUAL-FJ-007-P10-B1-20260120',
             now(), 'uji');

\echo ''
\echo '== BLOK 1: arah masuk/keluar, dan saldonya selisih keduanya ======================='

SELECT m.jenis, m.arah, m.kuantitas, m.nomor_dokumen,
       CASE WHEN (m.jenis = 'PENGADAAN' AND m.arah = 1)
              OR (m.jenis = 'PENJUALAN' AND m.arah = -1)
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM r24.mutasi_stok m WHERE m.produk_id = 10 ORDER BY m.tanggal;

SELECT COALESCE(SUM(m.arah * m.kuantitas),0) AS "saldo produk 10",
       CASE WHEN COALESCE(SUM(m.arah * m.kuantitas),0) = 70
            THEN 'LULUS (100 masuk - 30 keluar)' ELSE 'GAGAL' END AS hasil
  FROM r24.mutasi_stok m WHERE m.produk_id = 10;

\echo ''
\echo '== BLOK 2: nilai baris = kuantitas x harga satuan ================================='

SELECT m.nomor_dokumen, m.kuantitas, m.harga_satuan, m.nilai,
       CASE WHEN m.nilai = m.kuantitas * m.harga_satuan THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM r24.mutasi_stok m WHERE m.produk_id = 10 ORDER BY m.tanggal;

\echo ''
\echo '== BLOK 3 (PENJAGA): indeks unik menolak impor ulang, bukan sekadar kodenya ======='
\echo '   Baris yang sama disisipkan lagi di dalam transaksi yang lalu dibatalkan. Galat'
\echo '   pelanggaran batasan unik di bawah ADALAH hasil yang diharapkan.'
\echo '   Kalau indeksnya tidak menjaga, sisipan kedua akan LOLOS dan stoknya berlipat.'

BEGIN;
\set ON_ERROR_STOP 0
INSERT INTO r24.mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah, kuantitas, harga_satuan,
                             nilai, nomor_dokumen, keterangan, idempotency_key, dibuat_pada, oleh)
     VALUES (10, 1, DATE '2026-01-05', 'PENGADAAN', 1, 100, 8000, 800000, 'FB-001',
             'ulangan', 'LEGACY-BELI-FB-001-P10-B1-20260105', now(), 'uji');
\set ON_ERROR_STOP 1
ROLLBACK;

SELECT COUNT(*) AS "baris berkunci sama",
       (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM r24.mutasi_stok m
         WHERE m.produk_id = 10) AS "saldo tetap",
       CASE WHEN COUNT(*) = 1
             AND (SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM r24.mutasi_stok m
                   WHERE m.produk_id = 10) = 70
            THEN 'LULUS (sisipan kedua ditolak indeks unik)' ELSE 'GAGAL' END AS hasil
  FROM r24.mutasi_stok WHERE idempotency_key = 'LEGACY-BELI-FB-001-P10-B1-20260105';

\echo ''
\echo '== BLOK 4: TIDAK ADA dokumen yang dikarang ========================================'
\echo '   Impor riwayat tidak boleh menerbitkan pembelian/faktur/hutang/piutang: baris DBF'
\echo '   tidak membawa apa pun yang diperlukan kepala dokumen, dan dokumen bertebakan akan'
\echo '   ikut masuk ke umur hutang dan piutang seolah tagihan sungguhan.'

SELECT (SELECT COUNT(*) FROM r24.pembelian)         AS "pembelian",
       (SELECT COUNT(*) FROM r24.faktur_penjualan)  AS "faktur",
       (SELECT COUNT(*) FROM r24.hutang_supplier)   AS "hutang",
       (SELECT COUNT(*) FROM r24.piutang_customer)  AS "piutang",
       CASE WHEN (SELECT COUNT(*) FROM r24.pembelian) = 0
             AND (SELECT COUNT(*) FROM r24.faktur_penjualan) = 0
             AND (SELECT COUNT(*) FROM r24.hutang_supplier) = 0
             AND (SELECT COUNT(*) FROM r24.piutang_customer) = 0
            THEN 'LULUS (tidak ada dokumen karangan)' ELSE 'GAGAL' END AS hasil;

\echo '   Asal-usulnya tetap terbaca pada keterangan, sebab teks tidak bisa jadi relasi'
\echo '   tetapi membuangnya berarti kehilangan satu-satunya petunjuk asalnya.'

SELECT m.nomor_dokumen, m.keterangan,
       CASE WHEN m.keterangan LIKE '%supplier=SUP1%' OR m.keterangan LIKE '%customer=C9%'
            THEN 'LULUS' ELSE 'GAGAL' END AS hasil
  FROM r24.mutasi_stok m WHERE m.produk_id = 10 ORDER BY m.tanggal;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
