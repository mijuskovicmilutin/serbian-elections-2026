package rs.serbianelection2026.backend.ingestion.rik.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RikDocumentsResponse(List<RikDocumentRecord> records) {
}
