-- =====================================================================================
-- Uji kesetaraan Master: Supplier / Customer / Sales
-- =====================================================================================
--
-- Menjawab: untuk data yang SAMA, apakah kedua model menghasilkan daftar dan layar rinci
-- supplier, customer, dan sales yang sama?
--
-- CARA PAKAI -- klaster sekali-pakai, JANGAN pada basis data sungguhan:
--
--   initdb -D <dir> -U uji --auth=trust
--   pg_ctl -D <dir> -o "-p 55439" start
--   java -cp out ais.service.tenant.test.TenantSchemaDdlDump mst mst__audit > mst.sql
--   psql -h 127.0.0.1 -p 55439 -U uji -d postgres -v ON_ERROR_STOP=1 -f mst.sql
--   psql -h 127.0.0.1 -p 55439 -U uji -d postgres -f uji-kesetaraan-master.sql
--
-- YANG PALING MENENTUKAN: helper ini memindahkan LETAK MEDAN, bukan sekadar nama tabel.
--   library.penyedia + supplier_inventory_profile -> supplier + supplier_profile
--                                                    + supplier_bank_account
--   anggota_koperasi + customer_inventory_profile -> customer + customer_profile
--   sales_inventory                               -> salesperson + sales_assignment
-- Satu kolom bergeser berarti nomor rekening muncul di kolom nama.
-- =====================================================================================

\set ON_ERROR_STOP on
\set QUIET on

DROP SCHEMA IF EXISTS koperasi CASCADE;
DROP SCHEMA IF EXISTS library CASCADE;
CREATE SCHEMA koperasi;
CREATE SCHEMA library;

CREATE TABLE library.penyedia (id bigserial PRIMARY KEY, kode varchar(64), nama varchar(255),
  alamat varchar(255), kode_pos varchar(16), telp varchar(64), fax varchar(64),
  kontak varchar(128), email varchar(128), keterangan text, akun_utang bigint);
CREATE TABLE koperasi.supplier_inventory_profile (id bigserial PRIMARY KEY, penyedia bigint,
  termin_hari int, wilayah varchar(64), no_rekening varchar(64), atas_nama varchar(128),
  bank varchar(64), alamat_bank varchar(255), aktif boolean DEFAULT true);
CREATE TABLE koperasi.anggota_koperasi (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255), alamat varchar(255), telp varchar(64), hp varchar(64),
  email varchar(128), limit_kredit numeric(18,2));
CREATE TABLE koperasi.customer_inventory_profile (id bigserial PRIMARY KEY,
  anggota_koperasi bigint, termin_hari int, diskon_default_persen numeric(9,4),
  wilayah varchar(64), sales_owner bigint, aktif boolean DEFAULT true);
CREATE TABLE koperasi.toko (id bigserial PRIMARY KEY, nama varchar(255));
CREATE TABLE koperasi.sales_inventory (id bigserial PRIMARY KEY, kode varchar(64),
  nama varchar(255), nomor_perkiraan varchar(64), area varchar(64), telepon varchar(64),
  target_bulanan numeric(18,2), limit_penagihan numeric(18,2), toko bigint,
  tbmuser_id varchar(64), aktif boolean DEFAULT true);
CREATE TABLE koperasi.pengadaan_faktur (id bigserial PRIMARY KEY, supplier bigint,
  total_faktur_manual numeric(18,2), total_hitung_saat_simpan numeric(18,2));
CREATE TABLE koperasi.payable_faktur_info (id bigserial PRIMARY KEY, pengadaan_faktur bigint,
  jenis_pembayaran varchar(16), dibayar_awal numeric(18,2));
CREATE TABLE koperasi.alokasi_pembayaran_hutang_supplier (id bigserial PRIMARY KEY,
  pengadaan_faktur bigint, nominal numeric(18,2));
CREATE TABLE koperasi.piutang_customer_doc (id bigserial PRIMARY KEY, anggota_koperasi bigint,
  total_faktur numeric(18,2), dibayar_awal numeric(18,2), status varchar(16));
CREATE TABLE koperasi.alokasi_penerimaan_piutang_customer (id bigserial PRIMARY KEY,
  piutang_doc bigint, nominal numeric(18,2));

-- ------------------------------------------------------------------ data: LEGACY
INSERT INTO koperasi.toko (id, nama) VALUES (3, 'Toko Pusat');
INSERT INTO library.penyedia (id, kode, nama, alamat, kode_pos, telp, fax, kontak, email)
  VALUES (10, 'SUP-01', 'CV Sumber Kopi', 'Jl. Merdeka 1', '40111', '022-111', '022-222',
          'Pak Budi', 'sup@ex.com');
INSERT INTO koperasi.supplier_inventory_profile
    (penyedia, termin_hari, wilayah, no_rekening, atas_nama, bank, alamat_bank, aktif)
  VALUES (10, 30, 'Jawa Barat', '1234567890', 'CV Sumber Kopi', 'BCA', 'Jl. Bank 9', true);

INSERT INTO koperasi.sales_inventory
    (id, kode, nama, nomor_perkiraan, area, telepon, target_bulanan, limit_penagihan, toko,
     tbmuser_id, aktif)
  VALUES (5, 'SLS-1', 'Budi', '1100', 'Bandung', '0812-1', 50000000, 10000000, 3, 'budi', true);

INSERT INTO koperasi.anggota_koperasi (id, kode, nama, alamat, telp, hp, email, limit_kredit)
  VALUES (7, 'CUS-1', 'Warung Melati', 'Jl. Mawar 5', '022-333', '0813-9', 'cus@ex.com', 5000000);
INSERT INTO koperasi.customer_inventory_profile
    (anggota_koperasi, termin_hari, diskon_default_persen, wilayah, sales_owner, aktif)
  VALUES (7, 14, 2.5, 'Bandung', 5, true);

-- Hutang supplier: total 1.000.000, DP 200.000, dibayar 300.000 -> sisa 500.000
INSERT INTO koperasi.pengadaan_faktur (id, supplier, total_faktur_manual, total_hitung_saat_simpan)
  VALUES (1, 10, 1000000, 1000000);
INSERT INTO koperasi.payable_faktur_info (pengadaan_faktur, jenis_pembayaran, dibayar_awal)
  VALUES (1, 'DP', 200000);
INSERT INTO koperasi.alokasi_pembayaran_hutang_supplier (pengadaan_faktur, nominal)
  VALUES (1, 300000);

-- Piutang customer: total 800.000, dibayar 250.000 -> sisa 550.000
INSERT INTO koperasi.piutang_customer_doc (id, anggota_koperasi, total_faktur, dibayar_awal, status)
  VALUES (50, 7, 800000, 0, 'AKTIF');
INSERT INTO koperasi.alokasi_penerimaan_piutang_customer (piutang_doc, nominal)
  VALUES (50, 250000);

-- ------------------------------------------------------------------ data: TENANT
DELETE FROM mst.alokasi_penerimaan_piutang;
DELETE FROM mst.penerimaan_piutang;
DELETE FROM mst.piutang_customer;
DELETE FROM mst.alokasi_pembayaran_hutang;
DELETE FROM mst.pembayaran_hutang;
DELETE FROM mst.hutang_supplier;
DELETE FROM mst.supplier_bank_account;
DELETE FROM mst.supplier_profile;
DELETE FROM mst.supplier;
DELETE FROM mst.customer_profile;
DELETE FROM mst.customer;
DELETE FROM mst.sales_assignment;
DELETE FROM mst.salesperson;
DELETE FROM mst.toko;

INSERT INTO mst.toko (id, nama) VALUES (3, 'Toko Pusat');
INSERT INTO mst.supplier (id, kode, nama, aktif) VALUES (10, 'SUP-01', 'CV Sumber Kopi', true);
INSERT INTO mst.supplier_profile (supplier_id, alamat1, kode_pos, telp, fax, kontak, email,
    syarat_bayar_hari)
  VALUES (10, 'Jl. Merdeka 1', '40111', '022-111', '022-222', 'Pak Budi', 'sup@ex.com', 30);
INSERT INTO mst.supplier_bank_account (supplier_id, nama_bank, nomor_rekening, atas_nama, utama, aktif)
  VALUES (10, 'BCA', '1234567890', 'CV Sumber Kopi', true, true);

INSERT INTO mst.salesperson (id, kode, nama, akun_perkiraan, telp, aktif)
  VALUES (5, 'SLS-1', 'Budi', '1100', '0812-1', true);
INSERT INTO mst.sales_assignment (salesperson_id, toko_id, wilayah, aktif)
  VALUES (5, 3, 'Bandung', true);

INSERT INTO mst.customer (id, kode, nama, salesperson_id, aktif)
  VALUES (7, 'CUS-1', 'Warung Melati', 5, true);
INSERT INTO mst.customer_profile (customer_id, alamat, telp, email, syarat_bayar_hari, diskon,
    plafon_piutang)
  VALUES (7, 'Jl. Mawar 5', '022-333', 'cus@ex.com', 14, 2.5, 5000000);

-- Hutang: DP legacy diwakili alokasi pembayaran biasa.
INSERT INTO mst.hutang_supplier (id, supplier_id, tanggal, nilai)
  VALUES (1, 10, '2026-02-01', 1000000);
INSERT INTO mst.pembayaran_hutang (id, nomor_dokumen, tanggal, supplier_id, nilai)
  VALUES (91, 'DP-1', '2026-02-01', 10, 200000),
         (92, 'PB-1', '2026-02-05', 10, 300000);
INSERT INTO mst.alokasi_pembayaran_hutang (pembayaran_hutang_id, hutang_supplier_id, nilai)
  VALUES (91, 1, 200000), (92, 1, 300000);

INSERT INTO mst.piutang_customer (id, customer_id, tanggal, nilai, status)
  VALUES (50, 7, '2026-02-01', 800000, 'AKTIF');
INSERT INTO mst.penerimaan_piutang (id, nomor_dokumen, tanggal, customer_id, nilai)
  VALUES (81, 'RCV-1', '2026-02-06', 7, 250000);
INSERT INTO mst.alokasi_penerimaan_piutang (penerimaan_piutang_id, piutang_customer_id, nilai)
  VALUES (81, 50, 250000);

\set QUIET off

-- =====================================================================================
\echo ''
\echo '=== 1. Daftar supplier (identitas + profil + bank) ==='
WITH l AS (
  SELECT p.id, p.kode, p.nama, COALESCE(p.alamat,'') AS alamat, COALESCE(p.telp,'') AS telp,
         COALESCE(p.kontak,'') AS kontak, COALESCE(p.email,'') AS email,
         COALESCE(sp.termin_hari,0) AS termin, COALESCE(sp.no_rekening,'') AS norek,
         COALESCE(sp.atas_nama,'') AS atas_nama, COALESCE(sp.bank,'') AS bank,
         COALESCE(sp.aktif,true) AS aktif
  FROM library.penyedia p
  LEFT JOIN koperasi.supplier_inventory_profile sp ON sp.penyedia = p.id
), t AS (
  SELECT p.id, p.kode, p.nama, COALESCE(sp.alamat1,'') AS alamat, COALESCE(sp.telp,'') AS telp,
         COALESCE(sp.kontak,'') AS kontak, COALESCE(sp.email,'') AS email,
         COALESCE(sp.syarat_bayar_hari,0) AS termin,
         COALESCE((SELECT b.nomor_rekening FROM mst.supplier_bank_account b
                   WHERE b.supplier_id = p.id ORDER BY COALESCE(b.utama,false) DESC, b.id LIMIT 1),'') AS norek,
         COALESCE((SELECT b.atas_nama FROM mst.supplier_bank_account b
                   WHERE b.supplier_id = p.id ORDER BY COALESCE(b.utama,false) DESC, b.id LIMIT 1),'') AS atas_nama,
         COALESCE((SELECT b.nama_bank FROM mst.supplier_bank_account b
                   WHERE b.supplier_id = p.id ORDER BY COALESCE(b.utama,false) DESC, b.id LIMIT 1),'') AS bank,
         COALESCE(p.aktif,true) AS aktif
  FROM mst.supplier p LEFT JOIN mst.supplier_profile sp ON sp.supplier_id = p.id
)
SELECT l.kode, l.nama AS legacy_nama, t.nama AS tenant_nama,
       l.alamat AS legacy_alamat, t.alamat AS tenant_alamat,
       l.termin AS legacy_termin, t.termin AS tenant_termin,
       l.norek AS legacy_norek, t.norek AS tenant_norek,
       CASE WHEN l.nama = t.nama AND l.alamat = t.alamat AND l.telp = t.telp
                 AND l.kontak = t.kontak AND l.email = t.email AND l.termin = t.termin
                 AND l.norek = t.norek AND l.atas_nama = t.atas_nama AND l.bank = t.bank
                 AND l.aktif = t.aktif
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.kode;

\echo ''
\echo '=== 2. Daftar customer (identitas + profil + pemilik sales) ==='
WITH l AS (
  SELECT a.id, a.kode, a.nama, COALESCE(a.alamat,'') AS alamat, COALESCE(a.telp,'') AS telp,
         COALESCE(a.limit_kredit,0) AS limit_kredit, COALESCE(cp.termin_hari,0) AS termin,
         COALESCE(cp.diskon_default_persen,0) AS diskon, COALESCE(cp.sales_owner,0) AS sales_id,
         COALESCE(cp.aktif,true) AS aktif
  FROM koperasi.anggota_koperasi a
  LEFT JOIN koperasi.customer_inventory_profile cp ON cp.anggota_koperasi = a.id
), t AS (
  SELECT a.id, a.kode, a.nama, COALESCE(cp.alamat,'') AS alamat, COALESCE(cp.telp,'') AS telp,
         COALESCE(cp.plafon_piutang,0) AS limit_kredit,
         COALESCE(cp.syarat_bayar_hari,0) AS termin, COALESCE(cp.diskon,0) AS diskon,
         COALESCE(a.salesperson_id,0) AS sales_id, COALESCE(a.aktif,true) AS aktif
  FROM mst.customer a LEFT JOIN mst.customer_profile cp ON cp.customer_id = a.id
)
SELECT l.kode, l.alamat AS legacy_alamat, t.alamat AS tenant_alamat,
       l.limit_kredit AS legacy_limit, t.limit_kredit AS tenant_limit,
       l.termin AS legacy_termin, t.termin AS tenant_termin,
       l.sales_id AS legacy_sales, t.sales_id AS tenant_sales,
       CASE WHEN l.nama = t.nama AND l.alamat = t.alamat AND l.telp = t.telp
                 AND l.limit_kredit = t.limit_kredit AND l.termin = t.termin
                 AND l.diskon = t.diskon AND l.sales_id = t.sales_id AND l.aktif = t.aktif
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.kode;

\echo ''
\echo '=== 3. Daftar sales (identitas + penugasan toko/wilayah + jumlah customer) ==='
WITH l AS (
  SELECT s.id, s.kode, s.nama, COALESCE(s.nomor_perkiraan,'') AS akun,
         COALESCE(s.area,'') AS area, COALESCE(s.telepon,'') AS telp,
         COALESCE(s.toko,0) AS toko_id, COALESCE(s.aktif,true) AS aktif,
         (SELECT COUNT(*) FROM koperasi.customer_inventory_profile cip
          WHERE cip.sales_owner = s.id AND COALESCE(cip.aktif,true) = true) AS jml_cust
  FROM koperasi.sales_inventory s
), t AS (
  SELECT s.id, s.kode, s.nama, COALESCE(s.akun_perkiraan,'') AS akun,
         COALESCE(sa.wilayah,'') AS area, COALESCE(s.telp,'') AS telp,
         COALESCE(sa.toko_id,0) AS toko_id, COALESCE(s.aktif,true) AS aktif,
         (SELECT COUNT(*) FROM mst.customer c
          WHERE c.salesperson_id = s.id AND COALESCE(c.aktif,true) = true) AS jml_cust
  FROM mst.salesperson s
  LEFT JOIN LATERAL (SELECT sa.toko_id, sa.wilayah FROM mst.sales_assignment sa
                     WHERE sa.salesperson_id = s.id AND COALESCE(sa.aktif,true) = true
                     ORDER BY sa.berlaku_dari DESC NULLS LAST, sa.id DESC LIMIT 1) sa ON true
)
SELECT l.kode, l.akun AS legacy_akun, t.akun AS tenant_akun,
       l.area AS legacy_area, t.area AS tenant_area,
       l.toko_id AS legacy_toko, t.toko_id AS tenant_toko,
       l.jml_cust AS legacy_jml, t.jml_cust AS tenant_jml,
       CASE WHEN l.nama = t.nama AND l.akun = t.akun AND l.area = t.area AND l.telp = t.telp
                 AND l.toko_id = t.toko_id AND l.aktif = t.aktif AND l.jml_cust = t.jml_cust
            THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hasil
FROM l JOIN t ON t.id = l.id ORDER BY l.kode;

\echo ''
\echo '=== 4. Saldo hutang pemasok dan saldo piutang pelanggan (layar rinci) ==='
SELECT
  (SELECT COALESCE(SUM(COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0))
     - COALESCE(i.dibayar_awal,0)
     - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_pembayaran_hutang_supplier a
                 WHERE a.pengadaan_faktur = f.id),0)),0)
   FROM koperasi.pengadaan_faktur f
   JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id
   WHERE f.supplier = 10 AND i.jenis_pembayaran IN ('DP','CREDIT')) AS legacy_hutang,
  (SELECT COALESCE(SUM(COALESCE(h.nilai,0)
     - COALESCE((SELECT SUM(a.nilai) FROM mst.alokasi_pembayaran_hutang a
                 WHERE a.hutang_supplier_id = h.id),0)),0)
   FROM mst.hutang_supplier h WHERE h.supplier_id = 10) AS tenant_hutang,
  (SELECT COALESCE(SUM(COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0)
     - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                 WHERE a.piutang_doc = d.id),0)),0)
   FROM koperasi.piutang_customer_doc d WHERE d.anggota_koperasi = 7) AS legacy_piutang,
  (SELECT COALESCE(SUM(COALESCE(d.nilai,0)
     - COALESCE((SELECT SUM(a.nilai) FROM mst.alokasi_penerimaan_piutang a
                 WHERE a.piutang_customer_id = d.id),0)),0)
   FROM mst.piutang_customer d WHERE d.customer_id = 7) AS tenant_piutang;

\echo ''
\echo '--- verdict saldo ---'
SELECT CASE WHEN
  (SELECT COALESCE(SUM(COALESCE(f.total_faktur_manual,0) - COALESCE(i.dibayar_awal,0)
     - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_pembayaran_hutang_supplier a
                 WHERE a.pengadaan_faktur = f.id),0)),0)
   FROM koperasi.pengadaan_faktur f
   JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id WHERE f.supplier = 10)
  = (SELECT COALESCE(SUM(COALESCE(h.nilai,0)
     - COALESCE((SELECT SUM(a.nilai) FROM mst.alokasi_pembayaran_hutang a
                 WHERE a.hutang_supplier_id = h.id),0)),0)
   FROM mst.hutang_supplier h WHERE h.supplier_id = 10)
  THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS hutang,
  CASE WHEN
  (SELECT COALESCE(SUM(COALESCE(d.total_faktur,0)
     - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a
                 WHERE a.piutang_doc = d.id),0)),0)
   FROM koperasi.piutang_customer_doc d WHERE d.anggota_koperasi = 7)
  = (SELECT COALESCE(SUM(COALESCE(d.nilai,0)
     - COALESCE((SELECT SUM(a.nilai) FROM mst.alokasi_penerimaan_piutang a
                 WHERE a.piutang_customer_id = d.id),0)),0)
   FROM mst.piutang_customer d WHERE d.customer_id = 7)
  THEN 'SETARA' ELSE '*** BERSELISIH ***' END AS piutang;

-- =====================================================================================
-- Perbedaan yang DISENGAJA
-- =====================================================================================
\echo ''
\echo '=== Perbedaan disengaja: wilayah mitra tidak punya padanan ==='
-- Wilayah pemasok/pelanggan sengaja TIDAK dipetakan ke kota. Keduanya berdekatan tetapi
-- berbeda: wilayah adalah pembagian penjualan, kota adalah bagian alamat. Memetakannya
-- membuat saringan "wilayah = Jawa Barat" mencari KOTA bernama demikian dan mengembalikan
-- nol baris -- saringan yang tampak bekerja padahal tidak pernah cocok.
--
-- Wilayah SALES berbeda: model tenant menyimpannya di sales_assignment (dibandingkan di
-- blok 3 di atas dan SETARA).
SELECT (SELECT wilayah FROM koperasi.supplier_inventory_profile WHERE penyedia = 10)
         AS legacy_wilayah_supplier,
       '' AS tenant_wilayah_supplier,
       (SELECT wilayah FROM koperasi.customer_inventory_profile WHERE anggota_koperasi = 7)
         AS legacy_wilayah_customer,
       '' AS tenant_wilayah_customer,
       'kosong DISENGAJA -- lihat catatan di atas' AS catatan;

\echo ''
\echo 'Selesai. Blok 1-4 harus SETARA.'
