-- Repository AIS modern V2 tidak memakai migrasi DDL/DML manual.
-- Tabel, kolom, constraint, dan pembaruan skema dikelola oleh mapping Hibernate
-- saat aplikasi server dijalankan dengan hibernate.hbm2ddl.auto=update.
--
-- File ini dipertahankan sebagai penanda kompatibilitas untuk prosedur deployment
-- lama. Tidak ada perintah mutasi database di dalamnya. Setelah startup server,
-- gunakan verifikasi_repository_modern_v2.sql yang bersifat read-only.
SELECT 'Repository schema is managed by Hibernate; no manual migration executed.' AS info;
