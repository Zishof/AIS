BEGIN;

CREATE SCHEMA IF NOT EXISTS inventory_distribution;

CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document (
    id bigserial PRIMARY KEY,
    toko_id bigint NOT NULL,
    document_type varchar(50) NOT NULL,
    document_no varchar(80) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    reference_no varchar(120),
    origin_name varchar(180),
    destination_name varchar(180),
    origin_toko_id bigint,
    destination_toko_id bigint,
    carrier_name varchar(180),
    tracking_no varchar(120),
    planned_at timestamp,
    actual_at timestamp,
    notes text,
    client_mutation_id varchar(100),
    created_by varchar(100),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_by varchar(100),
    updated_at timestamp NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_distribution_document_no
        UNIQUE (toko_id, document_type, document_no)
);

ALTER TABLE inventory_distribution.distribution_document
    ADD COLUMN IF NOT EXISTS origin_toko_id bigint;
ALTER TABLE inventory_distribution.distribution_document
    ADD COLUMN IF NOT EXISTS destination_toko_id bigint;

CREATE UNIQUE INDEX IF NOT EXISTS uq_distribution_document_mutation
    ON inventory_distribution.distribution_document(toko_id, client_mutation_id)
    WHERE client_mutation_id IS NOT NULL AND client_mutation_id <> '';

CREATE INDEX IF NOT EXISTS ix_distribution_document_list
    ON inventory_distribution.distribution_document
       (toko_id, document_type, updated_at DESC);

CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document_line (
    id bigserial PRIMARY KEY,
    document_id bigint NOT NULL
        REFERENCES inventory_distribution.distribution_document(id)
        ON DELETE CASCADE,
    line_no integer NOT NULL,
    item_id bigint,
    source_product_id bigint,
    destination_product_id bigint,
    item_code varchar(100),
    item_name varchar(255) NOT NULL,
    qty numeric(24,6) NOT NULL DEFAULT 0,
    uom varchar(50),
    notes text,
    CONSTRAINT uq_distribution_document_line UNIQUE (document_id, line_no)
);

ALTER TABLE inventory_distribution.distribution_document_line
    ADD COLUMN IF NOT EXISTS source_product_id bigint;
ALTER TABLE inventory_distribution.distribution_document_line
    ADD COLUMN IF NOT EXISTS destination_product_id bigint;

CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document_event (
    id bigserial PRIMARY KEY,
    document_id bigint NOT NULL
        REFERENCES inventory_distribution.distribution_document(id)
        ON DELETE CASCADE,
    from_status varchar(30),
    to_status varchar(30) NOT NULL,
    notes text,
    actor_id varchar(100),
    event_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_distribution_document_event
    ON inventory_distribution.distribution_document_event
       (document_id, event_at DESC);

CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_stock_posting (
    id bigserial PRIMARY KEY,
    document_id bigint NOT NULL
        REFERENCES inventory_distribution.distribution_document(id),
    line_id bigint NOT NULL
        REFERENCES inventory_distribution.distribution_document_line(id),
    direction varchar(20) NOT NULL,
    legacy_mutation_id bigint NOT NULL,
    source_toko_id bigint NOT NULL,
    destination_toko_id bigint NOT NULL,
    source_product_id bigint NOT NULL,
    destination_product_id bigint NOT NULL,
    qty numeric(24,6) NOT NULL,
    created_by varchar(100),
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_distribution_stock_posting
        UNIQUE (document_id, line_id, direction)
);

CREATE INDEX IF NOT EXISTS ix_distribution_stock_posting_mutation
    ON inventory_distribution.distribution_stock_posting(legacy_mutation_id);

COMMIT;
