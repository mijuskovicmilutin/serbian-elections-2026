package rs.serbianelection2026.backend.ingestion.news.dto;

import java.time.Instant;
import java.util.List;

public record RssItem(
        String title,
        String link,
        String guid,
        String description,
        Instant publishedAt,
        String imageUrl,
        List<String> categories) {
}
