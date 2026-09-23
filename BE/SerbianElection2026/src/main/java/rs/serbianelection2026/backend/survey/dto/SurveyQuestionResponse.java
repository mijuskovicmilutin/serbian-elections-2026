package rs.serbianelection2026.backend.survey.dto;

import java.util.List;

public record SurveyQuestionResponse(Long id, int position, String role, String text, List<SurveyOptionResponse> options) {
}
