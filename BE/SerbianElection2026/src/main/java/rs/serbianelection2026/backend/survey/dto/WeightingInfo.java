package rs.serbianelection2026.backend.survey.dto;

import java.util.List;

/** How the weighted figures were obtained. There is no margin of error: the sample is not random. */
public record WeightingInfo(
        double effectiveSampleSize,
        double designEffect,
        boolean converged,
        double maxDeviationPct,
        List<String> usedDimensions,
        List<String> droppedDimensions,
        boolean smallSample) {
}
