\set ON_ERROR_STOP on
BEGIN READ ONLY;
DO $$
DECLARE column_count integer; unique_count integer;
BEGIN
 IF to_regclass('public.lampiran_jurnal') IS NULL THEN
  RAISE EXCEPTION 'public.lampiran_jurnal missing';
 END IF;
 SELECT count(*) INTO column_count FROM information_schema.columns
  WHERE table_schema='public' AND table_name='lampiran_jurnal';
 IF column_count <> 19 THEN RAISE EXCEPTION 'lampiran_jurnal expected 19 columns, found %',column_count; END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='lampiran_jurnal' AND column_name='file_content' AND data_type='oid')
  THEN RAISE EXCEPTION 'lampiran_jurnal.file_content must be oid'; END IF;
 SELECT count(*) INTO unique_count FROM pg_constraint
  WHERE conrelid='public.lampiran_jurnal'::regclass AND contype='u';
 IF unique_count <> 2 THEN RAISE EXCEPTION 'lampiran_jurnal expected two unique constraints, found %',unique_count; END IF;
END $$;
SELECT current_database() AS database_name,
       (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='lampiran_jurnal') AS columns,
       (SELECT count(*) FROM pg_constraint WHERE conrelid='public.lampiran_jurnal'::regclass AND contype='u') AS unique_constraints,
       (SELECT count(*) FROM public.lampiran_jurnal) AS rows;
ROLLBACK;
