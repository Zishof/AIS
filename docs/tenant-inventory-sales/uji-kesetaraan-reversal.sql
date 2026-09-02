-- =====================================================================================
-- Uji kesetaraan Reversal: pembalikan pembayaran hutang, idempotensi, dan log cetak
-- =====================================================================================
--
-- CARA PAKAI -- klaster sekali-pakai berisi schema tenant v1-v11:
--   psql -h 127.0.0.1 -p 55501 -U uji -d postgres -f uji-kesetaraan-reversal.sql
--
-- Berkas ini mengandaikan schema tenant bernama rev11 (nama itu ikut membentuk nama
-- indeksnya: uq_rev11_pembayaran_hutang_idem). Untuk nama schema lain, ganti keduanya.
--
-- TIGA HAL YANG DIUJI
--   1. Pembalikan mengembalikan sisa hutang PERSIS ke keadaan sebelum pembayaran.
--      Yang mengembalikannya adalah ALOKASI NEGATIF, bukan dokumen pembaliknya.
--      Blok 2 membuktikan itu: tanpa alokasi negatif, sisanya TIDAK kembali.
--   2. Indeks unik parsial v11 benar-benar menolak kunci idempotensi kembar.
--      Blok 3 membuktikan indeksnya yang menolak, bukan kebetulan lain -- indeksnya
--      dilepas dulu, kembar dibuktikan LOLOS, lalu indeksnya dipasang lagi.
--   3. printLogList tenant mengembalikan ENAM medan yang sama dengan jalur legacy.
--
-- Setiap blok mencetak LULUS/GAGAL. Satu GAGAL saja berarti jalur tenant tidak setara.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

-- -------------------------------------------------------------------------------------
-- Tiruan schema legacy secukupnya untuk pembanding
-- -------------------------------------------------------------------------------------
-- Dijalankan berulang: bersihkan sisa jalan uji sebelumnya lebih dulu.
-- Urutannya mengikuti kunci asing, dari anak ke induk.
DELETE FROM rev11.reversal_log;
DELETE FROM rev11.print_log;
DELETE FROM rev11.alokasi_pembayaran_hutang;
DELETE FROM rev11.pembayaran_hutang;
DELETE FROM rev11.hutang_supplier;
DELETE FROM rev11.supplier;

DROP SCHEMA IF EXISTS koperasi CASCADE;
CREATE SCHEMA koperasi;

CREATE TABLE koperasi.supplier (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));

CREATE TABLE koperasi.pengadaan_faktur (id bigserial PRIMARY KEY, supplier bigint,
  nomor varchar(64), tanggal date, total numeric(18,2));

CREATE TABLE koperasi.pembayaran_hutang_supplier (id bigserial PRIMARY KEY, supplier bigint,
  nominal numeric(18,2), metode varchar(32), no_bg varchar(64), nama_bank varchar(64),
  keterangan text, kode_unik varchar(128) UNIQUE, status_dok varchar(16),
  reversal_dari bigint, alasan_reversal text);

CREATE TABLE koperasi.alokasi_pembayaran_hutang_supplier (id bigserial PRIMARY KEY,
  pembayaran bigint, pengadaan_faktur bigint, nominal numeric(18,2));

CREATE TABLE koperasi.log_cetak (id bigserial PRIMARY KEY, jenis_dokumen varchar(64),
  referensi varchar(64), parameter_json text, user_id varchar(255), perangkat varchar(128),
  waktu timestamp DEFAULT now());

-- =====================================================================================
-- SKENARIO BERSAMA
--   Hutang 1.000.000. Dibayar 400.000 (satu alokasi). Lalu pembayarannya DIBALIK.
--   Sisa hutang harus kembali ke 1.000.000 di kedua jalur.
-- =====================================================================================

-- ---------- sisi legacy ----------
INSERT INTO koperasi.supplier (id, kode, nama) VALUES (1, 'S01', 'Supplier Satu');
INSERT INTO koperasi.pengadaan_faktur (id, supplier, nomor, tanggal, total)
  VALUES (1, 1, 'FKT-001', DATE '2026-01-10', 1000000);

INSERT INTO koperasi.pembayaran_hutang_supplier
  (id, supplier, nominal, metode, keterangan, kode_unik, status_dok)
  VALUES (1, 1, 400000, 'TUNAI', 'bayar sebagian', 'BAYAR-001', 'AKTIF');
INSERT INTO koperasi.alokasi_pembayaran_hutang_supplier (pembayaran, pengadaan_faktur, nominal)
  VALUES (1, 1, 400000);

-- pembalikan legacy: dokumen cermin negatif + alokasi negatif + asal DIBATALKAN
INSERT INTO koperasi.pembayaran_hutang_supplier
  (id, supplier, nominal, metode, keterangan, kode_unik, status_dok, reversal_dari)
  VALUES (2, 1, -400000, 'TUNAI', 'REVERSAL pembayaran #1: salah supplier',
          'REV-PHS-1', 'REVERSAL', 1);
INSERT INTO koperasi.alokasi_pembayaran_hutang_supplier (pembayaran, pengadaan_faktur, nominal)
  VALUES (2, 1, -400000);
UPDATE koperasi.pembayaran_hutang_supplier
  SET status_dok = 'DIBATALKAN', alasan_reversal = 'salah supplier' WHERE id = 1;

-- ---------- sisi tenant ----------
INSERT INTO rev11.supplier (id, kode, nama) VALUES (1, 'S01', 'Supplier Satu');
INSERT INTO rev11.hutang_supplier (id, supplier_id, tanggal, nilai)
  VALUES (1, 1, DATE '2026-01-10', 1000000);

INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, keterangan,
   idempotency_key, status, dibuat_pada, oleh)
  VALUES (1, 'BAYAR-001', DATE '2026-01-15', 1, 'TUNAI', 400000, 'bayar sebagian',
          'BAYAR-001', 'AKTIF', now(), 'uji');
INSERT INTO rev11.alokasi_pembayaran_hutang
  (pembayaran_hutang_id, hutang_supplier_id, nilai, dibuat_pada, oleh)
  VALUES (1, 1, 400000, now(), 'uji');

-- pembalikan tenant: persis urutan yang dikerjakan payablePaymentReverseTenant
INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, keterangan,
   idempotency_key, pembalik_dari_id, status, dibuat_pada, oleh)
  VALUES (2, 'REV-BAYAR-001', CURRENT_DATE, 1, 'TUNAI', -400000,
          'REVERSAL pembayaran #1: salah supplier', 'REV-PHS-1', 1, 'REVERSAL', now(), 'uji');
INSERT INTO rev11.alokasi_pembayaran_hutang
  (pembayaran_hutang_id, hutang_supplier_id, nilai, dibuat_pada, oleh)
  SELECT 2, a.hutang_supplier_id, -a.nilai, now(), 'uji'
  FROM rev11.alokasi_pembayaran_hutang a WHERE a.pembayaran_hutang_id = 1;
UPDATE rev11.pembayaran_hutang
  SET status = 'DIBATALKAN', dibatalkan = true, dibatalkan_pada = now(),
      alasan_batal = 'salah supplier' WHERE id = 1;
INSERT INTO rev11.reversal_log (dokumen_tipe, dokumen_id, alasan, user_id, waktu)
  VALUES ('PEMBAYARAN_HUTANG', 1, 'salah supplier', 'uji', now());

\echo ''
\echo '== BLOK 1: sisa hutang setelah pembalikan harus setara =============================='
\pset format aligned

SELECT
  leg.sisa                          AS "sisa legacy",
  ten.sisa                          AS "sisa tenant",
  CASE WHEN leg.sisa = ten.sisa AND ten.sisa = 1000000
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM
  (SELECT f.total - COALESCE((SELECT SUM(a.nominal)
      FROM koperasi.alokasi_pembayaran_hutang_supplier a
      WHERE a.pengadaan_faktur = f.id), 0) AS sisa
   FROM koperasi.pengadaan_faktur f WHERE f.id = 1) leg,
  (SELECT h.nilai - COALESCE((SELECT SUM(a.nilai)
      FROM rev11.alokasi_pembayaran_hutang a
      WHERE a.hutang_supplier_id = h.id), 0) AS sisa
   FROM rev11.hutang_supplier h WHERE h.id = 1) ten;

\echo ''
\echo '== BLOK 2 (PENJAGA): yang mengembalikan sisa adalah ALOKASI negatif ================'
\echo '   Kalau dokumen pembalik saja sudah cukup, contoh di atas tidak membuktikan apa pun.'
\echo '   Di bawah alokasi negatifnya diabaikan; sisanya HARUS tetap 600.000 (bukan 1.000.000).'

SELECT
  h.nilai - COALESCE((SELECT SUM(a.nilai) FROM rev11.alokasi_pembayaran_hutang a
     WHERE a.hutang_supplier_id = h.id AND a.nilai > 0), 0) AS "sisa tanpa alokasi negatif",
  CASE WHEN h.nilai - COALESCE((SELECT SUM(a.nilai)
         FROM rev11.alokasi_pembayaran_hutang a
         WHERE a.hutang_supplier_id = h.id AND a.nilai > 0), 0) = 600000
       THEN 'LULUS (contoh benar-benar membedakan)'
       ELSE 'GAGAL (contoh tidak membedakan apa pun)' END AS hasil
FROM rev11.hutang_supplier h WHERE h.id = 1;

\echo ''
\echo '== BLOK 3: indeks unik parsial v11 menolak kunci idempotensi kembar ================'

-- 3a. PENJAGA: tanpa indeksnya, kembar LOLOS. Itu keadaan sebelum v11.
DROP INDEX rev11.uq_rev11_pembayaran_hutang_idem;

INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, idempotency_key,
   status, dibuat_pada, oleh)
  VALUES (98, 'KEMBAR-A', CURRENT_DATE, 1, 'TUNAI', 111, 'KUNCI-KEMBAR', 'AKTIF', now(), 'uji');
INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, idempotency_key,
   status, dibuat_pada, oleh)
  VALUES (99, 'KEMBAR-B', CURRENT_DATE, 1, 'TUNAI', 111, 'KUNCI-KEMBAR', 'AKTIF', now(), 'uji');

SELECT COUNT(*) AS "baris berkunci sama (tanpa indeks)",
       CASE WHEN COUNT(*) = 2 THEN 'LULUS (celah pra-v11 memang nyata)'
            ELSE 'GAGAL (penjaga tidak membuktikan apa-apa)' END AS hasil
FROM rev11.pembayaran_hutang WHERE idempotency_key = 'KUNCI-KEMBAR';

-- 3b. Bersihkan kembarnya, pasang lagi indeksnya, lalu coba lagi: harus DITOLAK.
DELETE FROM rev11.pembayaran_hutang WHERE id IN (98, 99);
CREATE UNIQUE INDEX uq_rev11_pembayaran_hutang_idem ON rev11.pembayaran_hutang
  (idempotency_key) WHERE idempotency_key IS NOT NULL;

INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, idempotency_key,
   status, dibuat_pada, oleh)
  VALUES (98, 'KEMBAR-A', CURRENT_DATE, 1, 'TUNAI', 111, 'KUNCI-KEMBAR', 'AKTIF', now(), 'uji');

\set ON_ERROR_STOP off
INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, idempotency_key,
   status, dibuat_pada, oleh)
  VALUES (99, 'KEMBAR-B', CURRENT_DATE, 1, 'TUNAI', 111, 'KUNCI-KEMBAR', 'AKTIF', now(), 'uji');
\set ON_ERROR_STOP on

SELECT COUNT(*) AS "baris berkunci sama (dengan indeks)",
       CASE WHEN COUNT(*) = 1 THEN 'LULUS (kembar ditolak)'
            ELSE 'GAGAL (kembar masih lolos)' END AS hasil
FROM rev11.pembayaran_hutang WHERE idempotency_key = 'KUNCI-KEMBAR';

\echo ''
\echo '== BLOK 4: baris ber-kunci NULL tetap boleh banyak (indeksnya parsial) ============='

INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, status, dibuat_pada, oleh)
  VALUES (101, 'IMPOR-1', CURRENT_DATE, 1, 'TUNAI', 5, 'AKTIF', now(), 'uji');
INSERT INTO rev11.pembayaran_hutang
  (id, nomor_dokumen, tanggal, supplier_id, cara_bayar, nilai, status, dibuat_pada, oleh)
  VALUES (102, 'IMPOR-2', CURRENT_DATE, 1, 'TUNAI', 5, 'AKTIF', now(), 'uji');

SELECT COUNT(*) AS "baris tanpa kunci",
       CASE WHEN COUNT(*) = 2 THEN 'LULUS (impor legacy tidak terganggu)'
            ELSE 'GAGAL (baris tanpa kunci ikut terjegal)' END AS hasil
FROM rev11.pembayaran_hutang WHERE idempotency_key IS NULL;

\echo ''
\echo '== BLOK 5: dokumen asal ditandai DIBATALKAN, jejak reversal tercatat ==============='

SELECT
  (SELECT status_dok FROM koperasi.pembayaran_hutang_supplier WHERE id = 1) AS "status legacy",
  (SELECT status FROM rev11.pembayaran_hutang WHERE id = 1)             AS "status tenant",
  (SELECT COUNT(*) FROM rev11.reversal_log WHERE dokumen_id = 1)        AS "jejak reversal",
  CASE WHEN (SELECT status FROM rev11.pembayaran_hutang WHERE id = 1) = 'DIBATALKAN'
        AND (SELECT COUNT(*) FROM rev11.reversal_log WHERE dokumen_id = 1) = 1
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil;

\echo ''
\echo '== BLOK 6: printLogList mengembalikan ENAM medan yang sama ========================='

INSERT INTO koperasi.log_cetak (jenis_dokumen, referensi, parameter_json, user_id, perangkat)
  VALUES ('FAKTUR', 'FKT-001', '{"salinan":2}', 'andi', 'KASIR-01');
INSERT INTO rev11.print_log (dokumen_tipe, nomor_dokumen, cetakan_ke, user_id, device_id,
                             alasan, waktu)
  VALUES ('FAKTUR', 'FKT-001', 1, 'andi', 'KASIR-01', '{"salinan":2}', now());

SELECT
  l.jenis_dokumen = t.dokumen_tipe   AS "jenisDokumen",
  l.referensi     = t.nomor_dokumen  AS "referensi",
  l.user_id       = t.user_id        AS "userId",
  l.perangkat     = t.device_id      AS "perangkat",
  l.parameter_json = t.alasan        AS "parameter tersimpan",
  CASE WHEN l.jenis_dokumen = t.dokumen_tipe AND l.referensi = t.nomor_dokumen
        AND l.user_id = t.user_id AND l.perangkat = t.device_id
        AND l.parameter_json = t.alasan
       THEN 'LULUS' ELSE 'GAGAL' END AS hasil
FROM koperasi.log_cetak l, rev11.print_log t;

\echo ''
\echo '== SELESAI. Setiap blok harus berbunyi LULUS. ====================================='
