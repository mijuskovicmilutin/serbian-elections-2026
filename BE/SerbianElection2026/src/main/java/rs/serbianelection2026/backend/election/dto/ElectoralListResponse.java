package rs.serbianelection2026.backend.election.dto;

import java.time.Instant;

public record ElectoralListResponse(
        Long id,
        String name,
        Integer ballotNumber,
        String status,
        String sourceUrl,
        Instant publishedAt,
        Instant lastSeenAt) {
}
