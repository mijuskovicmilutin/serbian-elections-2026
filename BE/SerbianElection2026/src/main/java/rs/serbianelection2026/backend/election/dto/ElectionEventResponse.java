package rs.serbianelection2026.backend.election.dto;

import java.time.LocalDate;

public record ElectionEventResponse(
        Long id, String type, String title, String description, LocalDate eventDate, String sourceUrl) {
}
