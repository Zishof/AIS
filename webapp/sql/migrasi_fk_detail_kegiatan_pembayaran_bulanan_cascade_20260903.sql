-- Memperbaiki kegagalan hapus PengaturanPembayaranBulanan yang masih mempunyai
-- DetailKegiatan turunan. Hanya FK detail_kegiatan yang diubah; FK transaksi
-- pembayaran/cicilan lain tetap menjadi pagar pelindung riwayat pembayaran.
--
-- Aman dijalankan ulang. Nama constraint lama dapat berbeda antar instalasi,
-- sehingga pencarian dilakukan berdasarkan tabel dan kolom, bukan nama hash
-- Hibernate (contoh lama: fk59bea98af381abdb).

BEGIN;

DO $migrasi$
DECLARE
    fk RECORD;
    nama_constraint_baru CONSTANT text :=
        'fk_detail_kegiatan_pengaturan_pembayaran_bulanan';
BEGIN
    IF to_regclass('public.detail_kegiatan') IS NULL THEN
        RAISE EXCEPTION 'Tabel public.detail_kegiatan tidak ditemukan';
    END IF;
    IF to_regclass('public.pengaturan_pembayaran_bulanan') IS NULL THEN
        RAISE EXCEPTION 'Tabel public.pengaturan_pembayaran_bulanan tidak ditemukan';
    END IF;

    -- Hapus seluruh FK satu-kolom lama untuk relasi yang sama. Ini mencakup
    -- nama constraint hasil generate Hibernate maupun nama stabil migrasi ini.
    FOR fk IN
        SELECT c.conname
          FROM pg_constraint c
          JOIN pg_class anak ON anak.oid = c.conrelid
          JOIN pg_namespace ns_anak ON ns_anak.oid = anak.relnamespace
          JOIN pg_class induk ON induk.oid = c.confrelid
          JOIN pg_namespace ns_induk ON ns_induk.oid = induk.relnamespace
          JOIN pg_attribute kolom
            ON kolom.attrelid = anak.oid
           AND kolom.attnum = ANY (c.conkey)
         WHERE c.contype = 'f'
           AND array_length(c.conkey, 1) = 1
           AND ns_anak.nspname = 'public'
           AND anak.relname = 'detail_kegiatan'
           AND kolom.attname = 'pengaturan_pembayaran_bulanan'
           AND ns_induk.nspname = 'public'
           AND induk.relname = 'pengaturan_pembayaran_bulanan'
    LOOP
        EXECUTE format(
            'ALTER TABLE public.detail_kegiatan DROP CONSTRAINT %I',
            fk.conname
        );
    END LOOP;

    EXECUTE format(
        'ALTER TABLE public.detail_kegiatan '
        || 'ADD CONSTRAINT %I FOREIGN KEY (pengaturan_pembayaran_bulanan) '
        || 'REFERENCES public.pengaturan_pembayaran_bulanan(id) '
        || 'ON UPDATE NO ACTION ON DELETE CASCADE',
        nama_constraint_baru
    );
END
$migrasi$;

COMMIT;

-- VERIFIKASI: harus menghasilkan tepat satu baris dengan delete_action = CASCADE.
SELECT c.conname AS constraint_name,
       pg_get_constraintdef(c.oid) AS definisi,
       CASE c.confdeltype
           WHEN 'c' THEN 'CASCADE'
           WHEN 'n' THEN 'SET NULL'
           WHEN 'r' THEN 'RESTRICT'
           WHEN 'a' THEN 'NO ACTION'
           WHEN 'd' THEN 'SET DEFAULT'
           ELSE c.confdeltype::text
       END AS delete_action
  FROM pg_constraint c
  JOIN pg_class anak ON anak.oid = c.conrelid
  JOIN pg_namespace ns_anak ON ns_anak.oid = anak.relnamespace
  JOIN pg_attribute kolom
    ON kolom.attrelid = anak.oid
   AND kolom.attnum = ANY (c.conkey)
 WHERE c.contype = 'f'
   AND array_length(c.conkey, 1) = 1
   AND ns_anak.nspname = 'public'
   AND anak.relname = 'detail_kegiatan'
   AND kolom.attname = 'pengaturan_pembayaran_bulanan';
