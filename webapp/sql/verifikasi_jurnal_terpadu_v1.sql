\set ON_ERROR_STOP on
BEGIN READ ONLY;
DO $$
DECLARE n integer;
BEGIN
 SELECT count(*) INTO n FROM (VALUES
  ('template_email_jurnal'),('langganan_jurnal'),('undangan_peran_jurnal'),('peserta_diskusi_jurnal'),
  ('penugasan_tahap_jurnal'),('penugasan_reviewer_jurnal'),('agregat_penggunaan_jurnal'),
  ('rentang_ip_langganan_jurnal'),('import_sumber_ojs'),('import_job_ojs'),
  ('import_checkpoint_ojs'),('import_mapping_ojs')) x(name)
 WHERE to_regclass('penelitiandanpengabdian.'||x.name) IS NOT NULL;
 IF n<>12 THEN RAISE EXCEPTION 'Expected 12 journal tables, found %',n; END IF;
 IF EXISTS(SELECT 1 FROM (VALUES ('tipe_langganan_jurnal'),('putaran_review_jurnal'),('form_review_jurnal'),('elemen_form_review_jurnal'),('jawaban_review_jurnal'),('lampiran_jurnal')) f(name)
           WHERE to_regclass('penelitiandanpengabdian.'||f.name) IS NOT NULL)
 THEN RAISE EXCEPTION 'Forbidden redundant journal table exists'; END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='tbmrole' AND column_name='jurnal_akses_json')
 THEN RAISE EXCEPTION 'tbmrole.jurnal_akses_json is missing'; END IF;
END $$;
WITH expected(name) AS (VALUES
 ('template_email_jurnal'),('langganan_jurnal'),('undangan_peran_jurnal'),('peserta_diskusi_jurnal'),
 ('penugasan_tahap_jurnal'),('penugasan_reviewer_jurnal'),('agregat_penggunaan_jurnal'),
 ('rentang_ip_langganan_jurnal'),('import_sumber_ojs'),('import_job_ojs'),
 ('import_checkpoint_ojs'),('import_mapping_ojs'))
SELECT count(*) AS expected_count,
       count(to_regclass('penelitiandanpengabdian.'||name)) AS physical_count
FROM expected;

SELECT EXISTS(SELECT 1 FROM information_schema.columns
 WHERE table_schema='public' AND table_name='tbmrole' AND column_name='jurnal_akses_json') AS tbmrole_column;

SELECT table_name,count(*) AS columns
FROM information_schema.columns
WHERE table_schema='penelitiandanpengabdian' AND table_name IN
 ('template_email_jurnal','langganan_jurnal','undangan_peran_jurnal','peserta_diskusi_jurnal',
  'penugasan_tahap_jurnal','penugasan_reviewer_jurnal','agregat_penggunaan_jurnal',
  'rentang_ip_langganan_jurnal','import_sumber_ojs','import_job_ojs','import_checkpoint_ojs','import_mapping_ojs')
GROUP BY table_name ORDER BY table_name;
ROLLBACK;
