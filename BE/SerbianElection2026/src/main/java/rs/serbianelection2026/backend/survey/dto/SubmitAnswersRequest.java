package rs.serbianelection2026.backend.survey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** One answer per question. {@code turnstileToken} is required when the bot check is switched on. */
public record SubmitAnswersRequest(
        @NotEmpty @Size(max = 20) List<@Valid AnswerInput> answers, @Size(max = 4096) String turnstileToken) {
}
