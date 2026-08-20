-- Repository AIS modern post-Hibernate migration (PostgreSQL 9.6+).
-- IMPORTANT: every ADD/ALTER COLUMN is intentionally delegated to
-- hibernate.hbm2ddl.auto=update. Run this script only AFTER the first successful
-- application startup with the new entities/mappings.
-- This script is additive/idempotent and only performs backfill, supporting
-- table/index creation, and verification.
BEGIN;

UPDATE public.repo_item
SET workflow_status = CASE
    WHEN is_withdrawn IS TRUE THEN 'WITHDRAWN'
    WHEN sync_status IN ('SYNCED','PUBLISHED','APPROVED') THEN 'PUBLISHED'
    WHEN sync_status = 'FAILED' THEN 'DRAFT'
    ELSE coalesce(nullif(workflow_status,''),'DRAFT') END,
    owner_id = coalesce(owner_id, olehid),
    published_at = CASE WHEN sync_status IN ('SYNCED','PUBLISHED','APPROVED')
        THEN coalesce(published_at, issued_at, last_sync_at, submitted_at) ELSE published_at END,
    withdrawn_at = CASE WHEN is_withdrawn IS TRUE
        THEN coalesce(withdrawn_at, tanggal_dirubah) ELSE withdrawn_at END;


CREATE TABLE IF NOT EXISTS public.repo_workflow_event (
    id bigserial PRIMARY KEY,
    item_id bigint NOT NULL REFERENCES public.repo_item(id),
    from_status varchar(40),
    to_status varchar(40) NOT NULL,
    action varchar(40) NOT NULL,
    comment_text text,
    actor_id varchar(255) NOT NULL,
    actor_name varchar(500),
    request_id varchar(100),
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.repo_item_relation (
    id bigserial PRIMARY KEY,
    item_id bigint NOT NULL REFERENCES public.repo_item(id),
    related_item_id bigint NOT NULL REFERENCES public.repo_item(id),
    relation_type varchar(60) NOT NULL,
    actor_id varchar(255),
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    aktif boolean NOT NULL DEFAULT true,
    CONSTRAINT repo_item_relation_not_self CHECK (item_id <> related_item_id),
    CONSTRAINT repo_item_relation_unique UNIQUE (item_id, related_item_id, relation_type)
);

CREATE TABLE IF NOT EXISTS public.repo_usage_event (
    id bigserial PRIMARY KEY,
    item_id bigint NOT NULL REFERENCES public.repo_item(id),
    bitstream_id bigint REFERENCES public.repo_bitstream(id),
    event_type varchar(20) NOT NULL,
    visitor_hash varchar(64),
    actor_id varchar(255),
    user_agent_class varchar(40),
    occurred_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT repo_usage_event_type CHECK (event_type IN ('VIEW','DOWNLOAD'))
);

CREATE TABLE IF NOT EXISTS public.repo_notification (
    id bigserial PRIMARY KEY,
    item_id bigint NOT NULL REFERENCES public.repo_item(id),
    recipient_id varchar(255),
    recipient_role varchar(60),
    type varchar(40) NOT NULL,
    message varchar(1000) NOT NULL,
    read_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT repo_notification_recipient CHECK (recipient_id IS NOT NULL OR recipient_role IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS repo_item_public_idx
    ON public.repo_item (workflow_status, aktif, is_withdrawn, issued_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS repo_item_owner_status_idx
    ON public.repo_item (owner_id, workflow_status, tanggal_dirubah DESC);
CREATE INDEX IF NOT EXISTS repo_item_reviewer_status_idx
    ON public.repo_item (assigned_reviewer_id, workflow_status, submitted_at);
CREATE INDEX IF NOT EXISTS repo_item_collection_idx ON public.repo_item (collection_id);
CREATE INDEX IF NOT EXISTS repo_item_metadata_item_idx
    ON public.repo_item_metadata (item_id, aktif, metadata_field, place);
CREATE INDEX IF NOT EXISTS repo_bitstream_item_idx
    ON public.repo_bitstream (item_id, aktif, access_policy, primary_file);
CREATE INDEX IF NOT EXISTS repo_workflow_item_idx
    ON public.repo_workflow_event (item_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS repo_usage_item_idx
    ON public.repo_usage_event (item_id, occurred_at DESC, event_type);
CREATE INDEX IF NOT EXISTS repo_relation_item_idx
    ON public.repo_item_relation (item_id, aktif, relation_type);
CREATE INDEX IF NOT EXISTS repo_notification_recipient_idx
    ON public.repo_notification (recipient_id, recipient_role, read_at, created_at DESC);

-- PostgreSQL full-text discovery without requiring an extra extension.
DROP INDEX IF EXISTS public.repo_item_discovery_fts_idx;
CREATE INDEX repo_item_discovery_fts_idx ON public.repo_item USING gin
    (to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(authors,'') || ' '
        || coalesce(subjects,'') || ' ' || coalesce(abstract_text,'') || ' ' || coalesce(extracted_text,'')));

-- Fail visibly instead of leaving an apparently successful but incomplete release.
DO $repository_verify$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='repo_collection' AND column_name='deposit_enabled')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='repo_item' AND column_name='workflow_status')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='repo_bitstream' AND column_name='virus_scan_status')
       OR to_regclass('public.repo_workflow_event') IS NULL
       OR to_regclass('public.repo_usage_event') IS NULL
       OR to_regclass('public.repo_notification') IS NULL THEN
        RAISE EXCEPTION 'Migration repository modern v2 tidak lengkap; transaksi dibatalkan.';
    END IF;
END
$repository_verify$;

COMMIT;
