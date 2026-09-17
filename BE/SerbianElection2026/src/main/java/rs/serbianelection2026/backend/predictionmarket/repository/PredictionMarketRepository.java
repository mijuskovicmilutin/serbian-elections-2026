package rs.serbianelection2026.backend.predictionmarket.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;

public interface PredictionMarketRepository extends JpaRepository<PredictionMarket, Long> {

    Optional<PredictionMarket> findByExternalId(String externalId);

    Optional<PredictionMarket> findFirstByOrderByIdAsc();
}
