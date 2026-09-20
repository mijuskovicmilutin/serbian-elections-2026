package rs.serbianelection2026.backend.predictionmarket.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code price} is the market-implied probability (0-1); {@code yesPrice}/{@code noPrice} are what buying
 * that side currently costs. {@code priceHistory} is only present for the leading outcomes.
 */
public record PredictionMarketOutcomeResponse(
        String name,
        BigDecimal price,
        String imageUrl,
        BigDecimal volume,
        BigDecimal oneDayPriceChange,
        BigDecimal yesPrice,
        BigDecimal noPrice,
        List<PricePointResponse> priceHistory) {
}
