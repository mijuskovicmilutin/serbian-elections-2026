CREATE TABLE prediction_market (
    id            BIGSERIAL PRIMARY KEY,
    provider      VARCHAR(50) NOT NULL,
    external_id   VARCHAR(255) NOT NULL,
    market_name   VARCHAR(500) NOT NULL,
    source_url    VARCHAR(2048) NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_prediction_market_external_id ON prediction_market (external_id);

CREATE TABLE prediction_market_outcome (
    id                    BIGSERIAL PRIMARY KEY,
    prediction_market_id  BIGINT NOT NULL REFERENCES prediction_market (id),
    external_id           VARCHAR(255) NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    price                 NUMERIC(6, 4) NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_prediction_market_outcome_external_id ON prediction_market_outcome (external_id);
CREATE INDEX idx_prediction_market_outcome_market_id ON prediction_market_outcome (prediction_market_id);
