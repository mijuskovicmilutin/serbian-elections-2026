package rs.serbianelection2026.backend.ingestion.predictionmarket;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PricePoint;
import rs.serbianelection2026.backend.ingestion.repository.DataImportRepository;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketOutcomeRepository;
import rs.serbianelection2026.backend.predictionmarket.repository.PredictionMarketRepository;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class PolymarketImportService {

    private final PolymarketClient polymarketClient;
    private final PolymarketNormalizer polymarketNormalizer;
    private final PredictionMarketRepository predictionMarketRepository;
    private final PredictionMarketOutcomeRepository predictionMarketOutcomeRepository;
    private final DataImportRepository dataImportRepository;
    private final PolymarketProperties properties;
    private final ObjectMapper objectMapper;

    public PolymarketImportService(
            PolymarketClient polymarketClient,
            PolymarketNormalizer polymarketNormalizer,
            PredictionMarketRepository predictionMarketRepository,
            PredictionMarketOutcomeRepository predictionMarketOutcomeRepository,
            DataImportRepository dataImportRepository,
            PolymarketProperties properties,
            ObjectMapper objectMapper) {
        this.polymarketClient = polymarketClient;
        this.polymarketNormalizer = polymarketNormalizer;
        this.predictionMarketRepository = predictionMarketRepository;
        this.predictionMarketOutcomeRepository = predictionMarketOutcomeRepository;
        this.dataImportRepository = dataImportRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
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
            market.setVolume(normalized.volume());
            market.setEndDate(normalized.endDate());
            market = predictionMarketRepository.save(market);

            Set<String> chartedIds = leadingOutcomeIds(normalized.outcomes());

            int created = 0;
            int updated = 0;
            int unchanged = 0;
            for (NormalizedOutcome item : normalized.outcomes()) {
                String priceHistory = chartedIds.contains(item.externalId()) ? fetchHistoryJson(item) : null;
                PredictionMarketOutcome outcome = predictionMarketOutcomeRepository.findByExternalId(item.externalId())
                        .orElse(null);
                if (outcome == null) {
                    outcome = PredictionMarketOutcome.builder()
                            .predictionMarket(market)
                            .externalId(item.externalId())
                            .build();
                    applyContent(outcome, item, priceHistory);
                    created++;
                } else if (hasContentChanged(outcome, item, priceHistory)) {
                    applyContent(outcome, item, priceHistory);
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

    private Set<String> leadingOutcomeIds(List<NormalizedOutcome> outcomes) {
        Set<String> ids = new HashSet<>();
        outcomes.stream()
                .sorted(Comparator.comparing(NormalizedOutcome::price).reversed())
                .limit(properties.getHistoryOutcomes())
                .forEach(outcome -> ids.add(outcome.externalId()));
        return ids;
    }

    /** A failed history lookup must not fail the whole import, so it degrades to "no chart data". */
    private String fetchHistoryJson(NormalizedOutcome item) {
        if (item.yesTokenId() == null) {
            return null;
        }
        try {
            List<PricePoint> history = polymarketClient.fetchPriceHistory(item.yesTokenId());
            return history.isEmpty() ? null : objectMapper.writeValueAsString(history);
        } catch (RuntimeException e) {
            log.warn("Could not fetch price history for outcome {} ({}), skipping chart data", item.name(), item.externalId(), e);
            return null;
        }
    }

    private boolean hasContentChanged(PredictionMarketOutcome outcome, NormalizedOutcome item, String priceHistory) {
        return !Objects.equals(outcome.getName(), item.name())
                || outcome.getPrice().compareTo(item.price()) != 0
                || !Objects.equals(outcome.getUpdatedAt(), item.updatedAt())
                || !Objects.equals(outcome.getImageUrl(), item.imageUrl())
                || differs(outcome.getVolume(), item.volume())
                || differs(outcome.getOneDayPriceChange(), item.oneDayPriceChange())
                || differs(outcome.getBestAsk(), item.bestAsk())
                || differs(outcome.getBestBid(), item.bestBid())
                || !Objects.equals(outcome.getPriceHistory(), priceHistory);
    }

    private boolean differs(BigDecimal stored, BigDecimal incoming) {
        if (stored == null || incoming == null) {
            return stored != incoming;
        }
        return stored.compareTo(incoming) != 0;
    }

    private void applyContent(PredictionMarketOutcome outcome, NormalizedOutcome item, String priceHistory) {
        outcome.setName(item.name());
        outcome.setPrice(item.price());
        outcome.setUpdatedAt(item.updatedAt());
        outcome.setImageUrl(item.imageUrl());
        outcome.setVolume(item.volume());
        outcome.setOneDayPriceChange(item.oneDayPriceChange());
        outcome.setBestAsk(item.bestAsk());
        outcome.setBestBid(item.bestBid());
        outcome.setPriceHistory(priceHistory);
    }
}
