package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** One sample of an outcome's price history: {@code t} is epoch seconds, {@code p} the price (0-1). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PricePoint(long t, BigDecimal p) {
}
