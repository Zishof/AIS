
-- ============================================================================
-- AIS Generic CRUD V2.1
-- Audit/revision, restore jobs, Super Admin active-row delete policy,
-- and custom/complex form override configuration.
-- PostgreSQL; additive migration. Review naming/schema on the active checkout.
-- ============================================================================

BEGIN;

ALTER TABLE generic_crud_entity_config
    ADD COLUMN IF NOT EXISTS audit_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS global_audit_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS row_audit_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS field_restore_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS manual_field_correction_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS revision_restore_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deep_restore_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mass_restore_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS admin_delete_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS admin_delete_requires_reason boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS admin_delete_confirmation_template varchar(500),
    ADD COLUMN IF NOT EXISTS audit_revision_adapter_class varchar(500),
    ADD COLUMN IF NOT EXISTS restore_policy_class varchar(500),
    ADD COLUMN IF NOT EXISTS permanent_delete_policy_class varchar(500),
    ADD COLUMN IF NOT EXISTS form_mode varchar(40) NOT NULL DEFAULT 'GENERIC_DRAWER',
    ADD COLUMN IF NOT EXISTS form_override_provider_class varchar(500),
    ADD COLUMN IF NOT EXISTS form_definition_version integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS form_definition_json text;

ALTER TABLE generic_crud_field_config
    ADD COLUMN IF NOT EXISTS form_tab_key varchar(250),
    ADD COLUMN IF NOT EXISTS form_section_key varchar(250),
    ADD COLUMN IF NOT EXISTS form_position integer NOT NULL DEFAULT 9999,
    ADD COLUMN IF NOT EXISTS form_column_span integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS visible_expression text,
    ADD COLUMN IF NOT EXISTS readonly_expression text,
    ADD COLUMN IF NOT EXISTS restoreable boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS manual_correction_enabled boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS ix_generic_crud_field_form_position
    ON generic_crud_field_config
       (entity_config_id, form_tab_key, form_section_key, form_position)
    WHERE active = true;

CREATE TABLE IF NOT EXISTS generic_crud_form_definition (
    id                         bigserial PRIMARY KEY,
    entity_config_id           bigint NOT NULL,
    page_binding_id            bigint,
    form_key                   varchar(250) NOT NULL,
    display_name               varchar(500) NOT NULL,
    mode                       varchar(40) NOT NULL DEFAULT 'GENERIC_DRAWER',
    provider_class             varchar(500),
    save_strategy              varchar(40) NOT NULL DEFAULT 'ATOMIC_AGGREGATE',
    definition_json            text,
    version                    integer NOT NULL DEFAULT 1,
    active                     boolean NOT NULL DEFAULT false,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    created_by                 varchar(250),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_by                 varchar(250),
    CONSTRAINT fk_generic_crud_form_entity
        FOREIGN KEY (entity_config_id)
        REFERENCES generic_crud_entity_config(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_generic_crud_form_binding
        FOREIGN KEY (page_binding_id)
        REFERENCES generic_crud_page_binding(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_generic_crud_form_definition
        UNIQUE (entity_config_id, page_binding_id, form_key, version),
    CONSTRAINT ck_generic_crud_form_mode CHECK (
        mode IN ('GENERIC_DRAWER','GENERIC_MODAL','TABBED_DRAWER',
                 'FULL_PAGE_TABS','WIZARD','CUSTOM_COMPONENT','LEGACY_BRIDGE')
    ),
    CONSTRAINT ck_generic_crud_save_strategy CHECK (
        save_strategy IN ('ROOT_FIRST','PER_TAB_TRANSACTION','ATOMIC_AGGREGATE',
                          'DRAFT_AGGREGATE','LEGACY_ACTION_BRIDGE')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_form_lookup
    ON generic_crud_form_definition
       (entity_config_id, page_binding_id, active, version DESC);

CREATE TABLE IF NOT EXISTS generic_crud_form_tab_config (
    id                         bigserial PRIMARY KEY,
    form_definition_id         bigint NOT NULL,
    tab_key                    varchar(250) NOT NULL,
    display_name               varchar(500) NOT NULL,
    icon_key                   varchar(250),
    display_order              integer NOT NULL DEFAULT 9999,
    required_privilege         varchar(20) NOT NULL DEFAULT 'READ',
    lazy_load                  boolean NOT NULL DEFAULT false,
    requires_persisted_entity  boolean NOT NULL DEFAULT false,
    save_before_enter          boolean NOT NULL DEFAULT false,
    validation_group_key       varchar(250),
    visible_expression         text,
    badge_provider_key         varchar(500),
    mobile_presentation        varchar(30) NOT NULL DEFAULT 'AUTO',
    config_json                text,
    active                     boolean NOT NULL DEFAULT true,
    CONSTRAINT fk_generic_crud_form_tab
        FOREIGN KEY (form_definition_id)
        REFERENCES generic_crud_form_definition(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_generic_crud_form_tab UNIQUE (form_definition_id, tab_key),
    CONSTRAINT ck_generic_crud_form_tab_priv CHECK (
        required_privilege IN ('READ','CREATE','UPDATE','DELETE','APPROVE','REJECT','CUSTOM')
    ),
    CONSTRAINT ck_generic_crud_form_mobile CHECK (
        mobile_presentation IN ('AUTO','TAB_SCROLL','STEPPER','SELECT','SECTION')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_form_tab_order
    ON generic_crud_form_tab_config (form_definition_id, active, display_order);

CREATE TABLE IF NOT EXISTS generic_crud_form_section_config (
    id                         bigserial PRIMARY KEY,
    form_definition_id         bigint NOT NULL,
    tab_key                    varchar(250),
    section_key                varchar(250) NOT NULL,
    display_name               varchar(500) NOT NULL,
    description                text,
    display_order              integer NOT NULL DEFAULT 9999,
    collapsible                boolean NOT NULL DEFAULT false,
    initially_expanded         boolean NOT NULL DEFAULT true,
    columns_desktop            integer NOT NULL DEFAULT 2,
    columns_tablet             integer NOT NULL DEFAULT 2,
    config_json                text,
    active                     boolean NOT NULL DEFAULT true,
    CONSTRAINT fk_generic_crud_form_section
        FOREIGN KEY (form_definition_id)
        REFERENCES generic_crud_form_definition(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_generic_crud_form_section
        UNIQUE (form_definition_id, tab_key, section_key),
    CONSTRAINT ck_generic_crud_form_columns CHECK (
        columns_desktop BETWEEN 1 AND 4 AND columns_tablet BETWEEN 1 AND 2
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_form_section_order
    ON generic_crud_form_section_config
       (form_definition_id, tab_key, active, display_order);

CREATE TABLE IF NOT EXISTS generic_crud_restore_job (
    id                         bigserial PRIMARY KEY,
    job_key                    varchar(100) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    owner_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    menu_id                    bigint,
    request_id                 varchar(100),
    scope_hash                 varchar(128),
    filter_hash                varchar(128),
    mode                       varchar(40) NOT NULL,
    status                     varchar(40) NOT NULL DEFAULT 'PREVIEWING',
    target_object_key          varchar(1000),
    target_revision_number     bigint,
    target_property            varchar(500),
    from_date                  timestamp without time zone,
    deep_restore               boolean NOT NULL DEFAULT false,
    dry_run                    boolean NOT NULL DEFAULT true,
    reason                     text,
    request_snapshot_json      text,
    preview_summary_json       text,
    total_items                bigint NOT NULL DEFAULT 0,
    processed_items            bigint NOT NULL DEFAULT 0,
    success_items              bigint NOT NULL DEFAULT 0,
    failed_items               bigint NOT NULL DEFAULT 0,
    skipped_items              bigint NOT NULL DEFAULT 0,
    conflict_items             bigint NOT NULL DEFAULT 0,
    result_file_reference      text,
    error_summary              text,
    cancel_requested           boolean NOT NULL DEFAULT false,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    started_at                 timestamp without time zone,
    heartbeat_at               timestamp without time zone,
    finished_at                timestamp without time zone,
    expires_at                 timestamp without time zone,
    CONSTRAINT uq_generic_crud_restore_job_key UNIQUE (job_key),
    CONSTRAINT ck_generic_crud_restore_mode CHECK (
        mode IN ('FIELD','MANUAL_FIELD','REVISION','DEEP_REVISION',
                 'LATEST_FROM_DATE','RESTORE_SOFT_DELETED')
    ),
    CONSTRAINT ck_generic_crud_restore_status CHECK (
        status IN ('PREVIEWING','PREVIEW_READY','CONFIRMATION_REQUIRED',
                   'QUEUED','RUNNING','COMPLETED','COMPLETED_WITH_ERRORS',
                   'FAILED','CANCELED','EXPIRED')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_restore_owner
    ON generic_crud_restore_job
       (owner_user_key, active_role_key, entity_key, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_generic_crud_restore_status
    ON generic_crud_restore_job (status, heartbeat_at);

CREATE TABLE IF NOT EXISTS generic_crud_restore_job_item (
    id                         bigserial PRIMARY KEY,
    restore_job_id             bigint NOT NULL,
    object_key                 varchar(1000),
    revision_number            bigint,
    item_order                 bigint NOT NULL DEFAULT 0,
    status                     varchar(30) NOT NULL DEFAULT 'PENDING',
    result_message             text,
    error_class                varchar(500),
    error_message              text,
    started_at                 timestamp without time zone,
    finished_at                timestamp without time zone,
    CONSTRAINT fk_generic_crud_restore_item
        FOREIGN KEY (restore_job_id)
        REFERENCES generic_crud_restore_job(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_generic_crud_restore_item_status CHECK (
        status IN ('PENDING','RUNNING','SUCCESS','FAILED','SKIPPED','CONFLICT')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_restore_item_job
    ON generic_crud_restore_job_item (restore_job_id, status, item_order);

ALTER TABLE generic_crud_audit_event
    ADD COLUMN IF NOT EXISTS revision_number bigint,
    ADD COLUMN IF NOT EXISTS revision_timestamp timestamp without time zone,
    ADD COLUMN IF NOT EXISTS revision_type varchar(20),
    ADD COLUMN IF NOT EXISTS target_revision_number bigint,
    ADD COLUMN IF NOT EXISTS operation_mode varchar(50),
    ADD COLUMN IF NOT EXISTS super_admin_operation boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS policy_version varchar(100),
    ADD COLUMN IF NOT EXISTS correlation_id varchar(100),
    ADD COLUMN IF NOT EXISTS metadata_json text;

CREATE INDEX IF NOT EXISTS ix_generic_crud_audit_revision
    ON generic_crud_audit_event
       (entity_key, object_key, revision_number, created_at DESC);

COMMIT;

-- Safe defaults: audit view can be enabled per reviewed entity, while every
-- restore and admin-delete feature remains disabled until policy/adapters/tests
-- are complete. Never bulk-enable admin_delete_enabled for all entities.
