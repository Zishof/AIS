BEGIN;

CREATE SCHEMA IF NOT EXISTS asset;

CREATE TABLE IF NOT EXISTS asset.procurement_document_extension (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL,
    document_type varchar(20) NOT NULL,
    legacy_document_id bigint NOT NULL,
    idempotency_key varchar(160) NOT NULL,
    source_document_type varchar(40),
    source_document_number varchar(120),
    source_location_id bigint,
    destination_location_id bigint,
    status varchar(30) NOT NULL,
    correlation_id varchar(160),
    created_at timestamp without time zone NOT NULL DEFAULT current_timestamp,
    updated_at timestamp without time zone NOT NULL DEFAULT current_timestamp,
    CONSTRAINT uq_procurement_document_extension_idempotency
        UNIQUE (tenant_id, document_type, idempotency_key),
    CONSTRAINT uq_procurement_document_extension_legacy
        UNIQUE (document_type, legacy_document_id)
);

CREATE INDEX IF NOT EXISTS idx_procurement_document_extension_source
    ON asset.procurement_document_extension
    (tenant_id, source_document_type, source_document_number);

CREATE TABLE IF NOT EXISTS asset.procurement_item_reference (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL,
    document_type varchar(20) NOT NULL,
    legacy_document_id bigint NOT NULL,
    legacy_line_id bigint NOT NULL,
    line_number integer NOT NULL,
    source_line_number integer NOT NULL,
    item_id bigint NOT NULL,
    uom_id bigint NOT NULL,
    requested_quantity numeric(18,4) NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT current_timestamp,
    CONSTRAINT ck_procurement_item_reference_quantity
        CHECK (requested_quantity > 0),
    CONSTRAINT uq_procurement_item_reference_legacy_line
        UNIQUE (document_type, legacy_line_id),
    CONSTRAINT uq_procurement_item_reference_line_number
        UNIQUE (tenant_id, document_type, legacy_document_id, line_number)
);

CREATE INDEX IF NOT EXISTS idx_procurement_item_reference_item
    ON asset.procurement_item_reference (tenant_id, item_id, uom_id);

COMMIT;
