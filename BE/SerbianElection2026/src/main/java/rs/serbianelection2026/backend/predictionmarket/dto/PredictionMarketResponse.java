package rs.serbianelection2026.backend.predictionmarket.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PredictionMarketResponse(
        Long id,
        String provider,
        String marketName,
        String sourceUrl,
        Instant updatedAt,
        BigDecimal volume,
        Instant endDate,
        List<PredictionMarketOutcomeResponse> outcomes) {
}
