package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolymarketEventRecord(
        String id,
        String slug,
        String title,
        BigDecimal volume,
        Instant endDate,
        List<PolymarketMarketRecord> markets) {
}
