-- Tidak ada rollback DDL manual untuk Repository AIS modern V2.
-- Skema dikelola Hibernate dan data repository tidak boleh dihapus oleh skrip.
-- Rollback aplikasi dilakukan dengan mengembalikan source/WAR versi sebelumnya;
-- kolom/tabel tambahan dibiarkan tetap ada agar rollback tidak merusak data.
SELECT 'No destructive database rollback executed.' AS info;
