package rs.serbianelection2026.backend.predictionmarket.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketOutcomeResponse;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketResponse;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;

@Component
public class PredictionMarketMapper {

    public PredictionMarketResponse toResponse(PredictionMarket market, List<PredictionMarketOutcome> outcomes) {
        return new PredictionMarketResponse(
                market.getId(),
                market.getProvider().name(),
                market.getMarketName(),
                market.getSourceUrl(),
                market.getUpdatedAt(),
                outcomes.stream().map(this::toOutcomeResponse).toList());
    }

    private PredictionMarketOutcomeResponse toOutcomeResponse(PredictionMarketOutcome outcome) {
        return new PredictionMarketOutcomeResponse(outcome.getName(), outcome.getPrice());
    }
}
