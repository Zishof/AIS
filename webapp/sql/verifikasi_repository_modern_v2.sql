-- Read-only verification after deploying Repository AIS modern.
-- Expected result: zero rows in both queries.

WITH expected(table_name, column_name) AS (VALUES
 ('repo_collection','metadata_profile_json'),('repo_collection','workflow_profile_json'),
 ('repo_collection','access_policy_json'),('repo_collection','default_license_uri'),
 ('repo_collection','deposit_enabled'),('repo_item','lock_version'),
 ('repo_item','workflow_status'),('repo_item','owner_id'),('repo_item','license_uri'),
 ('repo_item','embargo_until'),('repo_item','published_at'),('repo_item','withdrawn_at'),
 ('repo_item','doi'),('repo_item','version_number'),('repo_item','view_count'),
 ('repo_item','download_count'),('repo_item','extracted_text'),
 ('repo_bitstream','virus_scan_status'),('repo_bitstream','virus_scanned_at'),
 ('repo_bitstream','signature_valid'),('repo_bitstream','file_version')
)
SELECT e.table_name, e.column_name AS missing_column
FROM expected e
LEFT JOIN information_schema.columns c ON c.table_schema='public'
 AND c.table_name=e.table_name AND c.column_name=e.column_name
WHERE c.column_name IS NULL
ORDER BY e.table_name,e.column_name;

WITH expected(table_name) AS (VALUES
 ('repo_workflow_event'),('repo_item_relation'),('repo_usage_event'),('repo_notification')
)
SELECT e.table_name AS missing_table
FROM expected e
WHERE to_regclass('public.'||e.table_name) IS NULL
ORDER BY e.table_name;
