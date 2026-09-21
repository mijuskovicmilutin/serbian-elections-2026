package rs.serbianelection2026.backend.poll.dto;

import java.time.Instant;

public record PollAuditEntryResponse(String action, String details, Instant at) {
}
