package rs.serbianelection2026.backend.survey.dto;

/** {@code rawPct} / {@code weightedPct} are percentages of the base; null when the base is empty or not weighted. */
public record OptionShare(
        Long optionId, String label, int position, String kind, int count, Double rawPct, Double weightedPct) {
}
