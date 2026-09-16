package rs.serbianelection2026.backend.election.dto;

import java.time.LocalDate;

public record ElectionResponse(Long id, String name, LocalDate electionDate) {
}
