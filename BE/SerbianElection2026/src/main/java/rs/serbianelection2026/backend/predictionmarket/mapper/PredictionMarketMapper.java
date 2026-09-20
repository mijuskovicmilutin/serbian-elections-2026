package rs.serbianelection2026.backend.predictionmarket.mapper;

import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketOutcomeResponse;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketResponse;
import rs.serbianelection2026.backend.predictionmarket.dto.PricePointResponse;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class PredictionMarketMapper {

    private final ObjectMapper objectMapper;

    public PredictionMarketMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PredictionMarketResponse toResponse(PredictionMarket market, List<PredictionMarketOutcome> outcomes) {
        return new PredictionMarketResponse(
                market.getId(),
                market.getProvider().name(),
                market.getMarketName(),
                market.getSourceUrl(),
                market.getUpdatedAt(),
                market.getVolume(),
                market.getEndDate(),
                outcomes.stream().map(this::toOutcomeResponse).toList());
    }

    private PredictionMarketOutcomeResponse toOutcomeResponse(PredictionMarketOutcome outcome) {
        return new PredictionMarketOutcomeResponse(
                outcome.getName(),
                outcome.getPrice(),
                outcome.getImageUrl(),
                outcome.getVolume(),
                outcome.getOneDayPriceChange(),
                outcome.getBestAsk(),
                noPrice(outcome.getBestBid()),
                parseHistory(outcome));
    }

    /** Buying "No" costs the complement of what a "Yes" holder can sell at, i.e. 1 - best bid. */
    private BigDecimal noPrice(BigDecimal bestBid) {
        return bestBid == null ? null : BigDecimal.ONE.subtract(bestBid);
    }

    private List<PricePointResponse> parseHistory(PredictionMarketOutcome outcome) {
        if (outcome.getPriceHistory() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(outcome.getPriceHistory(), new TypeReference<List<PricePointResponse>>() {
            });
        } catch (RuntimeException e) {
            log.warn("Unreadable stored price history for outcome '{}', omitting it", outcome.getName(), e);
            return null;
        }
    }
}
