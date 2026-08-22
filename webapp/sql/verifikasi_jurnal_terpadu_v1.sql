\set ON_ERROR_STOP on
BEGIN READ ONLY;
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
