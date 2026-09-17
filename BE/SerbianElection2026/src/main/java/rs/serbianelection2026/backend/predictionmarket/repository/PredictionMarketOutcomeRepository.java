package rs.serbianelection2026.backend.predictionmarket.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;

public interface PredictionMarketOutcomeRepository extends JpaRepository<PredictionMarketOutcome, Long> {

    Optional<PredictionMarketOutcome> findByExternalId(String externalId);

    List<PredictionMarketOutcome> findByPredictionMarket_IdOrderByPriceDesc(Long predictionMarketId);
}
