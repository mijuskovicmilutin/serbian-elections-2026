package rs.serbianelection2026.backend.ingestion.news.dto;

import java.time.Instant;

public record NormalizedNewsArticle(
        String externalId,
        String title,
        String description,
        String url,
        String imageUrl,
        Instant publishedAt) {
}
