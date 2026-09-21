package rs.serbianelection2026.backend.poll.dto;

import java.time.Instant;

public record AdminPollSummaryResponse(
        Long id,
        PollsterRefResponse pollster,
        String title,
        Instant publishedAt,
        String status,
        String sourceKind,
        Instant modifiedAt) {
}
