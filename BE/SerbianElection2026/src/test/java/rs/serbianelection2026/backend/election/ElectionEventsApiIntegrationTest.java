package rs.serbianelection2026.backend.election;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "rik.import-enabled=false",
            "news.import-enabled=false",
            "polymarket.import-enabled=false",
            "polls.discovery.enabled=false"
        })
class ElectionEventsApiIntegrationTest {

    @LocalServerPort
    private int port;

    private final JsonMapper json = JsonMapper.builder().build();

    private JsonNode get(String path) {
        return json.readTree(RestClient.create("http://localhost:" + port).get().uri(path).retrieve().body(String.class));
    }

    @Test
    void timelineIsInDateOrderEndsOnElectionDayAndEveryEventHasAnHttpSource() {
        JsonNode events = get("/api/v1/elections/current/events");

        assertThat(events.size()).isGreaterThanOrEqualTo(5);
        String previous = "";
        for (JsonNode event : events) {
            String date = event.get("eventDate").asString();
            assertThat(date.compareTo(previous)).isGreaterThanOrEqualTo(0);
            previous = date;
            assertThat(event.get("title").asString()).isNotBlank();
            assertThat(event.get("sourceUrl").asString()).startsWith("https://");
        }
        String electionDate = get("/api/v1/elections/current").get("electionDate").asString();
        JsonNode last = events.get(events.size() - 1);
        assertThat(last.get("type").asString()).isEqualTo("ELECTION_DAY");
        assertThat(last.get("eventDate").asString()).isEqualTo(electionDate);
    }
}
