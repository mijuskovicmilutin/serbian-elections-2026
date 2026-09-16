package rs.serbianelection2026.backend.ingestion.rik.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RikDocumentRecord(
        String number,
        String documentName,
        String datetime,
        List<String> extfilesLink) {
}
