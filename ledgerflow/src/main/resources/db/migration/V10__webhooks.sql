-- Phase 3: Webhook subscriptions and delivery tracking.
-- Merchants register HTTPS endpoints to receive signed event notifications.
-- Each delivery attempt is tracked with retry state and dead-letter status.

CREATE TABLE webhook_subscriptions (
    id         UUID          PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    url        VARCHAR(2048) NOT NULL,
    secret     VARCHAR(255)  NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ   NOT NULL
);

CREATE TABLE webhook_deliveries (
    id               UUID        PRIMARY KEY,
    subscription_id  UUID        NOT NULL REFERENCES webhook_subscriptions (id),
    event_id         UUID        NOT NULL REFERENCES payment_events (event_id),
    event_type       VARCHAR(64) NOT NULL,
    payload          TEXT        NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_count    INTEGER     NOT NULL DEFAULT 0,
    last_http_status INTEGER,
    last_error       TEXT,
    next_retry_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL,
    delivered_at     TIMESTAMPTZ
);

CREATE INDEX idx_webhook_deliveries_pending
    ON webhook_deliveries (next_retry_at)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_webhook_deliveries_subscription
    ON webhook_deliveries (subscription_id);
CREATE INDEX idx_webhook_deliveries_event
    ON webhook_deliveries (event_id);
