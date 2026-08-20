-- Rollback for objects created by migrasi_repository_modern_v2.sql.
-- Columns managed by Hibernate are intentionally retained; no ALTER TABLE is
-- executed here. Back up event/relation/usage/notification data first.
BEGIN;
DROP TABLE IF EXISTS public.repo_notification;
DROP TABLE IF EXISTS public.repo_usage_event;
DROP TABLE IF EXISTS public.repo_item_relation;
DROP TABLE IF EXISTS public.repo_workflow_event;
COMMIT;
