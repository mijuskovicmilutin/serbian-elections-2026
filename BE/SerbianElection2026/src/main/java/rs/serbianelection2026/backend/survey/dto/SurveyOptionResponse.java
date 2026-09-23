package rs.serbianelection2026.backend.survey.dto;

import java.time.LocalDate;

/** {@code addedOn} is only set for an option added after answers were already being collected. */
public record SurveyOptionResponse(Long id, int position, String label, String kind, LocalDate addedOn) {
}
