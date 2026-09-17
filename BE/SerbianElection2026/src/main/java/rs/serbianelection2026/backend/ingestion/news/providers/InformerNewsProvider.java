package rs.serbianelection2026.backend.ingestion.news.providers;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.AbstractRssNewsProvider;
import rs.serbianelection2026.backend.ingestion.news.RssFeedParser;

/** Informer's "politika" category feed is already scoped to politics — no extra filtering needed. */
@Component
public class InformerNewsProvider extends AbstractRssNewsProvider {

    private static final String FEED_URL = "https://informer.rs/rss/politika";

    public InformerNewsProvider(RestClient.Builder restClientBuilder, RssFeedParser rssFeedParser) {
        super(restClientBuilder, rssFeedParser);
    }

    @Override
    public ImportSource getSource() {
        return ImportSource.INFORMER;
    }

    @Override
    protected String feedUrl() {
        return FEED_URL;
    }
}
