package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolymarketMarketRecord(
        String id,
        String groupItemTitle,
        String outcomePrices,
        String volume,
        Instant updatedAt,
        String image,
        BigDecimal oneDayPriceChange,
        BigDecimal bestAsk,
        BigDecimal bestBid,
        String clobTokenIds) {
}
