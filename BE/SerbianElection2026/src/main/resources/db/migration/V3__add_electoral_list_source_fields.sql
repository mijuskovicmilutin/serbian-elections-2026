ALTER TABLE electoral_list
    ADD COLUMN external_id  VARCHAR(255) NOT NULL,
    ADD COLUMN published_at TIMESTAMPTZ NOT NULL;

CREATE UNIQUE INDEX idx_electoral_list_external_id ON electoral_list (external_id);
