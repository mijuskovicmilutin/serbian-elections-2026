package rs.serbianelection2026.backend.survey.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerInput(@NotNull Long questionId, @NotNull Long optionId) {
}
