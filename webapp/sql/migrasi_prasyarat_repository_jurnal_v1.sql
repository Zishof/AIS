-- Prasyarat Repository yang sudah dipetakan di hibernate.cfg.xml tetapi belum fisik
-- pada baseline deployment. Tabel ini bukan bagian dari budget 12 tabel domain jurnal.
BEGIN;

-- Baseline lama hanya mempunyai repo_collection/repo_item/repo_item_metadata/
-- repo_bitstream. Empat tabel repository modern berikut sebelumnya bergantung
-- pada hbm2ddl.auto=update, sedangkan deployment jurnal wajib memakai
-- hbm2ddl.auto=none. Bootstrap eksplisit ini membuat migration dapat dijalankan
-- pada baseline lama maupun baseline yang tabelnya sudah tersedia.
CREATE TABLE IF NOT EXISTS public.repo_workflow_event (
 id bigserial PRIMARY KEY, item_id bigint NOT NULL, from_status varchar(40),
 to_status varchar(40) NOT NULL, action varchar(40) NOT NULL, comment_text text,
 actor_id varchar(255) NOT NULL, actor_name varchar(500), request_id varchar(100),
 created_at timestamp NOT NULL, round_number integer
);

CREATE TABLE IF NOT EXISTS public.repo_item_relation (
 id bigserial PRIMARY KEY, item_id bigint NOT NULL, related_item_id bigint NOT NULL,
 relation_type varchar(60) NOT NULL, actor_id varchar(255),
 sort_order integer NOT NULL DEFAULT 0, created_at timestamp NOT NULL,
 aktif boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.repo_usage_event (
 id bigserial PRIMARY KEY, item_id bigint NOT NULL, bitstream_id bigint,
 event_type varchar(20) NOT NULL, visitor_hash varchar(64), actor_id varchar(255),
 user_agent_class varchar(40), country_code varchar(3), referrer_host varchar(255),
 occurred_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS public.repo_notification (
 id bigserial PRIMARY KEY, item_id bigint NOT NULL, recipient_id varchar(255),
 recipient_role varchar(60), type varchar(40) NOT NULL,
 message varchar(1000) NOT NULL, read_at timestamp, created_at timestamp NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_repo_workflow_item_created
 ON public.repo_workflow_event(item_id,created_at);
CREATE INDEX IF NOT EXISTS ix_repo_relation_related
 ON public.repo_item_relation(related_item_id,relation_type);
CREATE INDEX IF NOT EXISTS ix_repo_usage_item_occurred
 ON public.repo_usage_event(item_id,occurred_at);
CREATE INDEX IF NOT EXISTS ix_repo_notification_recipient
 ON public.repo_notification(recipient_id,read_at,created_at);

ALTER TABLE public.repo_collection ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
UPDATE public.repo_collection SET tenant_key='default' WHERE tenant_key IS NULL;
ALTER TABLE public.repo_collection ALTER COLUMN tenant_key SET NOT NULL;

ALTER TABLE public.repo_item ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
ALTER TABLE public.repo_item ADD COLUMN IF NOT EXISTS featured boolean;
ALTER TABLE public.repo_item ADD COLUMN IF NOT EXISTS featured_at timestamp;
ALTER TABLE public.repo_item ADD COLUMN IF NOT EXISTS doi_state varchar(30);
ALTER TABLE public.repo_item ADD COLUMN IF NOT EXISTS doi_updated_at timestamp;
UPDATE public.repo_item SET tenant_key='default' WHERE tenant_key IS NULL;
ALTER TABLE public.repo_item ALTER COLUMN tenant_key SET NOT NULL;

-- RepoCollection/RepoItem are Envers audited. Every new mapped column must
-- exist on the audit table before a committed transaction can complete.
ALTER TABLE IF EXISTS new_audit.repo_collection__audit ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
ALTER TABLE IF EXISTS new_audit.repo_item__audit ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
ALTER TABLE IF EXISTS new_audit.repo_item__audit ADD COLUMN IF NOT EXISTS featured boolean;
ALTER TABLE IF EXISTS new_audit.repo_item__audit ADD COLUMN IF NOT EXISTS featured_at timestamp;
ALTER TABLE IF EXISTS new_audit.repo_item__audit ADD COLUMN IF NOT EXISTS doi_state varchar(30);
ALTER TABLE IF EXISTS new_audit.repo_item__audit ADD COLUMN IF NOT EXISTS doi_updated_at timestamp;

ALTER TABLE public.repo_usage_event ADD COLUMN IF NOT EXISTS country_code varchar(3);
ALTER TABLE public.repo_usage_event ADD COLUMN IF NOT EXISTS referrer_host varchar(255);

ALTER TABLE public.repo_workflow_event ADD COLUMN IF NOT EXISTS round_number integer;
ALTER TABLE public.repo_item_relation ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;
ALTER TABLE public.repo_bitstream ADD COLUMN IF NOT EXISTS journal_stage varchar(60);
ALTER TABLE public.repo_bitstream ADD COLUMN IF NOT EXISTS journal_genre varchar(80);
ALTER TABLE public.repo_bitstream ADD COLUMN IF NOT EXISTS review_round integer;
ALTER TABLE public.repo_bitstream ADD COLUMN IF NOT EXISTS storage_state varchar(40);
ALTER TABLE public.repo_bitstream ADD COLUMN IF NOT EXISTS content_ref bigint;
ALTER TABLE public.diskusi ADD COLUMN IF NOT EXISTS jurnal_penelitian_id bigint;
ALTER TABLE public.diskusi ADD COLUMN IF NOT EXISTS repo_item_id bigint;
ALTER TABLE public.diskusi ADD COLUMN IF NOT EXISTS stage_key varchar(80);
ALTER TABLE public.diskusi ADD COLUMN IF NOT EXISTS visibility varchar(40);
ALTER TABLE public.diskusi ADD COLUMN IF NOT EXISTS anonymity_mode varchar(30);
ALTER TABLE public.diskusi_komentar ALTER COLUMN keterangan TYPE text;
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_penelitian_id bigint;
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_template_key varchar(160);
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_template_version integer;
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_idempotency_key varchar(180);
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_correlation_id varchar(180);
ALTER TABLE public.notifikasi ADD COLUMN IF NOT EXISTS jurnal_snapshot_json text;
ALTER TABLE IF EXISTS new_audit.repo_bitstream__audit ADD COLUMN IF NOT EXISTS journal_stage varchar(60);
ALTER TABLE IF EXISTS new_audit.repo_bitstream__audit ADD COLUMN IF NOT EXISTS journal_genre varchar(80);
ALTER TABLE IF EXISTS new_audit.repo_bitstream__audit ADD COLUMN IF NOT EXISTS review_round integer;
ALTER TABLE IF EXISTS new_audit.repo_bitstream__audit ADD COLUMN IF NOT EXISTS storage_state varchar(40);
ALTER TABLE IF EXISTS new_audit.repo_bitstream__audit ADD COLUMN IF NOT EXISTS content_ref bigint;
ALTER TABLE IF EXISTS new_audit.diskusi__audit ADD COLUMN IF NOT EXISTS jurnal_penelitian_id bigint;
ALTER TABLE IF EXISTS new_audit.diskusi__audit ADD COLUMN IF NOT EXISTS repo_item_id bigint;
ALTER TABLE IF EXISTS new_audit.diskusi__audit ADD COLUMN IF NOT EXISTS stage_key varchar(80);
ALTER TABLE IF EXISTS new_audit.diskusi__audit ADD COLUMN IF NOT EXISTS visibility varchar(40);
ALTER TABLE IF EXISTS new_audit.diskusi__audit ADD COLUMN IF NOT EXISTS anonymity_mode varchar(30);
ALTER TABLE IF EXISTS new_audit.diskusi_komentar__audit ALTER COLUMN keterangan TYPE text;
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_penelitian_id bigint;
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_template_key varchar(160);
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_template_version integer;
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_idempotency_key varchar(180);
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_correlation_id varchar(180);
ALTER TABLE IF EXISTS new_audit.notifikasi__audit ADD COLUMN IF NOT EXISTS jurnal_snapshot_json text;

CREATE TABLE IF NOT EXISTS public.repo_author_authority (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, canonical_name varchar(255) NOT NULL,
 normalized_name varchar(255) NOT NULL, name_variants text, orcid varchar(40), nidn varchar(80),
 nip varchar(100), nim varchar(100), affiliation varchar(500), ror_id varchar(100),
 institutional_email varchar(255), topics text, verified boolean, aktif boolean,
 created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(),
 CONSTRAINT uq_repo_author_name UNIQUE(tenant_key,normalized_name)
);
ALTER TABLE public.repo_author_authority ADD COLUMN IF NOT EXISTS user_ref_id varchar(255);
ALTER TABLE public.repo_author_authority ADD COLUMN IF NOT EXISTS mahasiswa_ref_id bigint;

CREATE TABLE IF NOT EXISTS public.repo_item_contributor (
 id bigserial PRIMARY KEY, item_id bigint NOT NULL, authority_id bigint NOT NULL,
 contributor_role varchar(60) NOT NULL, display_name varchar(255) NOT NULL,
 sequence_number integer, corresponding boolean, aktif boolean, created_at timestamp NOT NULL DEFAULT now(),
 CONSTRAINT fk_ric_item FOREIGN KEY(item_id) REFERENCES public.repo_item(id),
 CONSTRAINT fk_ric_authority FOREIGN KEY(authority_id) REFERENCES public.repo_author_authority(id),
 CONSTRAINT uq_ric_role UNIQUE(item_id,authority_id,contributor_role)
);

CREATE TABLE IF NOT EXISTS public.repo_user_preference (
 id bigserial PRIMARY KEY, user_id varchar(255) NOT NULL, tenant_key varchar(120) NOT NULL,
 preference_type varchar(30) NOT NULL, item_id bigint, label varchar(255), query_value varchar(2000),
 created_at timestamp NOT NULL DEFAULT now(), aktif boolean,
 CONSTRAINT fk_rup_item FOREIGN KEY(item_id) REFERENCES public.repo_item(id)
);

CREATE TABLE IF NOT EXISTS public.repo_integration_event (
 id bigserial PRIMARY KEY, item_id bigint, tenant_key varchar(120) NOT NULL,
 service_name varchar(60) NOT NULL, action_name varchar(80) NOT NULL, status varchar(30) NOT NULL,
 actor_id varchar(255), request_id varchar(120), request_payload text, response_payload text,
 error_message text, created_at timestamp NOT NULL DEFAULT now(),
 CONSTRAINT fk_rie_item FOREIGN KEY(item_id) REFERENCES public.repo_item(id)
);

CREATE INDEX IF NOT EXISTS ix_repo_item_tenant_type ON public.repo_item(tenant_key,collection_id,document_type,workflow_status);
CREATE INDEX IF NOT EXISTS ix_repo_author_orcid ON public.repo_author_authority(tenant_key,orcid);
CREATE UNIQUE INDEX IF NOT EXISTS uq_repo_author_user ON public.repo_author_authority(tenant_key,user_ref_id) WHERE user_ref_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_repo_author_mahasiswa ON public.repo_author_authority(tenant_key,mahasiswa_ref_id) WHERE mahasiswa_ref_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_repo_author_email ON public.repo_author_authority(tenant_key,lower(institutional_email)) WHERE institutional_email IS NOT NULL AND institutional_email<>'';
CREATE INDEX IF NOT EXISTS ix_repo_contributor_item ON public.repo_item_contributor(item_id,sequence_number);
CREATE INDEX IF NOT EXISTS ix_repo_preference_user ON public.repo_user_preference(tenant_key,user_id,preference_type);
CREATE UNIQUE INDEX IF NOT EXISTS uq_repo_preference_user_label ON public.repo_user_preference(tenant_key,user_id,preference_type,label) WHERE aktif=true;
CREATE INDEX IF NOT EXISTS ix_repo_integration_retry ON public.repo_integration_event(tenant_key,service_name,status,created_at);
CREATE INDEX IF NOT EXISTS ix_repo_relation_toc ON public.repo_item_relation(item_id,relation_type,sort_order) WHERE aktif=true;
-- Idempotency is journal-scoped. Replace the earlier global-key draft index
-- idempotently so two journals may use the same workflow request key.
DROP INDEX IF EXISTS public.uq_notifikasi_jurnal_idempotency;
CREATE UNIQUE INDEX uq_notifikasi_jurnal_idempotency
 ON public.notifikasi(jurnal_penelitian_id,jurnal_idempotency_key)
 WHERE jurnal_penelitian_id IS NOT NULL AND jurnal_idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_notifikasi_jurnal_correlation ON public.notifikasi(jurnal_penelitian_id,jurnal_correlation_id,waktu);

COMMIT;
