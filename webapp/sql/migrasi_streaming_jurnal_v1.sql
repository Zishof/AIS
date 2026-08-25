\set ON_ERROR_STOP on
BEGIN;

-- Explicit migration for the streaming database. Runtime hbm2ddl stays none;
-- this file is safe to rerun on SIT/UAT/demo/fixture clones.
CREATE TABLE IF NOT EXISTS public.lampiran_jurnal (
 id bigserial PRIMARY KEY,
 actual_size bigint,
 checksum_sha256 varchar(64),
 file_content oid NOT NULL,
 created_at timestamp NOT NULL,
 created_by varchar(100) NOT NULL,
 declared_mime_type varchar(100) NOT NULL,
 declared_size bigint NOT NULL,
 detected_mime_type varchar(100) NOT NULL,
 file_version bigint NOT NULL,
 idempotency_key varchar(160) NOT NULL,
 journal_stage varchar(60) NOT NULL,
 original_file_name varchar(255) NOT NULL,
 quarantine_state varchar(30) NOT NULL,
 repo_bitstream_id bigint NOT NULL,
 scan_state varchar(30) NOT NULL,
 storage_state varchar(40) NOT NULL,
 updated_at timestamp NOT NULL,
 updated_by varchar(100) NOT NULL,
 CONSTRAINT uk_lampiran_jurnal_idempotency UNIQUE(idempotency_key),
 CONSTRAINT uk_lampiran_jurnal_bitstream UNIQUE(repo_bitstream_id)
);

COMMIT;
