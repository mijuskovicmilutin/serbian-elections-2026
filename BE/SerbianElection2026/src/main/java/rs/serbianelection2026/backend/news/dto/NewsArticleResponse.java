package rs.serbianelection2026.backend.news.dto;

import java.time.Instant;

public record NewsArticleResponse(
        Long id,
        String source,
        String title,
        String description,
        String url,
        String imageUrl,
        Instant publishedAt,
        Instant fetchedAt) {
}
