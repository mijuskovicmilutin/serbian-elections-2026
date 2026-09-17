package rs.serbianelection2026.backend.ingestion.predictionmarket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.NormalizedOutcome;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.NormalizedPredictionMarket;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketEventRecord;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketMarketRecord;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Polymarket seeds a multi-outcome event with dozens of untraded placeholder slots (name
 * "Person X", price stuck at 0.50, zero volume) alongside the real, named candidates. We only
 * want the latter, so any outcome with zero trading volume is dropped as noise rather than a
 * genuine market position.
 */
@Slf4j
@Component
public class PolymarketNormalizer {

    private final ObjectMapper objectMapper;

    public PolymarketNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedPredictionMarket normalize(PolymarketEventRecord event) {
        log.info("Normalizing Polymarket event: id={}, title={}", event.id(), event.title());

        List<NormalizedOutcome> outcomes = new ArrayList<>();
        int skipped = 0;
        for (PolymarketMarketRecord market : event.markets()) {
            try {
                NormalizedOutcome outcome = normalizeOutcome(market);
                if (outcome == null) {
                    skipped++;
                    continue;
                }
                outcomes.add(outcome);
            } catch (RuntimeException e) {
                skipped++;
                log.warn("Skipping unparsable Polymarket outcome: {}", market, e);
            }
        }

        log.info("Normalized {} traded outcome(s), skipped {} untraded/unparsable slot(s)", outcomes.size(), skipped);

        String sourceUrl = "https://polymarket.com/event/" + event.slug();
        return new NormalizedPredictionMarket(event.id(), event.title(), sourceUrl, outcomes);
    }

    private NormalizedOutcome normalizeOutcome(PolymarketMarketRecord market) {
        BigDecimal volume = parseDecimal(market.volume());
        if (volume == null || volume.signum() <= 0) {
            return null;
        }

        List<String> outcomePrices = objectMapper.readValue(market.outcomePrices(), new TypeReference<List<String>>() {
        });
        if (outcomePrices.isEmpty()) {
            throw new IllegalArgumentException("Missing outcomePrices for market " + market.id());
        }

        BigDecimal price = new BigDecimal(outcomePrices.get(0));
        return new NormalizedOutcome(market.id(), market.groupItemTitle(), price, market.updatedAt());
    }

    private BigDecimal parseDecimal(String raw) {
        return raw == null || raw.isBlank() ? null : new BigDecimal(raw);
    }
}
