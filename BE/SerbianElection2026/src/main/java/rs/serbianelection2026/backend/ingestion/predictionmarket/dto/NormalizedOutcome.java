package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record NormalizedOutcome(String externalId, String name, BigDecimal price, Instant updatedAt) {
}
