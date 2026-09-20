package rs.serbianelection2026.backend.ingestion.predictionmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceHistoryResponse(List<PricePoint> history) {
}
