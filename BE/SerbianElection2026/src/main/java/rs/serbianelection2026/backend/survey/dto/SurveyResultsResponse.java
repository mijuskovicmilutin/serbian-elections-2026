package rs.serbianelection2026.backend.survey.dto;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of the survey results. {@code weighting} is null (and the weighted percentages are null) until the survey
 * has {@code minWeightedResponses} answers. Serialised as it is into {@code survey_result_snapshot.payload}.
 */
public record SurveyResultsResponse(
        Long surveyId,
        Instant computedAt,
        Instant opensAt,
        Instant closesAt,
        int responseCount,
        int minWeightedResponses,
        boolean weightedAvailable,
        WeightingInfo weighting,
        VoteIntentionResult voteIntention,
        ShareSet turnout,
        List<StructureDimension> structure) {
}
