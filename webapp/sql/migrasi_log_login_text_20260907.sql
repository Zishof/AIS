-- Jalankan pada database utama, bukan database streaming.
-- Perlu ALTER lock singkat; jalankan pada jendela pemeliharaan.
-- Tidak memotong teks audit yang sudah tersimpan.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.log_login
    ALTER COLUMN nama TYPE text,
    ALTER COLUMN keterangan TYPE text,
    ALTER COLUMN description TYPE text,
    ALTER COLUMN ip TYPE text,
    ALTER COLUMN hostname TYPE text,
    ALTER COLUMN sessionid TYPE text,
    ALTER COLUMN oleh TYPE text;
-- Nama fisik olehId mengikuti naming strategy instalasi.
DO $$
DECLARE c record;
BEGIN
    FOR c IN SELECT column_name FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'log_login'
          AND column_name IN ('olehid', 'oleh_id', 'olehId')
    LOOP
        EXECUTE 'ALTER TABLE public.log_login ALTER COLUMN '
            || quote_ident(c.column_name) || ' TYPE text';
    END LOOP;
END $$;
COMMIT;
