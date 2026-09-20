CREATE TABLE pollster (
    id             BIGSERIAL PRIMARY KEY,
    slug           VARCHAR(50)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    kind           VARCHAR(30)  NOT NULL,
    website        VARCHAR(2048),
    discovery_url  VARCHAR(2048),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_pollster_slug ON pollster (slug);

INSERT INTO pollster (slug, name, kind, website)
VALUES ('crta', 'CRTA', 'PRIMARY', 'https://crta.rs'),
       ('faktor-plus', 'Faktor Plus', 'SECONDARY_VIA_MEDIA', NULL),
       ('cesid', 'CeSID', 'PRIMARY', 'https://www.cesid.rs');

CREATE TABLE poll (
    id                     BIGSERIAL PRIMARY KEY,
    pollster_id            BIGINT        NOT NULL REFERENCES pollster (id),
    title                  VARCHAR(500)  NOT NULL,
    published_at           TIMESTAMPTZ   NOT NULL,
    fieldwork_from         DATE,
    fieldwork_to           DATE,
    fieldwork_note         VARCHAR(255),
    sample_size            INTEGER,
    population             VARCHAR(255),
    method                 VARCHAR(255),
    conducted_by           VARCHAR(255),
    commissioned_by        VARCHAR(255),
    margin_of_error        NUMERIC(5, 2),
    result_basis           VARCHAR(30),
    decided_share_pct      NUMERIC(5, 2),
    undecided_pct          NUMERIC(5, 2),
    wont_vote_pct          NUMERIC(5, 2),
    will_vote_pct          NUMERIC(5, 2),
    source_kind            VARCHAR(20)   NOT NULL,
    source_url             VARCHAR(2048) NOT NULL,
    original_document_url  VARCHAR(2048),
    media_sources          TEXT,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    reviewed_at            TIMESTAMPTZ,
    review_note            TEXT,
    scraped_at             TIMESTAMPTZ,
    content_hash           VARCHAR(64),
    source_snapshot        TEXT,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_poll_pollster_source_url ON poll (pollster_id, source_url);
CREATE INDEX idx_poll_status_published_at ON poll (status, published_at DESC);

CREATE TABLE poll_result (
    id                 BIGSERIAL PRIMARY KEY,
    poll_id            BIGINT        NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    raw_option_name    VARCHAR(500)  NOT NULL,
    percentage         NUMERIC(5, 2) NOT NULL,
    display_order      INTEGER       NOT NULL,
    option_kind        VARCHAR(20)   NOT NULL DEFAULT 'UNSPECIFIED',
    composition        TEXT,
    electoral_list_id  BIGINT REFERENCES electoral_list (id),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_poll_result_poll_id ON poll_result (poll_id, display_order);
