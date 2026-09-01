-- =====================================================================
-- 74 - Pemulihan nama pemesan (member) yang tertimpa NULL
--
-- Latar belakang lengkap: 73-stok-minus-tiga-nilai-dan-pemulihan-member.md bagian 2.
--
-- Sampai r78633 (31-08-2026), finalisasi pesanan dari halaman Pesanan mengirim
-- payload tanpa field member. Nilai NULL itu MENIMPA anggota pada header draft dan
-- pada transaksi finalnya, sehingga kolom NAMA PEMESAN berubah menjadi "-" dan
-- transaksinya lenyap dari laporan per tenant/member.
--
-- Kedua entitasnya @Audited (Hibernate Envers, skema new_audit, akhiran __audit),
-- jadi nilai SEBELUM ditimpa masih tersimpan dan dapat dikembalikan.
--
-- =====================================================================
-- CARA PAKAI - JANGAN LOMPAT LANGKAH
-- =====================================================================
--  1. Backup dulu (pg_dump). Skrip ini menulis ke tabel transaksi.
--  2. Jalankan BAGIAN 1 (SELECT). Aman, hanya membaca. Lihat berapa baris
--     yang benar-benar dapat dipulihkan.
--  3. Jalankan BAGIAN 2 (SELECT rinci) untuk memeriksa contoh barisnya - pastikan
--     nama yang akan dikembalikan memang masuk akal.
--  4. Baru jalankan BAGIAN 3 (UPDATE) di dalam transaksi, dan COMMIT hanya bila
--     jumlah baris yang terpengaruh sama dengan angka pada langkah 2.
--
-- Skrip ini HANYA menyentuh baris yang anggota_koperasi-nya SEKARANG NULL
-- sedangkan audit menyimpan nilai yang TIDAK NULL. Pesanan yang memang tidak
-- pernah punya member (pembeli umum) tidak tersentuh sama sekali.
-- =====================================================================


-- =====================================================================
-- BAGIAN 0 - Apakah pesanannya benar-benar hilang, atau hanya di luar filter?
--
-- Layar Pesanan menyaring rentang tanggal (bawaannya beberapa hari terakhir).
-- Sebelum menyimpulkan data hilang, hitung dulu apa adanya per bulan.
-- =====================================================================
SELECT DATE_TRUNC('month', d.tanggal_pembayaran) AS bulan,
       COUNT(*)                                                 AS jumlah_pesanan,
       COUNT(d.anggota_koperasi)                                AS ada_member,
       COUNT(*) - COUNT(d.anggota_koperasi)                     AS member_kosong,
       COUNT(*) FILTER (WHERE d.lunas IS NULL)                  AS belum_dibayar
  FROM koperasi.draft_pembelian_anggota_koperasi d
 WHERE d.tanggal_pembayaran >= DATE '2026-08-01'
 GROUP BY 1
 ORDER BY 1;


-- =====================================================================
-- BAGIAN 1 - Berapa banyak yang dapat dipulihkan
-- =====================================================================

-- 1a. Header draft (sumber kolom NAMA PEMESAN di layar Pesanan)
WITH pulih AS (
    SELECT d.id,
           (SELECT a.anggota_koperasi
              FROM new_audit.draft_pembelian_anggota_koperasi__audit a
             WHERE a.id = d.id
               AND a.anggota_koperasi IS NOT NULL
             ORDER BY a.rev DESC
             LIMIT 1) AS anggota_dari_audit
      FROM koperasi.draft_pembelian_anggota_koperasi d
     WHERE d.anggota_koperasi IS NULL
       AND d.tanggal_pembayaran >= DATE '2026-08-01'
)
SELECT COUNT(*) FILTER (WHERE anggota_dari_audit IS NOT NULL) AS draft_dapat_dipulihkan,
       COUNT(*) FILTER (WHERE anggota_dari_audit IS NULL)     AS draft_memang_tanpa_member
  FROM pulih;

-- 1b. Transaksi final (sumber rekap transaksi harian dan hitungan reward member)
WITH pulih AS (
    SELECT p.id,
           COALESCE(
             (SELECT a.anggota_koperasi
                FROM new_audit.pembelian_anggota_koperasi__audit a
               WHERE a.id = p.id
                 AND a.anggota_koperasi IS NOT NULL
               ORDER BY a.rev DESC
               LIMIT 1),
             -- Cadangan: warisi dari header draft asalnya, yang dipulihkan di 3a.
             (SELECT d.anggota_koperasi
                FROM koperasi.draft_pembelian_anggota_koperasi d
               WHERE d.id = p.draft_pembelian_anggota_koperasi)
           ) AS anggota_dari_audit
      FROM koperasi.pembelian_anggota_koperasi p
     WHERE p.anggota_koperasi IS NULL
       AND p.tanggal_pembayaran >= DATE '2026-08-01'
)
SELECT COUNT(*) FILTER (WHERE anggota_dari_audit IS NOT NULL) AS transaksi_dapat_dipulihkan,
       COUNT(*) FILTER (WHERE anggota_dari_audit IS NULL)     AS transaksi_memang_tanpa_member
  FROM pulih;


-- =====================================================================
-- BAGIAN 2 - Periksa contoh barisnya sebelum menulis apa pun
--
-- Baca daftar ini. Nama yang akan dikembalikan harus masuk akal untuk
-- tanggal dan tokonya. Bila ada yang janggal, BERHENTI dan laporkan.
-- =====================================================================
SELECT d.id,
       d.kode,
       d.tanggal_pembayaran,
       t.nama AS toko,
       ak.nama AS nama_yang_akan_dikembalikan,
       ak.kode_identitas
  FROM koperasi.draft_pembelian_anggota_koperasi d
  LEFT JOIN koperasi.toko t ON t.id = d.toko
  JOIN LATERAL (
        SELECT a.anggota_koperasi
          FROM new_audit.draft_pembelian_anggota_koperasi__audit a
         WHERE a.id = d.id
           AND a.anggota_koperasi IS NOT NULL
         ORDER BY a.rev DESC
         LIMIT 1
  ) src ON TRUE
  JOIN koperasi.anggota_koperasi ak ON ak.id = src.anggota_koperasi
 WHERE d.anggota_koperasi IS NULL
   AND d.tanggal_pembayaran >= DATE '2026-08-01'
 ORDER BY d.tanggal_pembayaran DESC
 LIMIT 50;


-- =====================================================================
-- BAGIAN 3 - Pemulihan
--
-- Dibungkus BEGIN tanpa COMMIT dengan sengaja. Jalankan, baca jumlah baris
-- yang terpengaruh, cocokkan dengan BAGIAN 1, baru ketik COMMIT sendiri.
-- Bila angkanya tidak cocok: ROLLBACK.
-- =====================================================================
BEGIN;

-- 3a. Header draft
UPDATE koperasi.draft_pembelian_anggota_koperasi d
   SET anggota_koperasi = src.anggota_koperasi
  FROM LATERAL (
        SELECT a.anggota_koperasi
          FROM new_audit.draft_pembelian_anggota_koperasi__audit a
         WHERE a.id = d.id
           AND a.anggota_koperasi IS NOT NULL
         ORDER BY a.rev DESC
         LIMIT 1
  ) src
 WHERE d.anggota_koperasi IS NULL
   AND d.tanggal_pembayaran >= DATE '2026-08-01';

-- 3b. Transaksi final - audit dulu, header draft sebagai cadangan.
--     Dijalankan SESUDAH 3a supaya cadangannya sudah terisi.
UPDATE koperasi.pembelian_anggota_koperasi p
   SET anggota_koperasi = src.anggota_koperasi
  FROM LATERAL (
        SELECT COALESCE(
                 (SELECT a.anggota_koperasi
                    FROM new_audit.pembelian_anggota_koperasi__audit a
                   WHERE a.id = p.id
                     AND a.anggota_koperasi IS NOT NULL
                   ORDER BY a.rev DESC
                   LIMIT 1),
                 (SELECT d.anggota_koperasi
                    FROM koperasi.draft_pembelian_anggota_koperasi d
                   WHERE d.id = p.draft_pembelian_anggota_koperasi)
               ) AS anggota_koperasi
  ) src
 WHERE p.anggota_koperasi IS NULL
   AND src.anggota_koperasi IS NOT NULL
   AND p.tanggal_pembayaran >= DATE '2026-08-01';

-- 3c. Baris rincian mewarisi dari headernya sendiri - tidak perlu audit,
--     header adalah sumber kebenarannya.
UPDATE koperasi.pembelian b
   SET anggota_koperasi = p.anggota_koperasi
  FROM koperasi.pembelian_anggota_koperasi p
 WHERE b.pembelian_anggota_koperasi = p.id
   AND b.anggota_koperasi IS NULL
   AND p.anggota_koperasi IS NOT NULL;

-- Periksa hasilnya SEBELUM commit - ketiganya harus 0.
SELECT (SELECT COUNT(*) FROM koperasi.draft_pembelian_anggota_koperasi d
         WHERE d.anggota_koperasi IS NULL
           AND d.tanggal_pembayaran >= DATE '2026-08-01'
           AND EXISTS (SELECT 1 FROM new_audit.draft_pembelian_anggota_koperasi__audit a
                        WHERE a.id = d.id AND a.anggota_koperasi IS NOT NULL)) AS sisa_draft,
       (SELECT COUNT(*) FROM koperasi.pembelian_anggota_koperasi p
         WHERE p.anggota_koperasi IS NULL
           AND p.tanggal_pembayaran >= DATE '2026-08-01'
           AND EXISTS (SELECT 1 FROM new_audit.pembelian_anggota_koperasi__audit a
                        WHERE a.id = p.id AND a.anggota_koperasi IS NOT NULL)) AS sisa_transaksi,
       (SELECT COUNT(*) FROM koperasi.pembelian b
          JOIN koperasi.pembelian_anggota_koperasi p ON p.id = b.pembelian_anggota_koperasi
         WHERE b.anggota_koperasi IS NULL
           AND p.anggota_koperasi IS NOT NULL) AS sisa_rincian;

-- COMMIT;    <-- ketik sendiri bila angka di atas sudah cocok
-- ROLLBACK;  <-- bila tidak
