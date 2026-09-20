ALTER TABLE prediction_market
    ADD COLUMN volume   NUMERIC(18, 2),
    ADD COLUMN end_date TIMESTAMPTZ;

ALTER TABLE prediction_market_outcome
    ADD COLUMN image_url            VARCHAR(2048),
    ADD COLUMN volume               NUMERIC(18, 2),
    ADD COLUMN one_day_price_change NUMERIC(6, 4),
    ADD COLUMN best_ask             NUMERIC(6, 4),
    ADD COLUMN best_bid             NUMERIC(6, 4),
    ADD COLUMN price_history        TEXT;
