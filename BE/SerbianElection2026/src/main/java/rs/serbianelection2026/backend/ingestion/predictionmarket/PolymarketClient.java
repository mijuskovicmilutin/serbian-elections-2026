package rs.serbianelection2026.backend.ingestion.predictionmarket;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketEventRecord;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PriceHistoryResponse;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PricePoint;
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

    /** Price history of one outcome's "Yes" token from the CLOB API; empty when Polymarket has none. */
    public List<PricePoint> fetchPriceHistory(String yesTokenId) {
        log.info("Fetching Polymarket price history: tokenId={}", yesTokenId);
        Instant start = Instant.now();

        URI uri = UriComponentsBuilder.fromUriString(properties.getClobBaseUrl())
                .path("/prices-history")
                .queryParam("market", yesTokenId)
                .queryParam("interval", "max")
                .queryParam("fidelity", properties.getHistoryFidelityMinutes())
                .build()
                .encode()
                .toUri();

        String rawBody = restClient.get().uri(uri).retrieve().body(String.class);
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("Polymarket returned an empty price history for tokenId={}", yesTokenId);
            return List.of();
        }

        PriceHistoryResponse response = objectMapper.readValue(rawBody, PriceHistoryResponse.class);
        List<PricePoint> history = response.history() == null ? List.of() : response.history();
        log.info(
                "Fetched {} price point(s) for tokenId={} in {} ms",
                history.size(),
                yesTokenId,
                Duration.between(start, Instant.now()).toMillis());
        return history;
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
