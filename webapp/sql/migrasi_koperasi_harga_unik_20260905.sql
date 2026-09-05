-- ============================================================================
-- Integritas finansial: cegah dua versi Master Harga (Beli Supplier / Jual
-- Customer, skema koperasi -- BUKAN skema tenant) tersimpan pada kombinasi
-- pihak+produk+tanggal_efektif yang SAMA.
--
-- SalesInventoryHargaHelper.supplierPriceSave/customerPriceSave hanya menjaga
-- keunikan ini lewat "SELECT COUNT(*) ... lalu INSERT" (check-then-insert)
-- TANPA FOR UPDATE/advisory lock/constraint DB -- dua request simpan yang
-- benar-benar bersamaan pada kombinasi yang sama bisa lolos keduanya (TOCTOU).
-- Akibatnya dua baris "unik" sama-sama aktif=true; resolusi harga transaksi
-- (ORDER BY tanggal_efektif DESC, id DESC LIMIT 1) diam-diam memilih baris
-- id-terbesar tanpa galat -- bila baris itu kelak dinonaktifkan admin yang
-- tidak sadar ada duplikat, baris "kalah" yang lebih lama diam-diam menjadi
-- harga aktif berikutnya dengan nilai yang mungkin berbeda. Lihat javadoc
-- HargaBeliSupplier.getTanggalEfektif()/HargaJualCustomer.getTanggalEfektif().
--
-- Skrip ini menutup jalur tsb dengan UNIQUE INDEX -- rem terakhir yang tidak
-- bergantung pada penjaga sisi aplikasi. Kode pemanggil (SalesInventoryHargaHelper)
-- sudah diubah menangkap pelanggaran index ini (ConstraintViolationException)
-- sebagai penolakan bersih, bukan biar tembus jadi error 500.
--
-- Skema tenant (TenantSchemaMigrationsV10, {S}.harga_jual_customer) SUDAH
-- punya index parsial serupa utk baris umum; skema koperasi (shared, dikelola
-- hbm2ddl.auto=update -- BUKAN katalog migrasi ber-checksum spt tenant) belum
-- pernah punya satu pun. hbm2ddl Hibernate 3.6 tidak sanggup membuat index
-- unik PARSIAL (klausa WHERE) dari anotasi, jadi ini WAJIB dijalankan manual.
--
-- Aman dijalankan berulang (IF NOT EXISTS di semua DDL). Pembuatan index
-- DILEWATI (bukan gagal) bila data historis sudah terlanjur berduplikat pada
-- kombinasi yg sama persis -- lihat AUDIT di bagian akhir (read-only) untuk
-- mengukur & membersihkannya dulu, baru jalankan ulang skrip ini.
--
-- Jalankan SEKALI di database produksi:
--   psql -d <database_ais> -f migrasi_koperasi_harga_unik_20260905.sql
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------- supplier
-- Tidak perlu index parsial: kolom supplier NOT NULL (tidak ada konsep
-- "harga beli umum tanpa supplier", beda dari sisi jual).
DO $$
DECLARE
    v_dupe bigint;
BEGIN
    SELECT count(*) INTO v_dupe
      FROM (
          SELECT supplier, produk, tanggal_efektif
            FROM koperasi.harga_beli_supplier
           GROUP BY supplier, produk, tanggal_efektif
          HAVING count(*) > 1
      ) d;

    IF v_dupe = 0 THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes
                        WHERE schemaname = 'koperasi'
                          AND indexname = 'uq_koperasi_harga_beli_supplier') THEN
            CREATE UNIQUE INDEX uq_koperasi_harga_beli_supplier
                ON koperasi.harga_beli_supplier (supplier, produk, tanggal_efektif);
        END IF;
    ELSE
        RAISE NOTICE 'Index unik harga_beli_supplier (supplier, produk, tanggal_efektif) DILEWATI: % kombinasi duplikat ditemukan. Bersihkan dulu (lihat AUDIT 1), lalu jalankan ulang skrip ini.',
            v_dupe;
    END IF;
END $$;

-- ---------------------------------------------------------------- customer
-- Dua index parsial, sebab anggota_koperasi NULLABLE (NULL = baris harga
-- UMUM, layar 13): NULL <> NULL di Postgres sehingga satu index komposit
-- biasa tidak akan menjaga keunikan pasangan (produk, tanggal_efektif) pada
-- baris umum -- perlu klausa WHERE terpisah per kasus, pola sama persis
-- TenantSchemaMigrationsV10 (uq_{SU}_harga_jual_umum).
DO $$
DECLARE
    v_dupe_umum bigint;
    v_dupe_khusus bigint;
BEGIN
    SELECT count(*) INTO v_dupe_umum
      FROM (
          SELECT produk, tanggal_efektif
            FROM koperasi.harga_jual_customer
           WHERE anggota_koperasi IS NULL
           GROUP BY produk, tanggal_efektif
          HAVING count(*) > 1
      ) d;

    IF v_dupe_umum = 0 THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes
                        WHERE schemaname = 'koperasi'
                          AND indexname = 'uq_koperasi_harga_jual_umum') THEN
            CREATE UNIQUE INDEX uq_koperasi_harga_jual_umum
                ON koperasi.harga_jual_customer (produk, tanggal_efektif)
                WHERE anggota_koperasi IS NULL;
        END IF;
    ELSE
        RAISE NOTICE 'Index unik harga_jual_customer UMUM (produk, tanggal_efektif) DILEWATI: % kombinasi duplikat ditemukan. Bersihkan dulu (lihat AUDIT 2), lalu jalankan ulang skrip ini.',
            v_dupe_umum;
    END IF;

    SELECT count(*) INTO v_dupe_khusus
      FROM (
          SELECT anggota_koperasi, produk, tanggal_efektif
            FROM koperasi.harga_jual_customer
           WHERE anggota_koperasi IS NOT NULL
           GROUP BY anggota_koperasi, produk, tanggal_efektif
          HAVING count(*) > 1
      ) d;

    IF v_dupe_khusus = 0 THEN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes
                        WHERE schemaname = 'koperasi'
                          AND indexname = 'uq_koperasi_harga_jual_customer') THEN
            CREATE UNIQUE INDEX uq_koperasi_harga_jual_customer
                ON koperasi.harga_jual_customer (anggota_koperasi, produk, tanggal_efektif)
                WHERE anggota_koperasi IS NOT NULL;
        END IF;
    ELSE
        RAISE NOTICE 'Index unik harga_jual_customer KHUSUS (anggota_koperasi, produk, tanggal_efektif) DILEWATI: % kombinasi duplikat ditemukan. Bersihkan dulu (lihat AUDIT 3), lalu jalankan ulang skrip ini.',
            v_dupe_khusus;
    END IF;
END $$;

COMMIT;

-- ===================== AUDIT (jalankan terpisah, hanya-baca) =====================

-- AUDIT 1: duplikat harga_beli_supplier pada (supplier, produk, tanggal_efektif)
-- yang sama persis -- baris ber-id terbesar per grup adalah yang SAAT INI
-- dipilih resolusi harga transaksi; baris lain di grup itu adalah "kalah diam-diam".
SELECT supplier, produk, tanggal_efektif, count(*) AS jumlah_baris,
       array_agg(id ORDER BY id) AS id_baris, array_agg(aktif ORDER BY id) AS aktif_baris
  FROM koperasi.harga_beli_supplier
 GROUP BY supplier, produk, tanggal_efektif
HAVING count(*) > 1
 ORDER BY count(*) DESC;

-- AUDIT 2: duplikat harga_jual_customer UMUM (anggota_koperasi IS NULL) pada
-- (produk, tanggal_efektif) yang sama persis.
SELECT produk, tanggal_efektif, count(*) AS jumlah_baris,
       array_agg(id ORDER BY id) AS id_baris, array_agg(aktif ORDER BY id) AS aktif_baris
  FROM koperasi.harga_jual_customer
 WHERE anggota_koperasi IS NULL
 GROUP BY produk, tanggal_efektif
HAVING count(*) > 1
 ORDER BY count(*) DESC;

-- AUDIT 3: duplikat harga_jual_customer KHUSUS (anggota_koperasi tertentu)
-- pada (anggota_koperasi, produk, tanggal_efektif) yang sama persis.
SELECT anggota_koperasi, produk, tanggal_efektif, count(*) AS jumlah_baris,
       array_agg(id ORDER BY id) AS id_baris, array_agg(aktif ORDER BY id) AS aktif_baris
  FROM koperasi.harga_jual_customer
 WHERE anggota_koperasi IS NOT NULL
 GROUP BY anggota_koperasi, produk, tanggal_efektif
HAVING count(*) > 1
 ORDER BY count(*) DESC;
