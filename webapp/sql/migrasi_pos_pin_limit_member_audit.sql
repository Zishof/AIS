-- Pra-deploy POS: PIN member, batas transaksi periodik, dan hak persetujuan.
--
-- hbm2ddl.auto=update membuat/menambah tabel serta kolom utama. Envers pada
-- versi Hibernate AIS tidak selalu menambah kolom baru ke tabel audit yang
-- sudah ada, sehingga ALTER berikut wajib dijalankan sekali sebelum Tomcat
-- memakai build baru. Semua perintah idempoten dan aman dijalankan ulang.

ALTER TABLE IF EXISTS new_audit.jenis_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_pin boolean;
ALTER TABLE IF EXISTS new_audit.jenis_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_verifikasi_biometric_wajah boolean;
ALTER TABLE IF EXISTS new_audit.jenis_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_verifikasi_biometric_fingerprint boolean;

ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS daftar_cara_pembayaran_yang_boleh_di_pilih text;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS cara_pembayaran_default_id int8;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS tidak_boleh_cara_pembayaran_lain boolean;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS maksimal_transaksi_harian float8;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS maksimal_transaksi_mingguan float8;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS maksimal_transaksi_bulanan float8;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_pin boolean;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_verifikasi_biometric_wajah boolean;
ALTER TABLE IF EXISTS new_audit.tipe_anggota_koperasi__audit
    ADD COLUMN IF NOT EXISTS wajib_verifikasi_biometric_fingerprint boolean;

ALTER TABLE IF EXISTS new_audit.tbmrole__audit
    ADD COLUMN IF NOT EXISTS boleh_verifikasi_member_melebihi_limit boolean;
