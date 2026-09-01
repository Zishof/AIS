-- Menyimpan daftar semester yang secara administratif harus tetap berstatus aktif.
-- Kolom audit wajib tersedia karena public.mahasiswa diaudit oleh Hibernate Envers.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'mahasiswa'
          AND column_name = 'paksa_aktif_semester'
    ) THEN
        ALTER TABLE public.mahasiswa
            ADD COLUMN paksa_aktif_semester varchar(255);
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'new_audit'
          AND table_name = 'mahasiswa__audit'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'new_audit'
          AND table_name = 'mahasiswa__audit'
          AND column_name = 'paksa_aktif_semester'
    ) THEN
        ALTER TABLE new_audit.mahasiswa__audit
            ADD COLUMN paksa_aktif_semester varchar(255);
    END IF;
END
$$;
