package rs.serbianelection2026.backend.ingestion.poll;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.news.NewsProvider;
import rs.serbianelection2026.backend.ingestion.news.dto.RssItem;
import rs.serbianelection2026.backend.ingestion.poll.dto.DiscoveredItem;
import rs.serbianelection2026.backend.poll.entity.SourceKind;

/**
 * Scans the media feeds we already aggregate for articles reporting a poll (secondary source). A
 * feed that is down only costs that feed's items; the others are still scanned.
 */
@Slf4j
@Component
public class MediaPollFeed implements PollDiscoveryFeed {

    private final List<NewsProvider> providers;

    public MediaPollFeed(List<NewsProvider> providers) {
        this.providers = providers;
    }

    @Override
    public ImportSource source() {
        return ImportSource.POLL_MEDIA;
    }

    @Override
    public List<DiscoveredItem> fetch() {
        List<DiscoveredItem> items = new ArrayList<>();
        int failed = 0;
        for (NewsProvider provider : providers) {
            try {
                for (RssItem item : provider.fetchAllItems()) {
                    items.add(new DiscoveredItem(
                            null,
                            label(provider.getSource()),
                            SourceKind.SECONDARY,
                            item.title(),
                            item.link(),
                            item.description(),
                            item.publishedAt()));
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("Media feed {} unavailable for poll discovery, skipping it this run", provider.getSource(), e);
            }
        }
        if (failed == providers.size() && !providers.isEmpty()) {
            throw new IllegalStateException("All " + failed + " media feeds were unavailable");
        }
        return items;
    }

    private String label(ImportSource source) {
        return switch (source) {
            case NOVA -> "Nova.rs";
            case N1 -> "N1";
            case BLIC -> "Blic";
            case INFORMER -> "Informer";
            default -> source.name();
        };
    }
}
