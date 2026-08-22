-- Jurnal Terpadu V1 - Fase 2
-- Idempotent, existing-first, dan tepat 12 tabel baru di schema penelitiandanpengabdian.
BEGIN;

ALTER TABLE public.tbmrole ADD COLUMN IF NOT EXISTS jurnal_akses_json text;
ALTER TABLE IF EXISTS new_audit.tbmrole__audit ADD COLUMN IF NOT EXISTS jurnal_akses_json text;
ALTER TABLE penelitiandanpengabdian.jurnal_penelitian ADD COLUMN IF NOT EXISTS repo_collection_id bigint;
ALTER TABLE penelitiandanpengabdian.jurnal_penelitian ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
ALTER TABLE penelitiandanpengabdian.jurnal_penelitian ADD COLUMN IF NOT EXISTS default_locale varchar(20);
ALTER TABLE IF EXISTS new_audit.jurnal_penelitian__audit ADD COLUMN IF NOT EXISTS repo_collection_id bigint;
ALTER TABLE IF EXISTS new_audit.jurnal_penelitian__audit ADD COLUMN IF NOT EXISTS tenant_key varchar(120);
ALTER TABLE IF EXISTS new_audit.jurnal_penelitian__audit ADD COLUMN IF NOT EXISTS default_locale varchar(20);
ALTER TABLE penelitiandanpengabdian.artikel ADD COLUMN IF NOT EXISTS repo_item_id bigint;
ALTER TABLE penelitiandanpengabdian.file_artikel ADD COLUMN IF NOT EXISTS repo_bitstream_id bigint;
ALTER TABLE penelitiandanpengabdian.anggota_artikel ADD COLUMN IF NOT EXISTS repo_contributor_id bigint;
ALTER TABLE IF EXISTS new_audit.artikel__audit ADD COLUMN IF NOT EXISTS repo_item_id bigint;
ALTER TABLE IF EXISTS new_audit.file_artikel__audit ADD COLUMN IF NOT EXISTS repo_bitstream_id bigint;
ALTER TABLE IF EXISTS new_audit.anggota_artikel__audit ADD COLUMN IF NOT EXISTS repo_contributor_id bigint;
CREATE UNIQUE INDEX IF NOT EXISTS uq_artikel_repo_item ON penelitiandanpengabdian.artikel(repo_item_id) WHERE repo_item_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_file_artikel_repo_bitstream ON penelitiandanpengabdian.file_artikel(repo_bitstream_id) WHERE repo_bitstream_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_anggota_artikel_repo_contributor ON penelitiandanpengabdian.anggota_artikel(repo_contributor_id) WHERE repo_contributor_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.template_email_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 template_key varchar(160) NOT NULL, locale varchar(20) NOT NULL,
 subject_template text NOT NULL, body_template text NOT NULL, variable_policy_json text NOT NULL,
 version_number integer NOT NULL CHECK(version_number>0), lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_tej_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT uq_tej_version UNIQUE(tenant_key,jurnal_penelitian_id,template_key,locale,version_number)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.langganan_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 collection_id bigint NOT NULL, policy_key varchar(120) NOT NULL, policy_snapshot_json text NOT NULL,
 user_id varchar(255), institution_type varchar(80), institution_id bigint,
 starts_at timestamp NOT NULL, ends_at timestamp NOT NULL, status varchar(40) NOT NULL,
 payment_id bigint, external_reference varchar(255), lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_lj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_lj_collection FOREIGN KEY(collection_id) REFERENCES public.repo_collection(id),
 CONSTRAINT ck_lj_period CHECK(ends_at>starts_at),
 CONSTRAINT ck_lj_subject CHECK(user_id IS NOT NULL OR institution_id IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.undangan_peran_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 email varchar(320) NOT NULL, role_key varchar(80) NOT NULL, scope_type varchar(40) NOT NULL,
 scope_key varchar(255), token_hash varchar(128) NOT NULL, status varchar(30) NOT NULL,
 invited_user_id varchar(255), expires_at timestamp NOT NULL, accepted_at timestamp,
 declined_at timestamp, revoked_at timestamp, lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_upj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT uq_upj_token UNIQUE(token_hash)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.peserta_diskusi_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 diskusi_id bigint NOT NULL, user_id varchar(255) NOT NULL, participant_role varchar(60) NOT NULL,
 joined_at timestamp NOT NULL, left_at timestamp, lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_pdj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_pdj_diskusi FOREIGN KEY(diskusi_id) REFERENCES public.diskusi(id),
 CONSTRAINT uq_pdj_member UNIQUE(diskusi_id,user_id)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.penugasan_tahap_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 item_id bigint, user_id varchar(255) NOT NULL, role_key varchar(80) NOT NULL,
 stage_key varchar(80) NOT NULL, section_key varchar(120), status varchar(30) NOT NULL,
 provenance_json text, starts_at timestamp NOT NULL, ends_at timestamp,
 lock_version bigint NOT NULL DEFAULT 0, created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_ptj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_ptj_item FOREIGN KEY(item_id) REFERENCES public.repo_item(id),
 CONSTRAINT ck_ptj_period CHECK(ends_at IS NULL OR ends_at>starts_at)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.penugasan_reviewer_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 item_id bigint NOT NULL, reviewer_id varchar(255) NOT NULL, round_number integer NOT NULL CHECK(round_number>0),
 status varchar(40) NOT NULL, anonymity_mode varchar(30) NOT NULL, recommendation varchar(80),
 form_version_key varchar(120), response_json text, response_checksum varchar(64), conflict_json text,
 invited_at timestamp, response_due_at timestamp, review_due_at timestamp, accepted_at timestamp,
 declined_at timestamp, completed_at timestamp, cancelled_at timestamp,
 lock_version bigint NOT NULL DEFAULT 0, created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_prj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_prj_item FOREIGN KEY(item_id) REFERENCES public.repo_item(id),
 CONSTRAINT uq_prj_round UNIQUE(item_id,round_number,reviewer_id)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.agregat_penggunaan_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 bucket_start timestamp NOT NULL, bucket_type varchar(20) NOT NULL,
 metric_key varchar(80) NOT NULL, dimension_type varchar(60) NOT NULL, dimension_key varchar(255) NOT NULL,
 metric_value numeric(24,6) NOT NULL, counter_report varchar(40), lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_apj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT uq_apj_dimension UNIQUE(tenant_key,jurnal_penelitian_id,bucket_start,bucket_type,metric_key,dimension_type,dimension_key)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.rentang_ip_langganan_jurnal (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 langganan_id bigint NOT NULL, address_family integer NOT NULL CHECK(address_family IN (4,6)),
 start_address varchar(45) NOT NULL, end_address varchar(45) NOT NULL, label varchar(255),
 lock_version bigint NOT NULL DEFAULT 0, created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_rilj_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_rilj_langganan FOREIGN KEY(langganan_id) REFERENCES penelitiandanpengabdian.langganan_jurnal(id)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.import_sumber_ojs (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 source_key varchar(120) NOT NULL, display_name varchar(255) NOT NULL, ojs_version varchar(40) NOT NULL,
 dialect varchar(40) NOT NULL, schema_signature varchar(128) NOT NULL,
 connection_reference varchar(255) NOT NULL, status varchar(30) NOT NULL,
 lock_version bigint NOT NULL DEFAULT 0, created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_iso_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT uq_iso_source UNIQUE(tenant_key,source_key)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.import_job_ojs (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 source_id bigint NOT NULL, dry_run boolean NOT NULL, status varchar(30) NOT NULL,
 idempotency_key varchar(160) NOT NULL, report_json text, error_summary text,
 started_at timestamp, finished_at timestamp, lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_ijo_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_ijo_source FOREIGN KEY(source_id) REFERENCES penelitiandanpengabdian.import_sumber_ojs(id),
 CONSTRAINT uq_ijo_idempotency UNIQUE(tenant_key,idempotency_key)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.import_checkpoint_ojs (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 job_id bigint NOT NULL, source_table varchar(160) NOT NULL, cursor_value varchar(500),
 batch_number integer NOT NULL, processed_count bigint NOT NULL, accepted_count bigint NOT NULL,
 failed_count bigint NOT NULL, status varchar(30) NOT NULL, lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_ico_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_ico_job FOREIGN KEY(job_id) REFERENCES penelitiandanpengabdian.import_job_ojs(id),
 CONSTRAINT uq_ico_cursor UNIQUE(job_id,source_table,batch_number)
);

CREATE TABLE IF NOT EXISTS penelitiandanpengabdian.import_mapping_ojs (
 id bigserial PRIMARY KEY, tenant_key varchar(120) NOT NULL, jurnal_penelitian_id bigint,
 source_id bigint NOT NULL, job_id bigint, source_table varchar(160) NOT NULL,
 source_pk varchar(500) NOT NULL, source_field varchar(160), target_type varchar(255),
 target_id bigint, target_field varchar(160), decision varchar(40) NOT NULL,
 raw_payload text, source_checksum varchar(64), lock_version bigint NOT NULL DEFAULT 0,
 created_by varchar(255), created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now(), aktif boolean NOT NULL DEFAULT true,
 CONSTRAINT fk_imo_jurnal FOREIGN KEY(jurnal_penelitian_id) REFERENCES penelitiandanpengabdian.jurnal_penelitian(id),
 CONSTRAINT fk_imo_source FOREIGN KEY(source_id) REFERENCES penelitiandanpengabdian.import_sumber_ojs(id),
 CONSTRAINT fk_imo_job FOREIGN KEY(job_id) REFERENCES penelitiandanpengabdian.import_job_ojs(id),
 CONSTRAINT uq_imo_provenance UNIQUE(source_id,source_table,source_pk,source_field)
);

CREATE INDEX IF NOT EXISTS ix_lj_access ON penelitiandanpengabdian.langganan_jurnal(tenant_key,jurnal_penelitian_id,status,starts_at,ends_at);
CREATE INDEX IF NOT EXISTS ix_upj_lookup ON penelitiandanpengabdian.undangan_peran_jurnal(tenant_key,jurnal_penelitian_id,email,status);
CREATE INDEX IF NOT EXISTS ix_ptj_scope ON penelitiandanpengabdian.penugasan_tahap_jurnal(tenant_key,jurnal_penelitian_id,item_id,user_id,stage_key,status);
CREATE INDEX IF NOT EXISTS ix_prj_queue ON penelitiandanpengabdian.penugasan_reviewer_jurnal(tenant_key,jurnal_penelitian_id,item_id,round_number,status);
CREATE INDEX IF NOT EXISTS ix_apj_report ON penelitiandanpengabdian.agregat_penggunaan_jurnal(tenant_key,jurnal_penelitian_id,bucket_start,metric_key);
CREATE INDEX IF NOT EXISTS ix_rilj_subscription ON penelitiandanpengabdian.rentang_ip_langganan_jurnal(langganan_id,address_family);
CREATE INDEX IF NOT EXISTS ix_ijo_status ON penelitiandanpengabdian.import_job_ojs(source_id,status,created_at);
CREATE INDEX IF NOT EXISTS ix_imo_target ON penelitiandanpengabdian.import_mapping_ojs(target_type,target_id);

COMMIT;
