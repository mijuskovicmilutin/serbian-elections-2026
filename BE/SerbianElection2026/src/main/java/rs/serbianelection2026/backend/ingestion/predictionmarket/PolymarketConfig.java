package rs.serbianelection2026.backend.ingestion.predictionmarket;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PolymarketProperties.class)
public class PolymarketConfig {
}
