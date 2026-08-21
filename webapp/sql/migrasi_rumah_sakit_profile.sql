-- Profil tenant fasilitas kesehatan untuk website publik dan branding eMedic.
-- RumahSakit mencakup RS, Puskesmas, Posyandu, Klinik, praktik mandiri,
-- laboratorium, apotek, dan fasilitas kesehatan lainnya.
CREATE SCHEMA IF NOT EXISTS sirs;

CREATE TABLE IF NOT EXISTS sirs.rumah_sakit (
    id bigserial PRIMARY KEY,
    kode varchar(50),
    jenis_fasilitas varchar(40) NOT NULL DEFAULT 'RUMAH_SAKIT',
    nama varchar(180) NOT NULL,
    nama_singkat varchar(80),
    alamat varchar(300),
    telepon varchar(80),
    whatsapp varchar(80),
    email varchar(255),
    website varchar(200),
    domain varchar(500) UNIQUE,
    motto varchar(300),
    deskripsi text,
    nomor_izin_operasional varchar(100),
    css varchar(150),
    warna varchar(20),
    pilihan_tampilan varchar(30) DEFAULT 'baru',
    aktif boolean DEFAULT true,
    dikunci varchar(50),
    oleh varchar(255),
    oleh_id varchar(255),
    tanggal_dirubah timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rumah_sakit_aktif
    ON sirs.rumah_sakit (aktif);

COMMENT ON COLUMN sirs.rumah_sakit.domain IS
    'Satu atau beberapa domain, dipisahkan koma, titik koma, atau spasi.';
