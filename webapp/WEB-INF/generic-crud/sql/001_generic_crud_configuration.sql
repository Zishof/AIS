-- ============================================================================
-- AIS Generic CRUD V2 - tabel konfigurasi, preferensi, job, idempotensi, audit
-- PostgreSQL. Review schema/table naming convention project sebelum production.
-- Migration ini additive dan tidak membuat foreign key ke tabel existing karena
-- tipe identifier Tbmuser/Tbmrole/Menu harus diverifikasi pada source terbaru.
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS generic_crud_entity_config (
    id                         bigserial PRIMARY KEY,
    entity_key                 varchar(500) NOT NULL,
    entity_class               varchar(500) NOT NULL,
    module_key                 varchar(250) NOT NULL,
    page_key                   varchar(250) NOT NULL,
    menu_id                    bigint,
    display_name               varchar(500) NOT NULL,
    description                text,
    lifecycle_status           varchar(40) NOT NULL DEFAULT 'REVIEW_REQUIRED',
    enabled                    boolean NOT NULL DEFAULT false,
    read_only                  boolean NOT NULL DEFAULT true,
    create_enabled             boolean NOT NULL DEFAULT false,
    update_enabled             boolean NOT NULL DEFAULT false,
    delete_enabled             boolean NOT NULL DEFAULT false,
    approve_enabled            boolean NOT NULL DEFAULT false,
    reject_enabled             boolean NOT NULL DEFAULT false,
    import_enabled             boolean NOT NULL DEFAULT false,
    import_delete_enabled      boolean NOT NULL DEFAULT false,
    export_xlsx_enabled        boolean NOT NULL DEFAULT true,
    export_pdf_enabled         boolean NOT NULL DEFAULT false,
    export_docx_enabled        boolean NOT NULL DEFAULT false,
    export_pptx_enabled        boolean NOT NULL DEFAULT false,
    photo_enabled              boolean NOT NULL DEFAULT false,
    saved_view_enabled         boolean NOT NULL DEFAULT true,
    bulk_edit_enabled          boolean NOT NULL DEFAULT false,
    soft_delete_enabled        boolean NOT NULL DEFAULT false,
    adapter_class              varchar(500),
    scope_adapter_class        varchar(500),
    approval_adapter_class     varchar(500),
    photo_adapter_class        varchar(500),
    identifier_property        varchar(250),
    natural_key_properties     text,
    version_property           varchar(250),
    soft_delete_property       varchar(250),
    active_property            varchar(250),
    default_sort_property      varchar(250),
    default_sort_direction     varchar(4) NOT NULL DEFAULT 'ASC',
    default_page_size          integer NOT NULL DEFAULT 10,
    max_page_size              integer NOT NULL DEFAULT 1000,
    lookup_threshold           integer NOT NULL DEFAULT 20,
    max_export_rows            integer NOT NULL DEFAULT 100000,
    max_import_rows            integer NOT NULL DEFAULT 100000,
    synchronous_export_limit   integer NOT NULL DEFAULT 5000,
    config_version             integer NOT NULL DEFAULT 1,
    metadata_json              text,
    review_notes               text,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    created_by                 varchar(250),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_by                 varchar(250),
    CONSTRAINT uq_generic_crud_entity_key UNIQUE (entity_key),
    CONSTRAINT uq_generic_crud_module_page UNIQUE (module_key, page_key),
    CONSTRAINT ck_generic_crud_lifecycle CHECK (
        lifecycle_status IN ('DISABLED','REVIEW_REQUIRED','READ_ONLY','FULL_CRUD')
    ),
    CONSTRAINT ck_generic_crud_sort_dir CHECK (default_sort_direction IN ('ASC','DESC')),
    CONSTRAINT ck_generic_crud_default_page_size CHECK (
        default_page_size IN (5,10,25,50,100,500,1000)
    ),
    CONSTRAINT ck_generic_crud_max_page_size CHECK (
        max_page_size IN (5,10,25,50,100,500,1000)
    ),
    CONSTRAINT ck_generic_crud_threshold CHECK (lookup_threshold >= 1),
    CONSTRAINT ck_generic_crud_limits CHECK (
        max_export_rows > 0 AND max_import_rows > 0 AND synchronous_export_limit > 0
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_entity_module
    ON generic_crud_entity_config (module_key, enabled, lifecycle_status);
CREATE INDEX IF NOT EXISTS ix_generic_crud_entity_menu
    ON generic_crud_entity_config (menu_id);

-- Satu entity dapat dipakai oleh beberapa menu/module. Binding aktif adalah
-- sumber menuId untuk privilege dan dapat memiliki adapter/scope override.
CREATE TABLE IF NOT EXISTS generic_crud_page_binding (
    id                         bigserial PRIMARY KEY,
    entity_config_id           bigint NOT NULL,
    module_key                 varchar(250) NOT NULL,
    page_key                   varchar(250) NOT NULL,
    menu_id                    bigint,
    display_name_override      varchar(500),
    description_override       text,
    adapter_class_override     varchar(500),
    scope_adapter_override     varchar(500),
    read_only_override         boolean,
    enabled                    boolean NOT NULL DEFAULT false,
    binding_json               text,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    created_by                 varchar(250),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_by                 varchar(250),
    CONSTRAINT fk_generic_crud_page_entity
        FOREIGN KEY (entity_config_id)
        REFERENCES generic_crud_entity_config(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_generic_crud_page_binding UNIQUE (module_key, page_key)
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_page_entity
    ON generic_crud_page_binding (entity_config_id, enabled);
CREATE INDEX IF NOT EXISTS ix_generic_crud_page_menu
    ON generic_crud_page_binding (menu_id)
    WHERE menu_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS generic_crud_field_config (
    id                         bigserial PRIMARY KEY,
    entity_config_id           bigint NOT NULL,
    field_key                  varchar(500) NOT NULL,
    property_path              varchar(500) NOT NULL,
    display_name               varchar(500) NOT NULL,
    section_name               varchar(250),
    mapped_type                varchar(250),
    java_type                  varchar(500),
    editor_type                varchar(80),
    filter_type                varchar(80),
    relation_entity_key        varchar(500),
    relation_display_property  varchar(500),
    relation_search_properties text,
    relation_lookup_threshold  integer,
    visible_in_table           boolean NOT NULL DEFAULT false,
    visible_in_quick_filter    boolean NOT NULL DEFAULT false,
    visible_in_advanced_filter boolean NOT NULL DEFAULT true,
    visible_in_detail          boolean NOT NULL DEFAULT true,
    visible_in_form            boolean NOT NULL DEFAULT true,
    readable                   boolean NOT NULL DEFAULT true,
    createable                 boolean NOT NULL DEFAULT false,
    updateable                 boolean NOT NULL DEFAULT false,
    required                   boolean NOT NULL DEFAULT false,
    sortable                   boolean NOT NULL DEFAULT false,
    searchable                 boolean NOT NULL DEFAULT false,
    importable                 boolean NOT NULL DEFAULT false,
    exportable                 boolean NOT NULL DEFAULT true,
    sensitive                  boolean NOT NULL DEFAULT false,
    masking_mode               varchar(40) NOT NULL DEFAULT 'NONE',
    default_position           integer NOT NULL DEFAULT 9999,
    default_width              integer,
    pin_position               varchar(10),
    help_text                  text,
    validation_rule_key        varchar(250),
    format_pattern             varchar(250),
    default_value_expression   text,
    config_json                text,
    active                     boolean NOT NULL DEFAULT true,
    config_version             integer NOT NULL DEFAULT 1,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    created_by                 varchar(250),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_by                 varchar(250),
    CONSTRAINT fk_generic_crud_field_entity
        FOREIGN KEY (entity_config_id)
        REFERENCES generic_crud_entity_config(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_generic_crud_field UNIQUE (entity_config_id, field_key),
    CONSTRAINT uq_generic_crud_property UNIQUE (entity_config_id, property_path),
    CONSTRAINT ck_generic_crud_mask CHECK (
        masking_mode IN ('NONE','PARTIAL','FULL','LAST4','EMAIL','PHONE','CUSTOM')
    ),
    CONSTRAINT ck_generic_crud_pin CHECK (
        pin_position IS NULL OR pin_position IN ('LEFT','RIGHT')
    ),
    CONSTRAINT ck_generic_crud_relation_threshold CHECK (
        relation_lookup_threshold IS NULL OR relation_lookup_threshold >= 1
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_field_entity_position
    ON generic_crud_field_config (entity_config_id, active, default_position);
CREATE INDEX IF NOT EXISTS ix_generic_crud_field_relation
    ON generic_crud_field_config (relation_entity_key)
    WHERE relation_entity_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS generic_crud_user_view (
    id                         bigserial PRIMARY KEY,
    user_key                   varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    view_name                  varchar(250) NOT NULL DEFAULT 'default',
    is_default                 boolean NOT NULL DEFAULT true,
    visible_columns_json       text,
    column_order_json          text,
    column_widths_json         text,
    pinned_columns_json        text,
    page_size                  integer NOT NULL DEFAULT 10,
    density                    varchar(20) NOT NULL DEFAULT 'COMFORTABLE',
    sort_json                  text,
    filter_json                text,
    ui_state_json              text,
    config_version             integer NOT NULL DEFAULT 1,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_generic_crud_user_view
        UNIQUE (user_key, active_role_key, entity_key, view_name),
    CONSTRAINT ck_generic_crud_user_page_size CHECK (
        page_size IN (5,10,25,50,100,500,1000)
    ),
    CONSTRAINT ck_generic_crud_density CHECK (
        density IN ('COMPACT','COMFORTABLE')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_user_view_lookup
    ON generic_crud_user_view (user_key, active_role_key, entity_key, is_default);

CREATE TABLE IF NOT EXISTS generic_crud_saved_view (
    id                         bigserial PRIMARY KEY,
    owner_user_key             varchar(250) NOT NULL,
    owner_role_key             varchar(250) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    view_name                  varchar(250) NOT NULL,
    description                text,
    visibility                 varchar(20) NOT NULL DEFAULT 'PRIVATE',
    shared_role_keys           text,
    filter_json                text,
    sort_json                  text,
    columns_json               text,
    ui_state_json              text,
    is_default                 boolean NOT NULL DEFAULT false,
    active                     boolean NOT NULL DEFAULT true,
    config_version             integer NOT NULL DEFAULT 1,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_generic_crud_saved_view
        UNIQUE (owner_user_key, owner_role_key, entity_key, view_name),
    CONSTRAINT ck_generic_crud_saved_visibility CHECK (
        visibility IN ('PRIVATE','ROLE','PUBLIC_AUTHORIZED')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_saved_view_entity
    ON generic_crud_saved_view (entity_key, active, visibility);

CREATE TABLE IF NOT EXISTS generic_crud_custom_action_config (
    id                         bigserial PRIMARY KEY,
    entity_key                 varchar(500) NOT NULL,
    action_key                 varchar(250) NOT NULL,
    display_name               varchar(500) NOT NULL,
    icon_key                   varchar(250),
    placement                 varchar(20) NOT NULL DEFAULT 'ROW',
    required_privilege         varchar(20) NOT NULL DEFAULT 'READ',
    selection_mode             varchar(20) NOT NULL DEFAULT 'SINGLE',
    confirmation_mode          varchar(20) NOT NULL DEFAULT 'NONE',
    danger                     boolean NOT NULL DEFAULT false,
    asynchronous              boolean NOT NULL DEFAULT false,
    handler_class              varchar(500) NOT NULL,
    parameter_schema_json      text,
    display_order              integer NOT NULL DEFAULT 9999,
    active                     boolean NOT NULL DEFAULT false,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    created_by                 varchar(250),
    updated_at                 timestamp without time zone NOT NULL DEFAULT now(),
    updated_by                 varchar(250),
    CONSTRAINT uq_generic_crud_custom_action UNIQUE (entity_key, action_key),
    CONSTRAINT ck_generic_crud_action_place CHECK (
        placement IN ('TOOLBAR','ROW','DETAIL','BULK','FORM')
    ),
    CONSTRAINT ck_generic_crud_action_priv CHECK (
        required_privilege IN ('READ','CREATE','UPDATE','DELETE','APPROVE','REJECT','CUSTOM')
    ),
    CONSTRAINT ck_generic_crud_action_selection CHECK (
        selection_mode IN ('NONE','SINGLE','MULTIPLE','ALL_FILTERED')
    ),
    CONSTRAINT ck_generic_crud_action_confirm CHECK (
        confirmation_mode IN ('NONE','SIMPLE','REASON','SECOND_FACTOR','TYPED_TEXT')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_custom_action_entity
    ON generic_crud_custom_action_config (entity_key, active, display_order);

CREATE TABLE IF NOT EXISTS generic_crud_import_job (
    id                         bigserial PRIMARY KEY,
    job_key                    varchar(100) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    owner_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    menu_id                    bigint,
    scope_hash                 varchar(128),
    file_hash                  varchar(128),
    original_file_name         varchar(1000),
    stored_file_reference      text,
    template_version           integer,
    mode                       varchar(30) NOT NULL DEFAULT 'UPSERT',
    status                     varchar(30) NOT NULL DEFAULT 'UPLOADED',
    total_rows                 bigint NOT NULL DEFAULT 0,
    create_rows                bigint NOT NULL DEFAULT 0,
    update_rows                bigint NOT NULL DEFAULT 0,
    delete_rows                bigint NOT NULL DEFAULT 0,
    skip_rows                  bigint NOT NULL DEFAULT 0,
    error_rows                 bigint NOT NULL DEFAULT 0,
    processed_rows             bigint NOT NULL DEFAULT 0,
    dry_run_summary_json       text,
    request_snapshot_json      text,
    error_summary              text,
    result_file_reference      text,
    request_id                 varchar(100),
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    started_at                 timestamp without time zone,
    finished_at                timestamp without time zone,
    expires_at                 timestamp without time zone,
    heartbeat_at               timestamp without time zone,
    cancel_requested           boolean NOT NULL DEFAULT false,
    CONSTRAINT uq_generic_crud_import_job_key UNIQUE (job_key),
    CONSTRAINT ck_generic_crud_import_mode CHECK (
        mode IN ('CREATE_ONLY','UPDATE_ONLY','UPSERT','UPSERT_DELETE')
    ),
    CONSTRAINT ck_generic_crud_import_status CHECK (
        status IN ('UPLOADED','VALIDATING','DRY_RUN_READY','CONFIRMATION_REQUIRED',
                   'QUEUED','RUNNING','COMPLETED','COMPLETED_WITH_ERRORS',
                   'FAILED','CANCELED','EXPIRED')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_import_owner
    ON generic_crud_import_job (owner_user_key, active_role_key, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_generic_crud_import_queue
    ON generic_crud_import_job (status, created_at)
    WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX IF NOT EXISTS ix_generic_crud_import_expiry
    ON generic_crud_import_job (expires_at)
    WHERE expires_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS generic_crud_import_row_error (
    id                         bigserial PRIMARY KEY,
    import_job_id              bigint NOT NULL,
    row_number                 bigint NOT NULL,
    operation                  varchar(20),
    business_key               varchar(1000),
    error_code                 varchar(100),
    error_message              text NOT NULL,
    field_errors_json          text,
    row_snapshot_json          text,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_generic_crud_import_error_job
        FOREIGN KEY (import_job_id)
        REFERENCES generic_crud_import_job(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_generic_crud_import_operation CHECK (
        operation IS NULL OR operation IN ('CREATE','UPDATE','DELETE','SKIP','ERROR')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_import_error_job_row
    ON generic_crud_import_row_error (import_job_id, row_number);

CREATE TABLE IF NOT EXISTS generic_crud_export_job (
    id                         bigserial PRIMARY KEY,
    job_key                    varchar(100) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    owner_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    menu_id                    bigint,
    scope_hash                 varchar(128),
    format                     varchar(10) NOT NULL,
    status                     varchar(30) NOT NULL DEFAULT 'QUEUED',
    estimated_rows             bigint,
    processed_rows             bigint NOT NULL DEFAULT 0,
    filter_snapshot_json       text,
    sort_snapshot_json         text,
    columns_snapshot_json      text,
    option_snapshot_json       text,
    result_file_reference      text,
    result_file_name           varchar(1000),
    error_summary              text,
    request_id                 varchar(100),
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    started_at                 timestamp without time zone,
    finished_at                timestamp without time zone,
    expires_at                 timestamp without time zone,
    heartbeat_at               timestamp without time zone,
    cancel_requested           boolean NOT NULL DEFAULT false,
    CONSTRAINT uq_generic_crud_export_job_key UNIQUE (job_key),
    CONSTRAINT ck_generic_crud_export_format CHECK (
        format IN ('XLSX','PDF','DOCX','PPTX')
    ),
    CONSTRAINT ck_generic_crud_export_status CHECK (
        status IN ('QUEUED','RUNNING','COMPLETED','FAILED','CANCELED','EXPIRED')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_export_owner
    ON generic_crud_export_job (owner_user_key, active_role_key, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_generic_crud_export_queue
    ON generic_crud_export_job (status, created_at)
    WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX IF NOT EXISTS ix_generic_crud_export_expiry
    ON generic_crud_export_job (expires_at)
    WHERE expires_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS generic_crud_idempotency (
    id                         bigserial PRIMARY KEY,
    idempotency_key            varchar(250) NOT NULL,
    operation_type             varchar(50) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    owner_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    request_hash               varchar(128) NOT NULL,
    status                     varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
    response_code              integer,
    response_reference         text,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    completed_at               timestamp without time zone,
    expires_at                 timestamp without time zone NOT NULL,
    CONSTRAINT uq_generic_crud_idempotency UNIQUE (
        idempotency_key, operation_type, entity_key, owner_user_key, active_role_key
    ),
    CONSTRAINT ck_generic_crud_idempotency_status CHECK (
        status IN ('IN_PROGRESS','COMPLETED','FAILED','EXPIRED')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_idempotency_expiry
    ON generic_crud_idempotency (expires_at);

CREATE TABLE IF NOT EXISTS generic_crud_audit_event (
    id                         bigserial PRIMARY KEY,
    request_id                 varchar(100),
    entity_key                 varchar(500) NOT NULL,
    object_key                 varchar(1000),
    operation                  varchar(50) NOT NULL,
    actor_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    menu_id                    bigint,
    scope_summary              text,
    source_type                varchar(30) NOT NULL DEFAULT 'UI',
    source_job_key             varchar(100),
    result_status              varchar(30) NOT NULL,
    reason                     text,
    changed_fields_json        text,
    before_snapshot_json       text,
    after_snapshot_json        text,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT ck_generic_crud_audit_source CHECK (
        source_type IN ('UI','IMPORT','CUSTOM_ACTION','APPROVAL','SYSTEM')
    ),
    CONSTRAINT ck_generic_crud_audit_result CHECK (
        result_status IN ('SUCCESS','FAILED','DENIED','PARTIAL','CONFLICT')
    )
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_audit_entity_object
    ON generic_crud_audit_event (entity_key, object_key, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_generic_crud_audit_actor
    ON generic_crud_audit_event (actor_user_key, active_role_key, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_generic_crud_audit_request
    ON generic_crud_audit_event (request_id)
    WHERE request_id IS NOT NULL;

-- Selection token untuk operasi "semua hasil filter" tanpa mengirim seluruh ID.
CREATE TABLE IF NOT EXISTS generic_crud_selection_token (
    id                         bigserial PRIMARY KEY,
    token_key                  varchar(128) NOT NULL,
    entity_key                 varchar(500) NOT NULL,
    owner_user_key             varchar(250) NOT NULL,
    active_role_key            varchar(250) NOT NULL,
    scope_hash                 varchar(128) NOT NULL,
    filter_hash                varchar(128) NOT NULL,
    filter_snapshot_json       text NOT NULL,
    sort_snapshot_json         text,
    excluded_ids_json          text,
    estimated_rows             bigint,
    created_at                 timestamp without time zone NOT NULL DEFAULT now(),
    expires_at                 timestamp without time zone NOT NULL,
    consumed_at                timestamp without time zone,
    CONSTRAINT uq_generic_crud_selection_token UNIQUE (token_key)
);

CREATE INDEX IF NOT EXISTS ix_generic_crud_selection_expiry
    ON generic_crud_selection_token (expires_at);
CREATE INDEX IF NOT EXISTS ix_generic_crud_selection_owner
    ON generic_crud_selection_token (owner_user_key, active_role_key, entity_key);

COMMIT;

-- Contoh seed aman: entity hanya REVIEW_REQUIRED dan disabled.
-- Ganti entity/class/module/page setelah scanner/runtime verifier menghasilkan data.
--
-- INSERT INTO generic_crud_entity_config
--     (entity_key, entity_class, module_key, page_key, display_name,
--      lifecycle_status, enabled, read_only)
-- SELECT
--     'ais.database.model.Agama', 'ais.database.model.Agama',
--     'root', 'agama', 'Agama', 'REVIEW_REQUIRED', false, true
-- WHERE NOT EXISTS (
--     SELECT 1 FROM generic_crud_entity_config
--     WHERE entity_key = 'ais.database.model.Agama'
-- );
