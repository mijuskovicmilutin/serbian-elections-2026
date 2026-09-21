package rs.serbianelection2026.backend.ingestion.poll;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.RssFeedParser;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;
import rs.serbianelection2026.backend.ingestion.poll.dto.DiscoveredItem;
import rs.serbianelection2026.backend.poll.entity.SourceKind;

/** A pollster's own RSS feed (primary source): every item already belongs to that pollster. */
@Slf4j
public class PollsterOwnFeed implements PollDiscoveryFeed {

    private final ImportSource source;
    private final String pollsterSlug;
    private final String feedUrl;
    private final RestClient restClient;
    private final RssFeedParser rssFeedParser;

    public PollsterOwnFeed(
            ImportSource source,
            String pollsterSlug,
            String feedUrl,
            RestClient restClient,
            RssFeedParser rssFeedParser) {
        this.source = source;
        this.pollsterSlug = pollsterSlug;
        this.feedUrl = feedUrl;
        this.restClient = restClient;
        this.rssFeedParser = rssFeedParser;
    }

    @Override
    public ImportSource source() {
        return source;
    }

    @Override
    public List<DiscoveredItem> fetch() {
        log.info("Fetching pollster feed: {} ({})", pollsterSlug, feedUrl);
        String xml = restClient.get().uri(feedUrl).retrieve().body(String.class);
        List<RssItem> items = rssFeedParser.parse(xml == null ? "" : xml);
        log.info("Pollster feed {} returned {} item(s)", pollsterSlug, items.size());
        return items.stream()
                .map(i -> new DiscoveredItem(
                        pollsterSlug, null, SourceKind.PRIMARY, i.title(), i.link(), i.description(), i.publishedAt()))
                .toList();
    }
}
