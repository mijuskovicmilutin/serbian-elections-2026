package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** {@code yesTokenId} is only needed to look up the price history and is not persisted. */
public record NormalizedOutcome(
        String externalId,
        String name,
        BigDecimal price,
        Instant updatedAt,
        String imageUrl,
        BigDecimal volume,
        BigDecimal oneDayPriceChange,
        BigDecimal bestAsk,
        BigDecimal bestBid,
        String yesTokenId) {
}
