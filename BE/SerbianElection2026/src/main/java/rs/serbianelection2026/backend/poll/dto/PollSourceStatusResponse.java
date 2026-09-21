package rs.serbianelection2026.backend.poll.dto;

import java.time.Instant;

/** State of one place we look for new polls; the time fields are null until its first check. */
public record PollSourceStatusResponse(
        String source,
        Instant lastRunAt,
        String status,
        Integer recordsFound,
        Integer recordsCreated,
        String errorMessage) {
}
