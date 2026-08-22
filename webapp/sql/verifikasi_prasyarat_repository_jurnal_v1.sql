\set ON_ERROR_STOP on
BEGIN READ ONLY;
SELECT table_name,count(*) AS columns
FROM information_schema.columns
WHERE table_schema='public' AND table_name IN
 ('repo_collection','repo_item','repo_usage_event','repo_author_authority','repo_item_contributor','repo_user_preference','repo_integration_event')
GROUP BY table_name ORDER BY table_name;
SELECT to_regclass('public.repo_author_authority') IS NOT NULL AS authority,
       to_regclass('public.repo_item_contributor') IS NOT NULL AS contributor,
       to_regclass('public.repo_user_preference') IS NOT NULL AS preference,
       to_regclass('public.repo_integration_event') IS NOT NULL AS integration_event;
ROLLBACK;
