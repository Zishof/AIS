\set ON_ERROR_STOP on
DO $$
BEGIN
  IF current_database() <> 'ais_jurnal_sit' THEN
    RAISE EXCEPTION 'Cleanup performance hanya boleh pada ais_jurnal_sit';
  END IF;
END $$;
DROP SCHEMA IF EXISTS jurnal_perf CASCADE;
