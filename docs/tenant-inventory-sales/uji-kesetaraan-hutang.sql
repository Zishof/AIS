-- =====================================================================================
-- Uji kesetaraan Hutang Supplier: model legacy (faktur + info sisi) vs model tenant
-- =====================================================================================
--
-- Menjawab: untuk peristiwa yang SAMA, apakah kedua model menghasilkan sisa hutang, umur
-- hutang, dan laporan pembelian yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55435" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump pay pay__audit > pay.sql
--   psql -h 127.0.0.1 -p 55435 -U uji -d postgres -v ON_ERROR_STOP=1 -f pay.sql
--   psql -h 127.0.0.1 -p 55435 -U uji -d postgres -f uji-kesetaraan-hutang.sql
--
-- PEMETAAN YANG DIUJI
--   pengadaan_faktur + payable_faktur_info  ->  hutang_supplier
--   alokasi_pembayaran_hutang_supplier      ->  alokasi_pembayaran_hutang
--   pembayaran_hutang_supplier              ->  pembayaran_hutang
--   library.penyedia                        ->  supplier
--   i.dibayar_awal                          ->  alokasi pembayaran biasa
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
DROP SCHEMA IF EXISTS library CASCADE;
CREATE SCHEMA koperasi;
CREATE SCHEMA library;

CREATE TABLE library.penyedia (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255),
  alamat varchar(255));
CREATE TABLE koperasi.pengadaan_faktur (id bigserial PRIMARY KEY, supplier bigint,
  nomor_faktur varchar(64), tanggal_faktur date, total_faktur_manual numeric(18,2),
  total_hitung_saat_simpan numeric(18,2), diskon numeric(18,2));
CREATE TABLE koperasi.payable_faktur_info (id bigserial PRIMARY KEY, pengadaan_faktur bigint,
  jenis_pembayaran varchar(16), termin_hari int, jatuh_tempo date,
  dibayar_awal numeric(18,2), keterangan text);
CREATE TABLE koperasi.pembayaran_hutang_supplier (id bigserial PRIMARY KEY, supplier bigint,
  tanggal date, nominal numeric(18,2), metode varchar(32), kode_unik varchar(128));
CREATE TABLE koperasi.alokasi_pembayaran_hutang_supplier (id bigserial PRIMARY KEY,
  pembayaran bigint, pengadaan_faktur bigint, nominal numeric(18,2));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO library.penyedia (id, kode, nama, alamat)
  VALUES (10, 'SUP-01', 'CV Sumber Kopi', 'Jl. Merdeka 1');

-- F-1 KREDIT: total 1.000.000, DP 200.000, dibayar 300.000 -> sisa 500.000
INSERT INTO koperasi.pengadaan_faktur (id, supplier, nomor_faktur, tanggal_faktur,
    total_faktur_manual, total_hitung_saat_simpan, diskon)
  VALUES (1, 10, 'F-1', '2026-01-10', 1000000, 1000000, 25000);
INSERT INTO koperasi.payable_faktur_info (pengadaan_faktur, jenis_pembayaran, termin_hari,
    jatuh_tempo, dibayar_awal)
  VALUES (1, 'DP', 30, '2026-02-09', 200000);

-- F-2 KREDIT: total 500.000, tanpa DP, belum dibayar -> sisa 500.000, sudah lewat tempo
INSERT INTO koperasi.pengadaan_faktur (id, supplier, nomor_faktur, tanggal_faktur,
    total_faktur_manual, total_hitung_saat_simpan, diskon)
  VALUES (2, 10, 'F-2', '2025-10-01', 500000, 500000, 0);
INSERT INTO koperasi.payable_faktur_info (pengadaan_faktur, jenis_pembayaran, termin_hari,
    jatuh_tempo, dibayar_awal)
  VALUES (2, 'CREDIT', 30, '2025-10-31', 0);

-- F-3 TUNAI: tidak menimbulkan hutang, TETAPI harus muncul di laporan pembelian.
INSERT INTO koperasi.pengadaan_faktur (id, supplier, nomor_faktur, tanggal_faktur,
    total_faktur_manual, total_hitung_saat_simpan, diskon)
  VALUES (3, 10, 'F-3', '2026-01-20', 250000, 250000, 5000);
INSERT INTO koperasi.payable_faktur_info (pengadaan_faktur, jenis_pembayaran, termin_hari,
    jatuh_tempo, dibayar_awal)
  VALUES (3, 'CASH', 0, '2026-01-20', 250000);

INSERT INTO koperasi.pembayaran_hutang_supplier (id, supplier, tanggal, nominal, metode, kode_unik)
  VALUES (100, 10, '2026-01-25', 300000, 'TRANSFER', 'UNIK-1');
INSERT INTO koperasi.alokasi_pembayaran_hutang_supplier (pembayaran, pengadaan_faktur, nominal)
  VALUES (100, 1, 300000);

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM pay.alokasi_pembayaran_hutang;
DELETE FROM pay.pembayaran_hutang;
DELETE FROM pay.hutang_supplier;
DELETE FROM pay.pembelian;
DELETE FROM pay.supplier_profile;
DELETE FROM pay.supplier;

INSERT INTO pay.supplier (id, kode, nama) VALUES (10, 'SUP-01', 'CV Sumber Kopi');
INSERT INTO pay.supplier_profile (supplier_id, alamat1) VALUES (10, 'Jl. Merdeka 1');

-- Dokumen pembelian: KETIGANYA, termasuk yang tunai.
INSERT INTO pay.pembelian (id, nomor_dokumen, nomor_faktur, tanggal, supplier_id, total, diskon) VALUES
  (1, 'PB-001', 'F-1', '2026-01-10', 10, 1000000, 25000),
  (2, 'PB-002', 'F-2', '2025-10-01', 10,  500000,      0),
  (3, 'PB-003', 'F-3', '2026-01-20', 10,  250000,   5000);

-- Hutang: HANYA yang berhutang. F-3 tunai tidak melahirkan baris di sini.
INSERT INTO pay.hutang_supplier (id, supplier_id, pembelian_id, nomor_faktur, tanggal,
    jatuh_tempo, nilai) VALUES
  (1, 10, 1, 'F-1', '2026-01-10', '2026-02-09', 1000000),
  (2, 10, 2, 'F-2', '2025-10-01', '2025-10-31',  500000);

-- DP legacy diwakili alokasi pembayaran biasa, sesuai pemetaan.
INSERT INTO pay.pembayaran_hutang (id, nomor_dokumen, tanggal, supplier_id, cara_bayar,
    nilai, idempotency_key)
  VALUES (99, 'UNIK-DP', '2026-01-10', 10, 'TUNAI', 200000, 'UNIK-DP'),
         (100, 'UNIK-1', '2026-01-25', 10, 'TRANSFER', 300000, 'UNIK-1');
INSERT INTO pay.alokasi_pembayaran_hutang (pembayaran_hutang_id, hutang_supplier_id, nilai)
  VALUES (99, 1, 200000), (100, 1, 300000);

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Sisa hutang per faktur ==='
WITH l AS (
  SELECT f.id, COALESCE(f.nomor_faktur,'') AS faktur,
         COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0)) AS total,
         COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0))
           - COALESCE(i.dibayar_awal,0)
           - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_pembayaran_hutang_supplier a
                       WHERE a.pengadaan_faktur = f.id),0) AS sisa
  FROM koperasi.pengadaan_faktur f
  JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id
  WHERE i.jenis_pembayaran IN ('DP','CREDIT')
), t AS (
  SELECT h.id, COALESCE(h.nomor_faktur,'') AS faktur, COALESCE(h.nilai,0) AS total,
         COALESCE(h.nilai,0)
           - COALESCE((SELECT SUM(a.nilai) FROM pay.alokasi_pembayaran_hutang a
                       WHERE a.hutang_supplier_id = h.id),0) AS sisa
  FROM pay.hutang_supplier h
)
SELECT l.faktur, l.total AS legacy_total, t.total AS tenant_total,
       l.sisa AS legacy_sisa, t.sisa AS tenant_sisa,
       CASE WHEN l.total = t.total AND l.sisa = t.sisa
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.faktur;

\echo ''
\echo '=== 2. Umur hutang (acuan 2026-02-15) ==='
WITH l AS (
  SELECT COALESCE(f.nomor_faktur,'') AS faktur,
         CASE WHEN i.jatuh_tempo IS NULL OR i.jatuh_tempo >= DATE '2026-02-15' THEN 'BELUM'
              WHEN (DATE '2026-02-15' - i.jatuh_tempo) <= 30 THEN 'B1_30'
              WHEN (DATE '2026-02-15' - i.jatuh_tempo) <= 60 THEN 'B31_60'
              WHEN (DATE '2026-02-15' - i.jatuh_tempo) <= 90 THEN 'B61_90'
              ELSE 'B90' END AS bucket
  FROM koperasi.pengadaan_faktur f
  JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id
  WHERE i.jenis_pembayaran IN ('DP','CREDIT')
), t AS (
  SELECT COALESCE(h.nomor_faktur,'') AS faktur,
         CASE WHEN h.jatuh_tempo IS NULL OR h.jatuh_tempo >= DATE '2026-02-15' THEN 'BELUM'
              WHEN (DATE '2026-02-15' - h.jatuh_tempo) <= 30 THEN 'B1_30'
              WHEN (DATE '2026-02-15' - h.jatuh_tempo) <= 60 THEN 'B31_60'
              WHEN (DATE '2026-02-15' - h.jatuh_tempo) <= 90 THEN 'B61_90'
              ELSE 'B90' END AS bucket
  FROM pay.hutang_supplier h
)
SELECT l.faktur, l.bucket AS legacy_bucket, t.bucket AS tenant_bucket,
       CASE WHEN l.bucket = t.bucket THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.faktur = l.faktur ORDER BY l.faktur;

\echo ''
\echo '=== 3. Laporan pembelian -- HARUS memuat pembelian TUNAI juga ==='
-- Bagian paling mudah salah: laporan disusun dari dokumen pembelian, bukan dari tabel
-- hutang. Menyusunnya dari hutang akan menghilangkan seluruh pembelian tunai tanpa satu
-- pun galat muncul -- angka yang salah, terlihat benar.
WITH l AS (
  SELECT COALESCE(f.nomor_faktur,'') AS faktur,
         COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0)) AS total,
         COALESCE(i.jenis_pembayaran,'CASH') AS jenis, COALESCE(f.diskon,0) AS diskon
  FROM koperasi.pengadaan_faktur f
  LEFT JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id
), t AS (
  SELECT COALESCE(b.nomor_faktur,'') AS faktur, COALESCE(b.total,0) AS total,
         CASE WHEN h.id IS NULL THEN 'CASH' ELSE 'CREDIT' END AS jenis,
         COALESCE(b.diskon,0) AS diskon
  FROM pay.pembelian b
  LEFT JOIN pay.hutang_supplier h ON h.pembelian_id = b.id
)
SELECT l.faktur, l.total AS legacy_total, t.total AS tenant_total,
       l.jenis AS legacy_jenis, t.jenis AS tenant_jenis,
       l.diskon AS legacy_diskon, t.diskon AS tenant_diskon,
       CASE WHEN l.total = t.total AND l.diskon = t.diskon
                 AND (l.jenis = t.jenis OR (l.jenis = 'DP' AND t.jenis = 'CREDIT'))
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.faktur = l.faktur ORDER BY l.faktur;

\echo ''
\echo '--- jumlah baris laporan harus sama (tunai tidak boleh hilang) ---'
SELECT (SELECT COUNT(*) FROM koperasi.pengadaan_faktur) AS baris_legacy,
       (SELECT COUNT(*) FROM pay.pembelian) AS baris_tenant,
       CASE WHEN (SELECT COUNT(*) FROM koperasi.pengadaan_faktur)
                 = (SELECT COUNT(*) FROM pay.pembelian)
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil;

-- =====================================================================================
-- Perbedaan yang DISENGAJA
-- =====================================================================================
\echo ''
\echo '=== Perbedaan disengaja: DP legacy menjadi alokasi pembayaran biasa ==='
-- Legacy menyimpan uang muka pada kolom terpisah (i.dibayar_awal) yang tidak punya
-- dokumen pembayaran. Model tenant mencatatnya sebagai pembayaran beralokasi seperti
-- pembayaran lain, sehingga uang muka pun punya jejak dokumennya sendiri.
SELECT 'F-1' AS faktur,
       (SELECT i.dibayar_awal FROM koperasi.payable_faktur_info i WHERE i.pengadaan_faktur = 1)
         AS legacy_kolom_dp,
       (SELECT COUNT(*) FROM koperasi.alokasi_pembayaran_hutang_supplier a WHERE a.pengadaan_faktur = 1)
         AS legacy_jumlah_alokasi,
       (SELECT COUNT(*) FROM pay.alokasi_pembayaran_hutang a WHERE a.hutang_supplier_id = 1)
         AS tenant_jumlah_alokasi,
       'DP tenant punya dokumen pembayaran sendiri -- jejaknya lebih lengkap' AS catatan;

\echo ''
\echo 'Selesai. Seluruh baris hasil harus SETARA.'
