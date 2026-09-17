package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import java.util.List;

public record NormalizedPredictionMarket(
        String externalId, String title, String sourceUrl, List<NormalizedOutcome> outcomes) {
}
