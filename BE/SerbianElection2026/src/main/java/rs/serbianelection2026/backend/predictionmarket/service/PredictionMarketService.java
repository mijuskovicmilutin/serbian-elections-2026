package rs.serbianelection2026.backend.predictionmarket.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketOutcomeRepository;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketRepository;

@Slf4j
@Service
public class PredictionMarketService {

    private final PredictionMarketRepository predictionMarketRepository;
    private final PredictionMarketOutcomeRepository predictionMarketOutcomeRepository;

    public PredictionMarketService(
            PredictionMarketRepository predictionMarketRepository,
            PredictionMarketOutcomeRepository predictionMarketOutcomeRepository) {
        this.predictionMarketRepository = predictionMarketRepository;
        this.predictionMarketOutcomeRepository = predictionMarketOutcomeRepository;
    }

    @Transactional(readOnly = true)
    public PredictionMarket getCurrentMarket() {
        log.info("Fetching current prediction market");

        PredictionMarket market = predictionMarketRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> {
                    log.warn("No prediction market found in the database");
                    return new NotFoundException("No prediction market available yet");
                });

        log.info("Successfully fetched prediction market: id={}, name={}", market.getId(), market.getMarketName());
        return market;
    }

    @Transactional(readOnly = true)
    public List<PredictionMarketOutcome> getOutcomes(Long marketId) {
        return predictionMarketOutcomeRepository.findByPredictionMarket_IdOrderByPriceDesc(marketId);
    }
}
