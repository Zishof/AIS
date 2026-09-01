-- =====================================================================================
-- Uji kesetaraan Harga: model legacy (koperasi + library) vs model tenant
-- =====================================================================================
--
-- Menjawab: untuk data harga yang SAMA, apakah kedua model menghasilkan daftar harga
-- supplier, daftar harga customer, dan analisa harga yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55434" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump hrg hrg__audit > hrg.sql
--   psql -h 127.0.0.1 -p 55434 -U uji -d postgres -v ON_ERROR_STOP=1 -f hrg.sql
--   psql -h 127.0.0.1 -p 55434 -U uji -d postgres -f uji-kesetaraan-harga.sql
--
-- Schema `koperasi` dan `library` di bawah adalah tiruan berisi HANYA kolom yang dipakai
-- SalesInventoryHargaHelper. Bukan replika lengkap AIS.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
DROP SCHEMA IF EXISTS library CASCADE;
CREATE SCHEMA koperasi;
CREATE SCHEMA library;

CREATE TABLE koperasi.satuan_produk (id bigserial PRIMARY KEY, nama varchar(64));
CREATE TABLE koperasi.produk (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255),
  satuan bigint, hargabeli numeric(18,2), hargajual numeric(18,2), stok numeric(18,4),
  aktif boolean DEFAULT true, toko bigint);
-- Supplier legacy TIDAK berada di schema koperasi, melainkan library.
CREATE TABLE library.penyedia (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.anggota_koperasi (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255));
CREATE TABLE koperasi.harga_beli_supplier (id bigserial PRIMARY KEY, supplier bigint, produk bigint,
  harga numeric(18,2), tanggal_efektif date, keterangan text, aktif boolean DEFAULT true,
  oleh varchar(255));
CREATE TABLE koperasi.harga_jual_customer (id bigserial PRIMARY KEY, anggota_koperasi bigint,
  produk bigint, harga numeric(18,2), tanggal_efektif date, keterangan text,
  aktif boolean DEFAULT true, oleh varchar(255));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.satuan_produk (id, nama) VALUES (1, 'PCS');
INSERT INTO library.penyedia (id, kode, nama) VALUES (10, 'SUP-01', 'CV Sumber Kopi');
INSERT INTO koperasi.anggota_koperasi (id, kode, nama) VALUES (20, 'AGT-01', 'Warung Melati');
INSERT INTO koperasi.produk (id, kode, nama, satuan, hargabeli, hargajual, stok) VALUES
  (1, 'P-001', 'Kopi Bubuk', 1, 2500, 4000, 0),
  (2, 'P-002', 'Gula Pasir', 1, 1200, 1000, 0);   -- margin NEGATIF disengaja

INSERT INTO koperasi.harga_beli_supplier (id, supplier, produk, harga, tanggal_efektif, aktif, oleh)
  VALUES (100, 10, 1, 2400, '2026-01-10', true, 'admin'),
         (101, 10, 1, 2500, '2026-02-01', true, 'admin');
INSERT INTO koperasi.harga_jual_customer (id, anggota_koperasi, produk, harga, tanggal_efektif, aktif, oleh)
  VALUES (200, NULL, 1, 4000, '2026-02-01', true, 'admin'),
         (201, 20,   1, 3800, '2026-02-01', true, 'admin');

-- Stok legacy adalah kolom; supaya setara dengan model tenant yang menurunkannya dari
-- buku besar, kolom itu diisi dari total mutasi yang sama.
UPDATE koperasi.produk SET stok = 25 WHERE id = 1;

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM hrg.harga_jual_customer;
DELETE FROM hrg.harga_beli_supplier;
DELETE FROM hrg.mutasi_stok;
DELETE FROM hrg.produk;
DELETE FROM hrg.satuan;
DELETE FROM hrg.customer;
DELETE FROM hrg.supplier;

INSERT INTO hrg.satuan (id, kode, nama) VALUES (1, 'PCS', 'PCS');
INSERT INTO hrg.supplier (id, kode, nama) VALUES (10, 'SUP-01', 'CV Sumber Kopi');
INSERT INTO hrg.customer (id, kode, nama) VALUES (20, 'AGT-01', 'Warung Melati');
INSERT INTO hrg.produk (id, kode, nama, satuan_id, harga_beli_terakhir, harga_jual_standar) VALUES
  (1, 'P-001', 'Kopi Bubuk', 1, 2500, 4000),
  (2, 'P-002', 'Gula Pasir', 1, 1200, 1000);
-- Stok tenant = turunan buku besar; 30 masuk - 5 keluar = 25, sama dengan kolom legacy.
INSERT INTO hrg.mutasi_stok (produk_id, tanggal, jenis, arah, kuantitas) VALUES
  (1, '2026-01-15', 'PENGADAAN', 1, 30),
  (1, '2026-01-20', 'PENJUALAN', -1, 5);

INSERT INTO hrg.harga_beli_supplier (id, supplier_id, produk_id, harga, berlaku_dari, aktif, oleh)
  VALUES (100, 10, 1, 2400, '2026-01-10', true, 'admin'),
         (101, 10, 1, 2500, '2026-02-01', true, 'admin');
-- Baris harga UMUM legacy (id 200, anggota NULL) TIDAK punya padanan di sini:
-- hrg.harga_jual_customer.customer_id NOT NULL, sehingga baris tanpa customer mustahil.
-- Harga umum pada model tenant diwakili produk.harga_jual_standar (= 4000 di atas).
INSERT INTO hrg.harga_jual_customer (id, customer_id, produk_id, harga, berlaku_dari, aktif, oleh)
  VALUES (201, 20, 1, 3800, '2026-02-01', true, 'admin');

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Daftar harga beli supplier ==='
WITH l AS (
  SELECT h.id, h.supplier AS pihak, s.kode AS pihak_kode, s.nama AS pihak_nama,
         h.produk, p.kode AS produk_kode, h.harga, h.tanggal_efektif AS tanggal
  FROM koperasi.harga_beli_supplier h
  JOIN library.penyedia s ON h.supplier = s.id
  JOIN koperasi.produk p ON h.produk = p.id
), t AS (
  SELECT h.id, h.supplier_id AS pihak, s.kode AS pihak_kode, s.nama AS pihak_nama,
         h.produk_id AS produk, p.kode AS produk_kode, h.harga, h.berlaku_dari AS tanggal
  FROM hrg.harga_beli_supplier h
  JOIN hrg.supplier s ON h.supplier_id = s.id
  JOIN hrg.produk p ON h.produk_id = p.id
)
SELECT l.id, l.pihak_kode, l.produk_kode, l.harga AS legacy_harga, t.harga AS tenant_harga,
       l.tanggal AS legacy_tanggal, t.tanggal AS tenant_tanggal,
       CASE WHEN l.pihak = t.pihak AND l.pihak_kode = t.pihak_kode
                 AND l.pihak_nama = t.pihak_nama AND l.produk = t.produk
                 AND l.harga = t.harga AND l.tanggal = t.tanggal
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.id;

\echo ''
\echo '=== 2. Daftar harga jual customer (termasuk baris UMUM) ==='
WITH l AS (
  SELECT h.id, h.anggota_koperasi AS pihak, COALESCE(a.kode,'') AS pihak_kode,
         COALESCE(a.nama,'(Umum)') AS pihak_nama, h.harga, h.tanggal_efektif AS tanggal
  FROM koperasi.harga_jual_customer h
  LEFT JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id
  JOIN koperasi.produk p ON h.produk = p.id
), t AS (
  SELECT h.id, h.customer_id AS pihak, COALESCE(a.kode,'') AS pihak_kode,
         COALESCE(a.nama,'(Umum)') AS pihak_nama, h.harga, h.berlaku_dari AS tanggal
  FROM hrg.harga_jual_customer h
  LEFT JOIN hrg.customer a ON h.customer_id = a.id
  JOIN hrg.produk p ON h.produk_id = p.id
)
SELECT l.id, l.pihak_nama AS legacy_pihak, t.pihak_nama AS tenant_pihak,
       l.harga AS legacy_harga, t.harga AS tenant_harga,
       CASE WHEN l.pihak IS NOT DISTINCT FROM t.pihak AND l.pihak_kode = t.pihak_kode
                 AND l.pihak_nama = t.pihak_nama AND l.harga = t.harga
                 AND l.tanggal = t.tanggal
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.id;

\echo ''
\echo '--- baris harga UMUM legacy tanpa padanan tenant (gap customer_id NOT NULL) ---'
SELECT h.id, COALESCE(a.nama,'(Umum)') AS pihak, h.harga,
       'tidak dapat disimpan di model tenant -- diwakili produk.harga_jual_standar' AS catatan
FROM koperasi.harga_jual_customer h
LEFT JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id
WHERE h.anggota_koperasi IS NULL;

\echo ''
\echo '=== 3. Analisa harga: stok, harga umum, harga beli terbaru ==='
WITH l AS (
  SELECT p.id, p.kode, COALESCE(p.stok,0) AS stok,
    (SELECT h.harga FROM koperasi.harga_jual_customer h WHERE h.produk = p.id
       AND h.anggota_koperasi IS NULL AND COALESCE(h.aktif,true) = true
       AND h.tanggal_efektif <= CURRENT_DATE
     ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1) AS harga_umum,
    (SELECT h.harga FROM koperasi.harga_beli_supplier h WHERE h.produk = p.id
       AND COALESCE(h.aktif,true) = true AND h.tanggal_efektif <= CURRENT_DATE
     ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1) AS harga_beli_terbaru
  FROM koperasi.produk p
), t AS (
  SELECT p.id, p.kode,
    COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM hrg.mutasi_stok m
              WHERE m.produk_id = p.id),0) AS stok,
    -- Model tenant tidak punya baris harga umum; harga_jual_standar yang mewakilinya.
    p.harga_jual_standar AS harga_umum,
    (SELECT h.harga FROM hrg.harga_beli_supplier h WHERE h.produk_id = p.id
       AND COALESCE(h.aktif,true) = true AND h.berlaku_dari <= CURRENT_DATE
       AND (h.berlaku_sampai IS NULL OR h.berlaku_sampai >= CURRENT_DATE)
     ORDER BY h.berlaku_dari DESC, h.id DESC LIMIT 1) AS harga_beli_terbaru
  FROM hrg.produk p
)
-- Yang WAJIB setara: stok dan harga beli terbaru. Keduanya punya padanan penuh.
SELECT l.kode, l.stok AS legacy_stok, t.stok AS tenant_stok,
       l.harga_beli_terbaru AS legacy_beli, t.harga_beli_terbaru AS tenant_beli,
       CASE WHEN l.stok = t.stok
                 AND l.harga_beli_terbaru IS NOT DISTINCT FROM t.harga_beli_terbaru
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.kode;

\echo ''
\echo '--- harga umum: KONSEKUENSI gap customer_id, dilaporkan bukan diklaim setara ---'
-- Legacy mengosongkannya bila tidak ada baris harga umum eksplisit. Model tenant tidak
-- dapat menyimpan baris semacam itu, sehingga jalur tenant memakai produk.harga_jual_standar
-- -- yang SELALU terisi. Akibatnya kolom ini terisi pada produk yang di legacy kosong.
-- Ini konsekuensi pemetaan, bukan kesalahan hitung; hilang bila migrasi v10 melonggarkan
-- customer_id sehingga baris harga umum dapat disimpan sebagaimana mestinya.
SELECT lp.kode,
       (SELECT h.harga FROM koperasi.harga_jual_customer h WHERE h.produk = lp.id
          AND h.anggota_koperasi IS NULL AND COALESCE(h.aktif,true) = true
          AND h.tanggal_efektif <= CURRENT_DATE
        ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1) AS legacy_umum,
       tp.harga_jual_standar AS tenant_umum,
       CASE WHEN (SELECT h.harga FROM koperasi.harga_jual_customer h WHERE h.produk = lp.id
                    AND h.anggota_koperasi IS NULL AND COALESCE(h.aktif,true) = true
                    AND h.tanggal_efektif <= CURRENT_DATE
                  ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1)
                 IS NOT DISTINCT FROM tp.harga_jual_standar
            THEN 'kebetulan sama'
            WHEN (SELECT COUNT(*) FROM koperasi.harga_jual_customer h WHERE h.produk = lp.id
                    AND h.anggota_koperasi IS NULL) = 0
            THEN 'legacy kosong, tenant memakai harga jual standar'
            ELSE 'PERIKSA -- selisih di luar dugaan' END AS catatan
FROM koperasi.produk lp JOIN hrg.produk tp ON tp.id = lp.id ORDER BY lp.kode;

-- =====================================================================================
-- Perbedaan yang DISENGAJA: harga kedaluwarsa
-- =====================================================================================
-- Legacy hanya menyimpan satu tanggal efektif, sehingga harga lama tetap terpilih
-- selamanya. Model tenant menyimpan RENTANG, sehingga harga yang masa berlakunya sudah
-- habis tidak lagi disodorkan. Bagian ini memperagakan selisihnya, bukan menggagalkannya.
\echo ''
\echo '=== Perbedaan disengaja: harga yang masa berlakunya sudah habis ==='
UPDATE hrg.harga_beli_supplier SET berlaku_sampai = '2026-02-10' WHERE id = 101;

SELECT
  (SELECT h.harga FROM koperasi.harga_beli_supplier h WHERE h.produk = 1
     AND COALESCE(h.aktif,true) = true AND h.tanggal_efektif <= CURRENT_DATE
   ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1) AS legacy_masih_menyodorkan,
  (SELECT h.harga FROM hrg.harga_beli_supplier h WHERE h.produk_id = 1
     AND COALESCE(h.aktif,true) = true AND h.berlaku_dari <= CURRENT_DATE
     AND (h.berlaku_sampai IS NULL OR h.berlaku_sampai >= CURRENT_DATE)
   ORDER BY h.berlaku_dari DESC, h.id DESC LIMIT 1) AS tenant_mundur_ke_versi_berlaku,
  'selisih DISENGAJA -- legacy tidak punya tanggal akhir berlaku' AS catatan;

\echo ''
\echo 'Selesai. Ketiga blok di atas harus SETARA pada seluruh barisnya.'
