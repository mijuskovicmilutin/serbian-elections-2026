package rs.serbianelection2026.backend.poll;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.poll.entity.OptionKind;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.entity.ResultBasis;
import rs.serbianelection2026.backend.poll.entity.SourceKind;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollResultRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Runs against the real local Postgres (like the context test); creates and removes only its own rows. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {"rik.import-enabled=false", "news.import-enabled=false", "polymarket.import-enabled=false",
            "polls.discovery.enabled=false"})
class PollApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollResultRepository pollResultRepository;

    @Autowired
    private PollsterRepository pollsterRepository;

    private final JsonMapper json = JsonMapper.builder().build();
    private final List<Long> createdPollIds = new ArrayList<>();
    private RestClient client;
    private Long approvedId;
    private Long draftId;
    private Long rejectedId;

    @BeforeEach
    void createPolls() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
        Pollster crta = pollsterRepository.findBySlug("crta").orElseThrow();
        Pollster faktor = pollsterRepository.findBySlug("faktor-plus").orElseThrow();

        approvedId = createPoll(crta, PollStatus.APPROVED, "IT approved", "2026-07-15T08:00:00Z");
        createPoll(faktor, PollStatus.APPROVED, "IT approved newer", "2026-09-04T08:00:00Z");
        draftId = createPoll(crta, PollStatus.DRAFT, "IT draft", "2026-09-10T08:00:00Z");
        rejectedId = createPoll(crta, PollStatus.REJECTED, "IT rejected", "2026-09-11T08:00:00Z");
    }

    @AfterEach
    void cleanUp() {
        pollRepository.deleteAllById(createdPollIds);
    }

    private Long createPoll(Pollster pollster, PollStatus status, String title, String publishedAt) {
        Poll poll = pollRepository.save(Poll.builder()
                .pollster(pollster)
                .title(title)
                .publishedAt(Instant.parse(publishedAt))
                .resultBasis(ResultBasis.DECIDED_VOTERS)
                .sourceKind(SourceKind.PRIMARY)
                .sourceUrl("https://it.example/" + title.replace(' ', '-'))
                .status(status)
                .reviewNote("secret review note")
                .sourceSnapshot("secret snapshot")
                .build());
        createdPollIds.add(poll.getId());
        pollResultRepository.save(PollResult.builder()
                .poll(poll).rawOptionName("Option B").percentage(new BigDecimal("10.5")).displayOrder(2)
                .optionKind(OptionKind.COALITION).composition("secret partners").build());
        pollResultRepository.save(PollResult.builder()
                .poll(poll).rawOptionName("Option A").percentage(new BigDecimal("44.9")).displayOrder(1)
                .optionKind(OptionKind.UNSPECIFIED).build());
        return poll.getId();
    }

    private JsonNode get(String path) {
        return json.readTree(client.get().uri(path).retrieve().body(String.class));
    }

    private HttpStatusCode statusOf(String path) {
        return client.get().uri(path).exchange((req, res) -> res.getStatusCode());
    }

    @Test
    void listShowsOnlyApprovedPollsNewestFirstWithResultsInSourceOrder() {
        JsonNode page = get("/api/v1/polls?size=50");

        List<String> titles = new ArrayList<>();
        page.get("content").forEach(p -> titles.add(p.get("title").asString()));
        assertThat(titles).containsSubsequence("IT approved newer", "IT approved");
        assertThat(titles).doesNotContain("IT draft", "IT rejected");

        JsonNode first = null;
        for (JsonNode p : page.get("content")) {
            if (p.get("title").asString().equals("IT approved")) {
                first = p;
            }
        }
        assertThat(first).isNotNull();
        assertThat(first.get("pollster").get("slug").asString()).isEqualTo("crta");
        assertThat(first.get("results").get(0).get("rawOptionName").asString()).isEqualTo("Option A");
        assertThat(first.get("results").get(1).get("rawOptionName").asString()).isEqualTo("Option B");
    }

    @Test
    void filtersByPollsterSlug() {
        JsonNode page = get("/api/v1/polls?pollster=faktor-plus&size=50");

        page.get("content").forEach(p -> assertThat(p.get("pollster").get("slug").asString()).isEqualTo("faktor-plus"));
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void publicResponseNeverLeaksInternalFields() {
        String body = client.get().uri("/api/v1/polls/" + approvedId).retrieve().body(String.class);

        assertThat(body).contains("Option A");
        assertThat(body)
                .doesNotContain("secret partners", "secret review note", "secret snapshot")
                .doesNotContain("optionKind", "composition", "reviewNote", "sourceSnapshot", "contentHash");
    }

    @Test
    void draftRejectedAndUnknownPollsAreNotFoundIndistinguishably() {
        assertThat(statusOf("/api/v1/polls/" + draftId).value()).isEqualTo(404);
        assertThat(statusOf("/api/v1/polls/" + rejectedId).value()).isEqualTo(404);
        assertThat(statusOf("/api/v1/polls/999999999").value()).isEqualTo(404);
        assertThat(statusOf("/api/v1/polls?pollster=does-not-exist").value()).isEqualTo(404);
    }

    @Test
    void pollstersListCountsOnlyApprovedPolls() {
        JsonNode pollsters = get("/api/v1/pollsters");

        assertThat(pollsters.size()).isGreaterThanOrEqualTo(3);
        JsonNode cesid = null;
        JsonNode crta = null;
        for (JsonNode p : pollsters) {
            if (p.get("slug").asString().equals("cesid")) {
                cesid = p;
            }
            if (p.get("slug").asString().equals("crta")) {
                crta = p;
            }
        }
        assertThat(cesid).isNotNull();
        assertThat(cesid.get("approvedPollCount").asLong()).isZero();
        // crta has 1 approved + 1 draft + 1 rejected from this test: only the approved one counts.
        assertThat(crta.get("approvedPollCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(crta.get("approvedPollCount").asLong()).isLessThan(3);
    }
}
