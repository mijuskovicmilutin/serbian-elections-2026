package rs.serbianelection2026.backend.ingestion.rik.dto;

import java.time.Instant;

public record NormalizedElectoralList(
        String externalId,
        String name,
        Integer ballotNumber,
        String sourceUrl,
        Instant publishedAt) {
}
