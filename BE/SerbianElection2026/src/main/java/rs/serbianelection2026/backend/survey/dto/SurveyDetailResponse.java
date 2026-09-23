package rs.serbianelection2026.backend.survey.dto;

import java.time.Instant;
import java.util.List;

/**
 * The survey as the form needs it. Only active options are listed. {@code botCheckRequired} tells the form to show
 * the Turnstile widget and send its token.
 */
public record SurveyDetailResponse(
        Long id,
        String slug,
        String title,
        Instant opensAt,
        Instant closesAt,
        boolean acceptingAnswers,
        boolean resultsVisible,
        long responseCount,
        int minWeightedResponses,
        boolean botCheckRequired,
        List<SurveyQuestionResponse> questions) {
}
