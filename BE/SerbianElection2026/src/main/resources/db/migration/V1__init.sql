CREATE TABLE election (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    election_date DATE NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE electoral_list (
    id            BIGSERIAL PRIMARY KEY,
    election_id   BIGINT NOT NULL REFERENCES election (id),
    name          VARCHAR(255) NOT NULL,
    ballot_number INTEGER,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('SUBMITTED', 'PROCLAIMED', 'REJECTED', 'WITHDRAWN')),
    source_url    VARCHAR(2048) NOT NULL,
    last_seen_at  TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_electoral_list_election_id ON electoral_list (election_id);

CREATE TABLE data_import (
    id              BIGSERIAL PRIMARY KEY,
    source          VARCHAR(50) NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL_SUCCESS')),
    records_found   INTEGER,
    records_created INTEGER,
    records_updated INTEGER,
    error_message   TEXT
);

CREATE INDEX idx_data_import_source_started_at ON data_import (source, started_at);
