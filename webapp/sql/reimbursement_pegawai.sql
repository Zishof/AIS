-- Opsional untuk instalasi yang menonaktifkan hbm2ddl.auto=update.
-- Aman dijalankan berulang kali pada PostgreSQL.
CREATE SCHEMA IF NOT EXISTS akunting;

CREATE TABLE IF NOT EXISTS akunting.reimbursement_pegawai (
    id bigserial PRIMARY KEY,
    kode varchar(80) NOT NULL UNIQUE,
    deskripsi text NOT NULL,
    kategori varchar(100) NOT NULL,
    nominal double precision NOT NULL,
    pajak_persen double precision DEFAULT 0,
    dibayar_pegawai boolean DEFAULT false,
    tanggal_pengeluaran date NOT NULL,
    tanggal_pengajuan timestamp NOT NULL,
    pegawai bigint NOT NULL REFERENCES public.pegawai(id),
    atasan bigint NOT NULL REFERENCES public.pegawai(id),
    dibuat_oleh bigint NOT NULL REFERENCES public.tbmuser(id),
    catatan_pengaju text,
    lampiran_id bigint,
    status varchar(30) NOT NULL,
    catatan_atasan text,
    diputuskan_oleh bigint REFERENCES public.tbmuser(id),
    tanggal_keputusan timestamp,
    tanggal_akuntansi date,
    akun_biaya bigint REFERENCES akunting.akun(id),
    posting_pengeluaran bigint REFERENCES akunting.posting_history(id),
    metode_pembayaran varchar(20),
    bank_penerima varchar(150),
    rekening_penerima varchar(100),
    tanggal_pembayaran date,
    catatan_pembayaran text,
    akun_pembayaran bigint REFERENCES akunting.akun(id),
    dibayar_oleh bigint REFERENCES public.tbmuser(id),
    posting_pembayaran bigint REFERENCES akunting.posting_history(id),
    tanggal_dirubah timestamp
);

CREATE INDEX IF NOT EXISTS reimbursement_pegawai_dibuat_idx
    ON akunting.reimbursement_pegawai (dibuat_oleh, tanggal_pengajuan DESC);
CREATE INDEX IF NOT EXISTS reimbursement_pegawai_atasan_idx
    ON akunting.reimbursement_pegawai (atasan, status, tanggal_pengajuan);
CREATE INDEX IF NOT EXISTS reimbursement_pegawai_finance_idx
    ON akunting.reimbursement_pegawai (status, tanggal_keputusan);

-- Wajib diisi melalui menu Konfigurasi sebelum approval pertama:
-- key   : akun_hutang_reimbursement_pegawai
-- nilai : ID atau kode akun kewajiban/utang reimbursement pegawai.
-- Opsional: kategori_reimbursement_pegawai = daftar kategori dipisahkan koma.
-- Daftar pajak otomatis memakai master asset.jenis_pajak_barang yang aktif.
