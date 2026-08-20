-- ============================================================================
-- Migrasi WAJIB sebelum deploy fitur "Penyesuaian Saldo" (opname saldo voucher
-- member) pada layar Pelanggan > Saldo Voucher.
--
-- Sejak hbm2ddl.auto diubah menjadi "none" (lihat hibernate.cfg.xml), Hibernate
-- TIDAK lagi membuat tabel/kolom baru saat server start. Tabel di bawah karena
-- itu harus dibuat manual; tanpanya, seluruh aksi penyesuaian_saldo_* gagal
-- dengan "relasi koperasi.penyesuaian_saldo_anggota tidak ada".
--
-- Entitas @Audited, sehingga tabel audit new_audit.*__audit juga wajib dibuat.
-- Bila tidak, SETIAP penyimpanan penyesuaian gagal menulis baris audit dan
-- transaksinya di-rollback (gotcha yang sama seperti migrasi audit lainnya di
-- folder ini).
--
-- Jalankan SEKALI di database produksi SEBELUM Tomcat di-restart dengan build
-- yang memuat fitur ini:
--   psql -d <database_ais> -f migrasi_penyesuaian_saldo_anggota.sql
--
-- Padanan entitas: ais.database.model.koperasi.PenyesuaianSaldoAnggota
-- Bentuk kolom mengikuti pola koperasi.stok_opname (opname barang), karena
-- fitur ini memang padanan opname untuk saldo.
-- ============================================================================

CREATE TABLE IF NOT EXISTS koperasi.penyesuaian_saldo_anggota (
    id               bigserial                   NOT NULL,
    anggota_koperasi bigint,
    saldo_sistem     double precision,
    saldo_fisik      double precision,
    selisih          double precision,
    waktu            timestamp without time zone,
    keterangan       text,
    deposit          bigint,
    oleh             character varying(255),
    olehid           character varying(255),
    tanggal_dirubah  timestamp without time zone,
    CONSTRAINT penyesuaian_saldo_anggota_pkey PRIMARY KEY (id)
);

-- Relasi dibuat sebagai FK opsional supaya baris lama/terhapus tidak menghalangi
-- pencatatan; jejak balik ke baris Deposit koreksi tetap ada di kolom "deposit".
ALTER TABLE koperasi.penyesuaian_saldo_anggota
    DROP CONSTRAINT IF EXISTS fk_psa_anggota_koperasi;
ALTER TABLE koperasi.penyesuaian_saldo_anggota
    ADD CONSTRAINT fk_psa_anggota_koperasi
    FOREIGN KEY (anggota_koperasi) REFERENCES koperasi.anggota_koperasi (id);

ALTER TABLE koperasi.penyesuaian_saldo_anggota
    DROP CONSTRAINT IF EXISTS fk_psa_deposit;
ALTER TABLE koperasi.penyesuaian_saldo_anggota
    ADD CONSTRAINT fk_psa_deposit
    FOREIGN KEY (deposit) REFERENCES public.deposit (id);

-- Riwayat per anggota adalah pola baca utamanya (dialog membuka riwayat terakhir).
CREATE INDEX IF NOT EXISTS idx_psa_anggota_waktu
    ON koperasi.penyesuaian_saldo_anggota (anggota_koperasi, waktu DESC);

-- ---------------------------------------------------------------------------
-- Tabel audit Envers. Kolomnya = kolom tabel utama + rev/revtype, tanpa NOT NULL
-- pada kolom relasi (baris audit boleh menyimpan keadaan sebelum relasi terisi).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS new_audit.penyesuaian_saldo_anggota__audit (
    id               bigint    NOT NULL,
    rev              integer   NOT NULL,
    revtype          smallint,
    anggota_koperasi bigint,
    saldo_sistem     double precision,
    saldo_fisik      double precision,
    selisih          double precision,
    waktu            timestamp without time zone,
    keterangan       text,
    deposit          bigint,
    oleh             character varying(255),
    olehid           character varying(255),
    tanggal_dirubah  timestamp without time zone,
    CONSTRAINT penyesuaian_saldo_anggota__audit_pkey PRIMARY KEY (id, rev)
);
