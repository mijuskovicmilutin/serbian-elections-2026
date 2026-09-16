package rs.serbianelection2026.backend.ingestion.rik;

import java.net.URI;
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
        List<RikDocumentRecord> all = new ArrayList<>();
        int page = 1;
        while (page <= MAX_PAGES) {
            RikDocumentsResponse response = fetchPage(page);
            if (response == null || response.records() == null || response.records().isEmpty()) {
                break;
            }
            all.addAll(response.records());
            page++;
        }
        if (page > MAX_PAGES) {
            log.warn("Reached RIK pagination safety cap of {} pages, results may be incomplete", MAX_PAGES);
        }
        return all;
    }

    private RikDocumentsResponse fetchPage(int page) {
        String rawBody = restClient.get()
                .uri(buildUri(page))
                .retrieve()
                .body(String.class);
        if (rawBody == null || rawBody.isBlank()) {
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
