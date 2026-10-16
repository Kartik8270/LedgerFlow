-- Phase 3: Settlement records uploaded from acquirer files, and the
-- reconciliation results produced by matching them against the ledger.

CREATE TABLE settlement_records (
    id                  UUID         PRIMARY KEY,
    file_name           VARCHAR(255) NOT NULL,
    external_reference  VARCHAR(255) NOT NULL,
    payment_id          UUID,
    amount_minor        BIGINT       NOT NULL,
    currency            CHAR(3)      NOT NULL,
    settled_at          TIMESTAMPTZ  NOT NULL,
    uploaded_at         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_settlement_records_payment ON settlement_records (payment_id);
CREATE INDEX idx_settlement_records_file    ON settlement_records (file_name);

CREATE TABLE reconciliation_results (
    id                      UUID        PRIMARY KEY,
    run_id                  UUID        NOT NULL,
    payment_id              UUID,
    settlement_record_id    UUID        REFERENCES settlement_records (id),
    status                  VARCHAR(20) NOT NULL,
    ledger_amount_minor     BIGINT,
    settlement_amount_minor BIGINT,
    currency                CHAR(3),
    discrepancy_minor       BIGINT,
    note                    TEXT,
    created_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reconciliation_run    ON reconciliation_results (run_id);
CREATE INDEX idx_reconciliation_status ON reconciliation_results (status);
