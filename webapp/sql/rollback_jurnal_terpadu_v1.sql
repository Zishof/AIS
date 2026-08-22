-- Rollback destruktif TIDAK dijalankan otomatis. Gunakan hanya pada clone SIT kosong.
-- Pada produksi, rollback aplikasi mempertahankan tabel/kolom agar data tidak hilang;
-- recovery penuh memakai dump custom terverifikasi sebelum migrasi.
SELECT 'No destructive journal rollback executed; restore the verified pre-migration dump when required.' AS info;
