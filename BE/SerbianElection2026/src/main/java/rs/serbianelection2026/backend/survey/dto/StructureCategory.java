package rs.serbianelection2026.backend.survey.dto;

public record StructureCategory(String code, String label, int sampleCount, Double samplePct, Double populationPct) {
}
