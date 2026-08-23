-- =====================================================================================
-- Migrasi 20260819.001 — Baseline kolom "SchemaFix" + pelebaran kolom ke TEXT
-- =====================================================================================
-- TUJUAN
--   Menuliskan SECARA EKSPLISIT seluruh perubahan skema yang selama ini dijalankan
--   diam-diam oleh kelas-kelas ais.common.*SchemaFix saat startup aplikasi. Dengan ini
--   skema menjadi versioned, dapat ditinjau, dan dapat dijalankan terkontrol oleh DBA
--   pada maintenance window — prasyarat sebelum hbm2ddl.auto boleh diubah.
--
-- SIFAT
--   * IDEMPOTEN  — aman dijalankan berulang kali; tidak melakukan apa pun bila sudah sesuai.
--   * TOLERAN    — melewati tabel/skema yang tidak ada, sehingga instalasi yang TIDAK
--                  memasang modul tertentu (mis. SIRS, akunting) tidak gagal.
--   * TIDAK MERUSAK — hanya ADD COLUMN dan pelebaran tipe ke text. Tidak ada DROP,
--                  tidak ada perubahan data, tidak ada penyempitan tipe.
--
-- CARA JALAN MANUAL (opsional; aplikasi juga menjalankannya otomatis saat startup
-- melalui ais.common.RetailDatabaseMigrations):
--     psql -U <user> -d <database> -v ON_ERROR_STOP=1 -f 20260819.001-baseline-schema-fix.sql
--
-- ROLLBACK
--   Tidak diperlukan dan TIDAK disarankan: seluruh operasi bersifat menambah. Menghapus
--   kolom yang sudah dipakai entitas @Audited justru akan menggagalkan INSERT audit.
--   Bila benar-benar perlu, backup database SEBELUM menjalankan adalah jalur pemulihannya.
--
-- PRASYARAT: PostgreSQL 9.1+ (blok DO / PL/pgSQL).
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- BAGIAN 1 — Tambah kolom yang belum ada (tabel utama + tabel audit Envers)
-- -------------------------------------------------------------------------------------
-- Catatan: kolom audit (skema new_audit) WAJIB mengikuti tabel basisnya. Bila tertinggal,
-- INSERT audit gagal -> flush rollback -> DATA TIDAK TERSIMPAN. Inilah alasan kelas
-- *SchemaFix dibuat dahulu, dan sejak r77609 disinkronkan otomatis oleh AuditSchemaSyncUtil.
DO $mig1$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT * FROM (VALUES
        -- AturanCutiSchemaFix
        ('new_audit','statusabsensi__audit','durasi_baku_hari','integer'),
        ('new_audit','libur_nasional__audit','libur_panjang','boolean'),
        -- KursusSchemaFix
        ('public','produk_kursus','instruktur','bigint'),
        ('new_audit','produk_kursus__audit','instruktur','bigint'),
        ('public','produk_kursus','status','varchar(50)'),
        ('new_audit','produk_kursus__audit','status','varchar(50)'),
        ('public','produk_kursus','gratis','boolean'),
        ('new_audit','produk_kursus__audit','gratis','boolean'),
        ('public','peserta_punya_produk_kursus','hargadibayar','double precision'),
        ('new_audit','peserta_punya_produk_kursus__audit','hargadibayar','double precision'),
        ('public','peserta_punya_produk_kursus','kupon_kursus','bigint'),
        ('new_audit','peserta_punya_produk_kursus__audit','kupon_kursus','bigint'),
        -- SirsSchemaFix (skema "sirs" hanya ada bila modul SIRS terpasang)
        ('sirs','pasien','nik','varchar(20)'),
        ('new_audit','pasien__audit','nik','varchar(20)'),
        ('sirs','pasien','no_kartu_bpjs','varchar(25)'),
        ('new_audit','pasien__audit','no_kartu_bpjs','varchar(25)'),
        ('sirs','pasien','ihs_number','varchar(30)'),
        ('new_audit','pasien__audit','ihs_number','varchar(30)'),
        ('sirs','asuransi','jenis_payer','varchar(30)'),
        ('new_audit','asuransi__audit','jenis_payer','varchar(30)'),
        ('sirs','asuransi','kode_payer','varchar(30)'),
        ('new_audit','asuransi__audit','kode_payer','varchar(30)'),
        ('sirs','asuransi','nomor_pks','varchar(50)'),
        ('new_audit','asuransi__audit','nomor_pks','varchar(50)'),
        ('sirs','asuransi','aktif','boolean'),
        ('new_audit','asuransi__audit','aktif','boolean'),
        ('public','tbmuser','dokter','bigint'),
        ('new_audit','tbmuser__audit','dokter','bigint'),
        -- DiskonMahasiswaSchemaFix
        ('public','diskon_mahasiswa','jenis_diskon_mahasiswa','bigint'),
        ('new_audit','diskon_mahasiswa__audit','jenis_diskon_mahasiswa','bigint'),
        ('public','gelombang_pendaftaran','jenis_diskon_mahasiswa','bigint'),
        ('new_audit','gelombang_pendaftaran__audit','jenis_diskon_mahasiswa','bigint'),
        ('public','jenis_seleksi','jenis_diskon_mahasiswa','bigint'),
        ('new_audit','jenis_seleksi__audit','jenis_diskon_mahasiswa','bigint'),
        ('public','kelompok_mahasiswa','jenis_diskon_mahasiswa','bigint'),
        ('new_audit','kelompok_mahasiswa__audit','jenis_diskon_mahasiswa','bigint'),
        ('public','jenis_diskon_mahasiswa','tanggal_mulai_berlaku','date'),
        ('public','jenis_diskon_mahasiswa','tanggal_sampai_berlaku','date'),
        ('public','jenis_diskon_mahasiswa','berlaku_untuk_semua_mahasiswa','boolean'),
        ('public','jenis_diskon_mahasiswa','fakultas','bigint'),
        ('public','jenis_diskon_mahasiswa','jurusan','bigint'),
        ('public','jenis_diskon_mahasiswa','program','varchar(50)'),
        ('public','jenis_diskon_mahasiswa','status_awal_mahasiswa','bigint'),
        ('new_audit','jenis_diskon_mahasiswa__audit','tanggal_mulai_berlaku','date'),
        ('new_audit','jenis_diskon_mahasiswa__audit','tanggal_sampai_berlaku','date'),
        -- CATATAN: kolom berikut TIDAK ada pada DiskonMahasiswaSchemaFix versi lama
        -- (hanya ditambahkan ke tabel utama). Disertakan di sini karena entitas
        -- JenisDiskonMahasiswa ber-@Audited: tanpa kolom ini di tabel audit, INSERT audit
        -- gagal. AuditSchemaSyncUtil juga menambahkannya otomatis sejak r77609.
        ('new_audit','jenis_diskon_mahasiswa__audit','berlaku_untuk_semua_mahasiswa','boolean'),
        ('new_audit','jenis_diskon_mahasiswa__audit','fakultas','bigint'),
        ('new_audit','jenis_diskon_mahasiswa__audit','jurusan','bigint'),
        ('new_audit','jenis_diskon_mahasiswa__audit','program','varchar(50)'),
        ('new_audit','jenis_diskon_mahasiswa__audit','status_awal_mahasiswa','bigint'),
        -- KelompokCalonMahasiswaSchemaFix
        ('public','kelompok_calon_mahasiswa','jenis_seleksi_target','bigint'),
        ('new_audit','kelompok_calon_mahasiswa__audit','jenis_seleksi_target','bigint'),
        -- DatabaseTextColumnSchemaFix (kolom text baru)
        ('new_audit','tugas_pertemuan__audit','sub_cpmk_per_peserta','text'),
        -- NilaiKunciSchemaFix — snapshot nilai terkunci
        ('public','detailperkuliahan','total_nilai_kunci','double precision'),
        ('new_audit','detailperkuliahan__audit','total_nilai_kunci','double precision'),
        ('public','detailperkuliahan','nilai_huruf_kunci','varchar(2)'),
        ('new_audit','detailperkuliahan__audit','nilai_huruf_kunci','varchar(2)'),
        ('public','detailperkuliahan','nilai_ip_kunci','double precision'),
        ('new_audit','detailperkuliahan__audit','nilai_ip_kunci','double precision'),
        ('public','detailperkuliahan','lulus_kunci','boolean'),
        ('new_audit','detailperkuliahan__audit','lulus_kunci','boolean'),
        ('public','detailperkuliahan','total_nilai_sementara_kunci','double precision'),
        ('new_audit','detailperkuliahan__audit','total_nilai_sementara_kunci','double precision'),
        ('public','detailperkuliahan','nilai_huruf_sementara_kunci','varchar(2)'),
        ('new_audit','detailperkuliahan__audit','nilai_huruf_sementara_kunci','varchar(2)'),
        ('public','detailperkuliahan','nilai_ip_sementara_kunci','double precision'),
        ('new_audit','detailperkuliahan__audit','nilai_ip_sementara_kunci','double precision')
    ) AS t(sch, tbl, col, typ)
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = r.sch AND table_name = r.tbl)
           AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                           WHERE table_schema = r.sch AND table_name = r.tbl AND column_name = r.col)
        THEN
            EXECUTE format('ALTER TABLE %I.%I ADD COLUMN %I %s', r.sch, r.tbl, r.col, r.typ);
            RAISE NOTICE 'ADD COLUMN %.%.% %', r.sch, r.tbl, r.col, r.typ;
        END IF;
    END LOOP;
END
$mig1$;

-- -------------------------------------------------------------------------------------
-- BAGIAN 2 — Lebarkan kolom varchar lama menjadi TEXT
-- -------------------------------------------------------------------------------------
-- PENTING: hbm2ddl.auto=update TIDAK PERNAH mengubah tipe kolom yang SUDAH ADA. Anotasi
-- columnDefinition="text" di Java hanya berlaku saat kolom PERTAMA KALI dibuat. Tabel lama
-- (mis. dibuat 2010) karena itu bisa masih varchar(255) walau Java sudah menyatakan text,
-- sehingga INSERT gagal "value too long for type character varying(255)".
-- Inilah bagian yang TIDAK BISA didelegasikan ke hibernate.cfg.xml.
DO $mig2$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT * FROM (VALUES
        -- virtual_account_bank
        ('public','virtual_account_bank','keterangan'),
        ('public','virtual_account_bank','cicilan'),
        ('public','virtual_account_bank','detailbiaya'),
        ('public','virtual_account_bank','bulanan'),
        ('public','virtual_account_bank','request'),
        ('public','virtual_account_bank','response'),
        ('public','virtual_account_bank','link'),
        ('public','virtual_account_bank','notif'),
        ('public','virtual_account_bank','barcode'),
        -- log_host_to_host
        ('public','log_host_to_host','oleh'),
        ('public','log_host_to_host','olehid'),
        ('public','log_host_to_host','ip'),
        ('public','log_host_to_host','nama'),
        ('public','log_host_to_host','keterangan'),
        ('public','log_host_to_host','response_description'),
        ('public','log_host_to_host','request'),
        ('public','log_host_to_host','response'),
        ('public','log_host_to_host','item'),
        ('public','log_host_to_host','stack_trace'),
        ('public','log_host_to_host','info0'),
        ('public','log_host_to_host','info1'),
        ('public','log_host_to_host','info2'),
        ('public','log_host_to_host','info3'),
        ('public','log_host_to_host','info4'),
        ('public','log_host_to_host','info5'),
        ('public','log_host_to_host','info6'),
        ('public','log_host_to_host','info7'),
        ('public','log_host_to_host','info8'),
        ('public','log_host_to_host','info9'),
        ('public','log_host_to_host','info10'),
        ('public','log_host_to_host','info11'),
        ('public','log_host_to_host','info12'),
        ('public','log_host_to_host','info13'),
        ('public','log_host_to_host','info14'),
        ('public','log_host_to_host','info15'),
        ('public','log_host_to_host','info16'),
        ('public','log_host_to_host','info17'),
        ('public','log_host_to_host','info18'),
        -- akunting.transaksi + audit
        ('akunting','transaksi','keterangan'),
        ('new_audit','transaksi__audit','keterangan'),
        -- konfigurasi + audit
        ('public','konfigurasi','keterangan'),
        ('public','konfigurasi','nilai'),
        ('public','konfigurasi','nilaidikunci'),
        ('new_audit','konfigurasi__audit','keterangan'),
        ('new_audit','konfigurasi__audit','nilai'),
        ('new_audit','konfigurasi__audit','nilaidikunci')
    ) AS t(sch, tbl, col)
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = r.sch AND table_name = r.tbl
                     AND column_name = r.col AND data_type <> 'text')
        THEN
            EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN %I TYPE text', r.sch, r.tbl, r.col);
            RAISE NOTICE 'ALTER TYPE text %.%.%', r.sch, r.tbl, r.col;
        END IF;
    END LOOP;
END
$mig2$;
