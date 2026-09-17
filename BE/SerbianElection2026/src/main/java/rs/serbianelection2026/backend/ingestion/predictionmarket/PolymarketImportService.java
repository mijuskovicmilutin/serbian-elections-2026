package rs.serbianelection2026.backend.ingestion.predictionmarket;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.ingestion.entity.DataImport;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.entity.ImportStatus;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.NormalizedOutcome;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.NormalizedPredictionMarket;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketEventRecord;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketOutcomeRepository;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketRepository;

@Slf4j
@Service
public class PolymarketImportService {

    private final PolymarketClient polymarketClient;
    private final PolymarketNormalizer polymarketNormalizer;
    private final PredictionMarketRepository predictionMarketRepository;
    private final PredictionMarketOutcomeRepository predictionMarketOutcomeRepository;
    private final DataImportRepository dataImportRepository;

    public PolymarketImportService(
            PolymarketClient polymarketClient,
            PolymarketNormalizer polymarketNormalizer,
            PredictionMarketRepository predictionMarketRepository,
            PredictionMarketOutcomeRepository predictionMarketOutcomeRepository,
            DataImportRepository dataImportRepository) {
        this.polymarketClient = polymarketClient;
        this.polymarketNormalizer = polymarketNormalizer;
        this.predictionMarketRepository = predictionMarketRepository;
        this.predictionMarketOutcomeRepository = predictionMarketOutcomeRepository;
        this.dataImportRepository = dataImportRepository;
    }

    @Transactional
    public DataImport importMarket() {
        log.info("Starting Polymarket prediction market import");
        Instant start = Instant.now();

        DataImport dataImport = DataImport.builder()
                .source(ImportSource.POLYMARKET)
                .startedAt(start)
                .status(ImportStatus.RUNNING)
                .build();
        dataImport = dataImportRepository.save(dataImport);

        try {
            Optional<PolymarketEventRecord> event = polymarketClient.fetchEvent();
            if (event.isEmpty()) {
                dataImport.setFinishedAt(Instant.now());
                dataImport.setStatus(ImportStatus.SUCCESS);
                dataImport.setRecordsFound(0);
                dataImport.setRecordsCreated(0);
                dataImport.setRecordsUpdated(0);
                dataImport.setRecordsUnchanged(0);
                log.warn("Polymarket import found no matching event, nothing to import");
                return dataImportRepository.save(dataImport);
            }

            NormalizedPredictionMarket normalized = polymarketNormalizer.normalize(event.get());

            PredictionMarket market = predictionMarketRepository.findByExternalId(normalized.externalId())
                    .orElseGet(() -> PredictionMarket.builder()
                            .provider(ImportSource.POLYMARKET)
                            .externalId(normalized.externalId())
                            .build());
            market.setMarketName(normalized.title());
            market.setSourceUrl(normalized.sourceUrl());
            market.setUpdatedAt(Instant.now());
            market = predictionMarketRepository.save(market);

            int created = 0;
            int updated = 0;
            int unchanged = 0;
            for (NormalizedOutcome item : normalized.outcomes()) {
                PredictionMarketOutcome outcome = predictionMarketOutcomeRepository.findByExternalId(item.externalId())
                        .orElse(null);
                if (outcome == null) {
                    outcome = PredictionMarketOutcome.builder()
                            .predictionMarket(market)
                            .externalId(item.externalId())
                            .build();
                    applyContent(outcome, item);
                    created++;
                } else if (hasContentChanged(outcome, item)) {
                    applyContent(outcome, item);
                    updated++;
                } else {
                    unchanged++;
                }
                predictionMarketOutcomeRepository.save(outcome);
            }

            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.SUCCESS);
            dataImport.setRecordsFound(normalized.outcomes().size());
            dataImport.setRecordsCreated(created);
            dataImport.setRecordsUpdated(updated);
            dataImport.setRecordsUnchanged(unchanged);

            log.info(
                    "Polymarket import succeeded in {} ms: found={}, created={}, updated={}, unchanged={}",
                    Duration.between(start, Instant.now()).toMillis(),
                    normalized.outcomes().size(),
                    created,
                    updated,
                    unchanged);
        } catch (Exception e) {
            dataImport.setFinishedAt(Instant.now());
            dataImport.setStatus(ImportStatus.FAILED);
            dataImport.setErrorMessage(e.getMessage());
            log.error(
                    "Polymarket import failed after {} ms",
                    Duration.between(start, Instant.now()).toMillis(),
                    e);
        }

        return dataImportRepository.save(dataImport);
    }

    private boolean hasContentChanged(PredictionMarketOutcome outcome, NormalizedOutcome item) {
        return !Objects.equals(outcome.getName(), item.name())
                || outcome.getPrice().compareTo(item.price()) != 0
                || !Objects.equals(outcome.getUpdatedAt(), item.updatedAt());
    }

    private void applyContent(PredictionMarketOutcome outcome, NormalizedOutcome item) {
        outcome.setName(item.name());
        outcome.setPrice(item.price());
        outcome.setUpdatedAt(item.updatedAt());
    }
}
