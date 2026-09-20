package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NormalizedPredictionMarket(
        String externalId,
        String title,
        String sourceUrl,
        BigDecimal volume,
        Instant endDate,
        List<NormalizedOutcome> outcomes) {
}
