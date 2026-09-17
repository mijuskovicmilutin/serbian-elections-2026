CREATE TABLE news_article (
    id            BIGSERIAL PRIMARY KEY,
    source        VARCHAR(50) NOT NULL,
    external_id   VARCHAR(512) NOT NULL,
    title         VARCHAR(500) NOT NULL,
    description   TEXT,
    url           VARCHAR(2048) NOT NULL,
    image_url     VARCHAR(2048),
    published_at  TIMESTAMPTZ NOT NULL,
    fetched_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_news_article_source_url ON news_article (source, url);
CREATE INDEX idx_news_article_published_at ON news_article (published_at DESC);
