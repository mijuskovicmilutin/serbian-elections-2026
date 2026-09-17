package rs.serbianelection2026.backend.ingestion.news.providers;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.AbstractRssNewsProvider;
import rs.serbianelection2026.backend.ingestion.news.RssFeedParser;

/**
 * N1 has no separate "Politika" category (verified: neither a category tag nor a category page
 * exist on their site) — "Vesti" (general/national news) is the closest section they offer, so
 * it's used as-is without further filtering.
 */
@Component
public class N1NewsProvider extends AbstractRssNewsProvider {

    private static final String FEED_URL = "https://n1info.rs/vesti/feed/";

    public N1NewsProvider(RestClient.Builder restClientBuilder, RssFeedParser rssFeedParser) {
        super(restClientBuilder, rssFeedParser);
    }

    @Override
    public ImportSource getSource() {
        return ImportSource.N1;
    }

    @Override
    protected String feedUrl() {
        return FEED_URL;
    }
}
