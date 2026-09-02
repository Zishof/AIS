-- Menyamakan status tersimpan dengan status yang selama ini ditampilkan oleh
-- PenghargaanMahasiswa.getStatus(). Aman dijalankan berulang.

BEGIN;

UPDATE penghargaan_mahasiswa
   SET status = 'Belum diproses'
 WHERE status IS NULL
    OR btrim(status) = '';

ALTER TABLE penghargaan_mahasiswa
    ALTER COLUMN status SET DEFAULT 'Belum diproses';

CREATE INDEX IF NOT EXISTS idx_penghargaan_mahasiswa_status_mahasiswa
    ON penghargaan_mahasiswa (status, mahasiswa);

COMMIT;

-- AUDIT 1: daftar pengajuan yang masih menunggu tindakan verifikator.
SELECT pm.id,
       m.nim,
       m.nama,
       pm.nama AS nama_karya,
       pm.status
  FROM penghargaan_mahasiswa pm
  JOIN mahasiswa m ON m.id = pm.mahasiswa
 WHERE pm.status IN ('Belum diproses', 'Sedang diproses')
 ORDER BY pm.id DESC;

-- AUDIT 2: hanya hasil ini yang memang harus dilengkapi homebase prodi sebelum
-- dapat diekspor ke repository.
SELECT pm.id,
       m.nim,
       m.nama,
       pm.nama AS nama_karya
  FROM penghargaan_mahasiswa pm
  JOIN mahasiswa m ON m.id = pm.mahasiswa
  LEFT JOIN jurusan j ON j.id = m.jurusan
 WHERE pm.status = 'Disetujui'
   AND j.id IS NULL
 ORDER BY pm.id DESC;
