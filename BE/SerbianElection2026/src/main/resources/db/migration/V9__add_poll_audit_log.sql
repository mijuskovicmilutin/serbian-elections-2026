CREATE TABLE poll_audit_log (
    id           BIGSERIAL PRIMARY KEY,
    poll_id      BIGINT       NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    action       VARCHAR(20)  NOT NULL,
    details      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_poll_audit_log_poll_id ON poll_audit_log (poll_id, created_at DESC);
