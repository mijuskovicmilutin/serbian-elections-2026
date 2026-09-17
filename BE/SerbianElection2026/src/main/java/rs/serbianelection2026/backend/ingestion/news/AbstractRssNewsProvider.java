package rs.serbianelection2026.backend.ingestion.news;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.ingestion.news.dto.NormalizedNewsArticle;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;

/** Shared fetch-and-parse logic for RSS-based {@link NewsProvider}s; subclasses supply the feed URL and an optional category filter. */
@Slf4j
public abstract class AbstractRssNewsProvider implements NewsProvider {

    /** Feeds return dozens-to-hundreds of items; we only ever want each source's latest few. */
    private static final int MAX_ITEMS_PER_FETCH = 5;

    private final RestClient restClient;
    private final RssFeedParser rssFeedParser;

    protected AbstractRssNewsProvider(RestClient.Builder restClientBuilder, RssFeedParser rssFeedParser) {
        this.restClient = restClientBuilder.build();
        this.rssFeedParser = rssFeedParser;
    }

    protected abstract String feedUrl();

    /** Override when the feed isn't already scoped to politics and needs filtering by category. */
    protected boolean matches(RssItem item) {
        return true;
    }

    @Override
    public List<NormalizedNewsArticle> fetch() {
        log.info("Fetching {} RSS feed: {}", getSource(), feedUrl());
        String xml = restClient.get().uri(feedUrl()).retrieve().body(String.class);
        List<RssItem> items = rssFeedParser.parse(xml == null ? "" : xml);

        List<NormalizedNewsArticle> result = items.stream()
                .filter(this::matches)
                .limit(MAX_ITEMS_PER_FETCH)
                .map(this::toNormalized)
                .toList();
        log.info(
                "{} feed returned {} item(s), {} kept after filtering/capping to latest {}",
                getSource(),
                items.size(),
                result.size(),
                MAX_ITEMS_PER_FETCH);
        return result;
    }

    private NormalizedNewsArticle toNormalized(RssItem item) {
        String externalId = (item.guid() != null && !item.guid().isBlank()) ? item.guid() : item.link();
        return new NormalizedNewsArticle(
                externalId, item.title(), item.description(), item.link(), item.imageUrl(), item.publishedAt());
    }
}
