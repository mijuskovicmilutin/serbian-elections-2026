package rs.serbianelection2026.backend.ingestion.predictionmarket;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketEventRecord;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class PolymarketClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PolymarketProperties properties;

    public PolymarketClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, PolymarketProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Optional<PolymarketEventRecord> fetchEvent() {
        log.info(
                "Fetching Polymarket event: baseUrl={}, eventSlug={}", properties.getBaseUrl(), properties.getEventSlug());
        Instant start = Instant.now();

        try {
            String rawBody = restClient.get().uri(buildUri()).retrieve().body(String.class);
            if (rawBody == null || rawBody.isBlank()) {
                log.warn("Polymarket returned an empty response body for slug={}", properties.getEventSlug());
                return Optional.empty();
            }

            PolymarketEventRecord[] events = objectMapper.readValue(rawBody, PolymarketEventRecord[].class);
            log.info(
                    "Successfully fetched Polymarket event(s): count={} in {} ms",
                    events.length,
                    Duration.between(start, Instant.now()).toMillis());
            return events.length > 0 ? Optional.of(events[0]) : Optional.empty();
        } catch (RuntimeException e) {
            log.error(
                    "Failed to fetch Polymarket event after {} ms",
                    Duration.between(start, Instant.now()).toMillis(),
                    e);
            throw e;
        }
    }

    private URI buildUri() {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/events")
                .queryParam("slug", properties.getEventSlug())
                .build()
                .encode()
                .toUri();
    }
}
