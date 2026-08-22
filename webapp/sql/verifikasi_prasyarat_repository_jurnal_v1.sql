\set ON_ERROR_STOP on
BEGIN READ ONLY;
DO $$
DECLARE missing text;
BEGIN
 SELECT string_agg(v.table_name||'.'||v.column_name,', ') INTO missing
 FROM (VALUES
  ('repo_collection','tenant_key'),('repo_item','tenant_key'),('repo_item','featured'),('repo_item','doi_state'),
  ('repo_usage_event','country_code'),('repo_usage_event','referrer_host'),('repo_workflow_event','round_number'),
  ('repo_item_relation','sort_order'),('repo_bitstream','journal_stage'),('repo_bitstream','content_ref'),
  ('diskusi','jurnal_penelitian_id'),('diskusi','repo_item_id'),('diskusi','anonymity_mode'),
  ('repo_author_authority','user_ref_id'),('repo_author_authority','mahasiswa_ref_id'),
  ('notifikasi','jurnal_idempotency_key'),('notifikasi','jurnal_snapshot_json')) v(table_name,column_name)
 WHERE NOT EXISTS(SELECT 1 FROM information_schema.columns c WHERE c.table_schema='public' AND c.table_name=v.table_name AND c.column_name=v.column_name);
 IF missing IS NOT NULL THEN RAISE EXCEPTION 'Missing prerequisite columns: %',missing; END IF;
 SELECT string_agg(v.table_name||'.'||v.column_name,', ') INTO missing
 FROM (VALUES
  ('repo_collection__audit','tenant_key'),
  ('repo_item__audit','tenant_key'),('repo_item__audit','featured'),('repo_item__audit','featured_at'),
  ('repo_item__audit','doi_state'),('repo_item__audit','doi_updated_at')) v(table_name,column_name)
 WHERE to_regclass('new_audit.'||v.table_name) IS NOT NULL
   AND NOT EXISTS(SELECT 1 FROM information_schema.columns c WHERE c.table_schema='new_audit' AND c.table_name=v.table_name AND c.column_name=v.column_name);
 IF missing IS NOT NULL THEN RAISE EXCEPTION 'Missing Envers prerequisite columns: %',missing; END IF;
 IF NOT EXISTS (
   SELECT 1 FROM pg_indexes
   WHERE schemaname='public' AND indexname='uq_notifikasi_jurnal_idempotency'
     AND indexdef LIKE '%(jurnal_penelitian_id, jurnal_idempotency_key)%'
 ) THEN RAISE EXCEPTION 'Journal-scoped notification idempotency index missing or invalid'; END IF;
 IF to_regclass('public.repo_author_authority') IS NULL OR to_regclass('public.repo_item_contributor') IS NULL
    OR to_regclass('public.repo_user_preference') IS NULL OR to_regclass('public.repo_integration_event') IS NULL
    OR to_regclass('public.repo_workflow_event') IS NULL OR to_regclass('public.repo_item_relation') IS NULL
    OR to_regclass('public.repo_usage_event') IS NULL OR to_regclass('public.repo_notification') IS NULL
 THEN RAISE EXCEPTION 'Repository prerequisite table missing'; END IF;
END $$;
SELECT table_name,count(*) AS columns
FROM information_schema.columns
WHERE table_schema='public' AND table_name IN
 ('repo_collection','repo_item','repo_workflow_event','repo_item_relation','repo_usage_event','repo_notification',
  'repo_author_authority','repo_item_contributor','repo_user_preference','repo_integration_event')
GROUP BY table_name ORDER BY table_name;
SELECT to_regclass('public.repo_author_authority') IS NOT NULL AS authority,
       to_regclass('public.repo_item_contributor') IS NOT NULL AS contributor,
       to_regclass('public.repo_user_preference') IS NOT NULL AS preference,
       to_regclass('public.repo_integration_event') IS NOT NULL AS integration_event;
ROLLBACK;
