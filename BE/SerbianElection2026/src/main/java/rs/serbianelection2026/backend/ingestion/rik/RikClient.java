package rs.serbianelection2026.backend.ingestion.rik;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentRecord;
import rs.serbianelection2026.backend.ingestion.rik.dto.RikDocumentsResponse;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class RikClient {

    private static final int MAX_PAGES = 50;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RikProperties properties;

    public RikClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, RikProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<RikDocumentRecord> fetchElectoralLists() {
        log.info(
                "Fetching RIK electoral lists: baseUrl={}, electionRoundId={}, documentType={}",
                properties.getBaseUrl(),
                properties.getElectionRoundId(),
                properties.getElectoralListDocumentType());
        Instant start = Instant.now();

        try {
            List<RikDocumentRecord> all = new ArrayList<>();
            int page = 1;
            while (page <= MAX_PAGES) {
                log.debug("Fetching RIK page {}", page);
                RikDocumentsResponse response = fetchPage(page);
                if (response == null || response.records() == null || response.records().isEmpty()) {
                    log.debug("Page {} returned no records, stopping pagination", page);
                    break;
                }
                log.debug("Page {} returned {} record(s)", page, response.records().size());
                all.addAll(response.records());
                page++;
            }
            if (page > MAX_PAGES) {
                log.warn("Reached RIK pagination safety cap of {} pages, results may be incomplete", MAX_PAGES);
            }

            log.info(
                    "Successfully fetched {} RIK record(s) across {} page(s) in {} ms",
                    all.size(),
                    page - 1,
                    Duration.between(start, Instant.now()).toMillis());
            return all;
        } catch (RuntimeException e) {
            log.error(
                    "Failed to fetch RIK electoral lists after {} ms",
                    Duration.between(start, Instant.now()).toMillis(),
                    e);
            throw e;
        }
    }

    private RikDocumentsResponse fetchPage(int page) {
        URI uri = buildUri(page);
        String rawBody = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
        if (rawBody == null || rawBody.isBlank()) {
            log.debug("Page {} returned an empty response body from {}", page, uri);
            return null;
        }
        return objectMapper.readValue(rawBody, RikDocumentsResponse.class);
    }

    private URI buildUri(int page) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/get-additional-documents")
                .queryParam("page", page)
                .queryParam("filters[document-type]", properties.getElectoralListDocumentType())
                .queryParam("filters[election-round]", properties.getElectionRoundId())
                .queryParam("filters[municipality-id]", "")
                .queryParam("filters[election_station]", 0)
                .queryParam("filters[additional-document]", 0)
                .queryParam("order", 1)
                .queryParam("sort", 1)
                .queryParam("election_type", 2)
                .build()
                .encode()
                .toUri();
    }
}
