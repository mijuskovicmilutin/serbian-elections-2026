package rs.serbianelection2026.backend.ingestion.poll.dto;

import java.time.Instant;
import rs.serbianelection2026.backend.poll.entity.SourceKind;

/**
 * One feed item that may announce a poll. {@code pollsterSlug} is known for a pollster's own feed and
 * null for media feeds, where the pollster is recognised from the text; {@code mediaName} is set for media.
 */
public record DiscoveredItem(
        String pollsterSlug,
        String mediaName,
        SourceKind sourceKind,
        String title,
        String link,
        String description,
        Instant publishedAt) {
}
