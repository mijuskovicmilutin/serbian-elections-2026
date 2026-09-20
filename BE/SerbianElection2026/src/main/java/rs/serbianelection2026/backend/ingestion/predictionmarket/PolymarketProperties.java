package rs.serbianelection2026.backend.ingestion.predictionmarket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "polymarket")
public class PolymarketProperties {

    private String baseUrl;
    private String eventSlug;
    private String clobBaseUrl;
    private int historyOutcomes = 2;
    private int historyFidelityMinutes = 720;
}
