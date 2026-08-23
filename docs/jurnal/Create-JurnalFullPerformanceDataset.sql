\set ON_ERROR_STOP on
\timing on

DO $$
BEGIN
  IF current_database() <> 'ais_jurnal_sit' THEN
    RAISE EXCEPTION 'Performance dataset hanya boleh dibuat pada ais_jurnal_sit';
  END IF;
END $$;

SET synchronous_commit = off;
SET maintenance_work_mem = '512MB';
SET statement_timeout = 0;

DROP SCHEMA IF EXISTS jurnal_perf CASCADE;
CREATE SCHEMA jurnal_perf;

CREATE UNLOGGED TABLE jurnal_perf.article (
  id bigint NOT NULL,
  journal_id bigint NOT NULL,
  tenant_key varchar(40) NOT NULL,
  title text NOT NULL,
  workflow_status varchar(32) NOT NULL,
  published_at timestamp,
  aktif boolean NOT NULL
);

INSERT INTO jurnal_perf.article
SELECT g,
       ((g - 1) % 100) + 1,
       'perf-tenant-' || (((g - 1) % 10) + 1),
       'Full scale performance article ' || g,
       (ARRAY['DRAFT','SUBMITTED','SCREENING','IN_REVIEW','COPYEDITING','PRODUCTION','PROOF','SCHEDULED','PUBLISHED','REJECTED'])[((((g - 1) / 100) % 10) + 1)::integer],
       CASE WHEN (((g - 1) / 100) % 10) = 8 THEN timestamp '2026-08-23 00:00:00' - ((g % 31536000) * interval '1 second') END,
       true
FROM generate_series(1,100000) g;

ALTER TABLE jurnal_perf.article ADD CONSTRAINT perf_article_pk PRIMARY KEY (id);
CREATE INDEX perf_article_journal_status_published ON jurnal_perf.article(journal_id,workflow_status,published_at DESC,id DESC);
CREATE INDEX perf_article_tenant_status ON jurnal_perf.article(tenant_key,workflow_status);

CREATE UNLOGGED TABLE jurnal_perf.file_metadata (
  id bigint NOT NULL,
  item_id bigint NOT NULL,
  bundle_name varchar(40) NOT NULL,
  mime_type varchar(100) NOT NULL,
  ukuran_byte bigint NOT NULL,
  checksum varchar(64) NOT NULL,
  journal_stage varchar(40) NOT NULL,
  storage_state varchar(32) NOT NULL,
  file_version bigint NOT NULL,
  aktif boolean NOT NULL
);

INSERT INTO jurnal_perf.file_metadata
SELECT g,
       ((g - 1) % 100000) + 1,
       (ARRAY['SUBMISSION','REVIEW','COPYEDIT','PRODUCTION','PUBLICATION'])[((g - 1) % 5) + 1],
       (ARRAY['application/pdf','text/xml','text/plain','image/png'])[((g - 1) % 4) + 1],
       1024 + (g % 10485760),
       lpad(to_hex(g),64,'0'),
       (ARRAY['SUBMISSION','REVIEW','COPYEDITING','PRODUCTION','PUBLICATION'])[((g - 1) % 5) + 1],
       'AVAILABLE',
       ((g - 1) % 10) + 1,
       true
FROM generate_series(1,1000000) g;

ALTER TABLE jurnal_perf.file_metadata ADD CONSTRAINT perf_file_pk PRIMARY KEY (id);
CREATE INDEX perf_file_item_bundle_version ON jurnal_perf.file_metadata(item_id,bundle_name,file_version DESC);
CREATE INDEX perf_file_stage_state ON jurnal_perf.file_metadata(journal_stage,storage_state);

CREATE UNLOGGED TABLE jurnal_perf.perf_user (
  userid varchar(40) NOT NULL,
  journal_id bigint NOT NULL,
  role_key varchar(32) NOT NULL,
  aktif boolean NOT NULL
);

INSERT INTO jurnal_perf.perf_user
SELECT 'PERF_USER_' || lpad(g::text,5,'0'),
       ((g - 1) % 100) + 1,
       (ARRAY['MANAGER','EDITOR','SECTION_EDITOR','REVIEWER','AUTHOR','COPYEDITOR','PRODUCTION','LIBRARIAN','FINANCE','READER'])[((g - 1) % 10) + 1],
       true
FROM generate_series(1,10000) g;

ALTER TABLE jurnal_perf.perf_user ADD CONSTRAINT perf_user_pk PRIMARY KEY (userid);
CREATE INDEX perf_user_journal_role ON jurnal_perf.perf_user(journal_id,role_key) WHERE aktif;

CREATE UNLOGGED TABLE jurnal_perf.usage_event (
  id bigint NOT NULL,
  item_id bigint NOT NULL,
  bitstream_id bigint,
  event_type varchar(20) NOT NULL,
  visitor_hash varchar(32),
  actor_id varchar(40),
  user_agent_class varchar(16),
  country_code varchar(2),
  referrer_host varchar(64),
  occurred_at timestamp NOT NULL
);

INSERT INTO jurnal_perf.usage_event
SELECT g,
       ((g - 1) % 100000) + 1,
       CASE WHEN (g % 3) = 0 THEN ((g - 1) % 1000000) + 1 END,
       CASE WHEN (g % 3) = 0 THEN 'DOWNLOAD' ELSE 'VIEW' END,
       'v' || lpad((g % 1000000)::text,7,'0'),
       CASE WHEN (g % 5) = 0 THEN 'PERF_USER_' || lpad(((g % 10000) + 1)::text,5,'0') END,
       CASE WHEN (g % 20) = 0 THEN 'BOT' ELSE 'BROWSER' END,
       (ARRAY['ID','US','SG','AU','MY'])[((g - 1) % 5) + 1],
       (ARRAY['google.com','scholar.google.com','example.edu','direct.local'])[((g - 1) % 4) + 1],
       timestamp '2026-08-23 00:00:00' - ((g % 2592000) * interval '1 second')
FROM generate_series(1,10000000) g;

ALTER TABLE jurnal_perf.usage_event ADD CONSTRAINT perf_usage_pk PRIMARY KEY (id);
CREATE INDEX perf_usage_item_occurred ON jurnal_perf.usage_event(item_id,occurred_at DESC);
CREATE INDEX perf_usage_occurred_type ON jurnal_perf.usage_event(occurred_at,event_type) WHERE user_agent_class <> 'BOT';

ANALYZE jurnal_perf.article;
ANALYZE jurnal_perf.file_metadata;
ANALYZE jurnal_perf.perf_user;
ANALYZE jurnal_perf.usage_event;

SELECT 'article' AS dataset,count(*) AS rows FROM jurnal_perf.article
UNION ALL SELECT 'file_metadata',count(*) FROM jurnal_perf.file_metadata
UNION ALL SELECT 'perf_user',count(*) FROM jurnal_perf.perf_user
UNION ALL SELECT 'usage_event',count(*) FROM jurnal_perf.usage_event
ORDER BY dataset;

SELECT pg_size_pretty(sum(pg_total_relation_size(c.oid))) AS performance_dataset_size
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='jurnal_perf' AND c.relkind IN ('r','i');
