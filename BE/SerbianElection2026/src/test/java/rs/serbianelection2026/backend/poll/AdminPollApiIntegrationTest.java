package rs.serbianelection2026.backend.poll;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.SourceKind;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Whole review flow over real HTTP against the local Postgres; removes the polls it creates. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "rik.import-enabled=false",
            "news.import-enabled=false",
            "polymarket.import-enabled=false",
            "polls.discovery.enabled=false",
            "admin.api-key=test-admin-key"
        })
class AdminPollApiIntegrationTest {

    private static final String KEY = "test-admin-key";

    @LocalServerPort
    private int port;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollsterRepository pollsterRepository;

    private final JsonMapper json = JsonMapper.builder().build();
    private final List<Long> createdIds = new ArrayList<>();
    private RestClient client;
    private int counter;

    private record Resp(int status, String body) {
        JsonNode node(JsonMapper m) {
            return m.readTree(body);
        }
    }

    @BeforeEach
    void setUp() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @AfterEach
    void cleanUp() {
        pollRepository.deleteAllById(createdIds);
    }

    private Resp call(HttpMethod method, String path, Object body, String key) {
        RestClient.RequestBodySpec spec = client.method(method).uri(path);
        if (key != null) {
            spec = spec.header("X-Admin-Key", key);
        }
        if (body != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(json.writeValueAsString(body));
        }
        return spec.exchange((req, res) -> new Resp(res.getStatusCode().value(), new String(res.getBody().readAllBytes())));
    }

    private Resp admin(HttpMethod method, String path, Object body) {
        return call(method, path, body, KEY);
    }

    private Map<String, Object> pollBody(boolean complete) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("pollsterSlug", "faktor-plus");
        b.put("title", "IT admin poll");
        b.put("publishedAt", "2026-09-04T08:00:00Z");
        b.put("sourceKind", "SECONDARY");
        b.put("sourceUrl", "https://it.example/admin-poll-" + (++counter) + "-" + System.nanoTime());
        b.put("fieldworkNote", "avgust–septembar 2026.");
        b.put("sampleSize", 1200);
        b.put("sourceNote", "Only the parties above the threshold are listed.");
        if (complete) {
            b.put("resultBasis", "DECIDED_VOTERS");
            b.put("undecidedPct", 31);
            b.put("mediaSources", List.of(Map.of("name", "Danas", "url", "https://www.danas.rs/x")));
            b.put("results", List.of(
                    Map.of("rawOptionName", "Second in source", "percentage", 31.5, "optionKind", "ELECTORAL_LIST"),
                    Map.of("rawOptionName", "First in source", "percentage", 47.2, "optionKind", "COALITION", "composition", "internal partners")));
        }
        return b;
    }

    private long createDraft(Map<String, Object> body) {
        Resp r = admin(HttpMethod.POST, "/internal/polls", body);
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        long id = r.node(json).get("id").asLong();
        createdIds.add(id);
        return id;
    }

    @Test
    void internalApiRequiresTheKey() {
        assertThat(call(HttpMethod.GET, "/internal/polls", null, null).status()).isEqualTo(401);
        assertThat(call(HttpMethod.GET, "/internal/polls", null, "wrong").status()).isEqualTo(401);
        assertThat(call(HttpMethod.POST, "/internal/polls", pollBody(true), null).status()).isEqualTo(401);
        assertThat(admin(HttpMethod.GET, "/internal/polls", null).status()).isEqualTo(200);
    }

    @Test
    void aNewPollIsADraftAndInvisibleToThePublic() {
        Map<String, Object> body = pollBody(true);
        long id = createDraft(body);

        JsonNode detail = admin(HttpMethod.GET, "/internal/polls/" + id, null).node(json);
        assertThat(detail.get("status").asString()).isEqualTo("DRAFT");
        assertThat(detail.get("audit").get(0).get("action").asString()).isEqualTo("CREATED");
        assertThat(detail.get("readiness").get("ready").asBoolean()).isTrue();

        assertThat(call(HttpMethod.GET, "/api/v1/polls/" + id, null, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/polls?size=100", null, null).body()).doesNotContain((String) body.get("sourceUrl"));
    }

    @Test
    void anIncompletePollCannotBeApprovedAndSaysWhatIsMissing() {
        long id = createDraft(pollBody(false));

        Resp r = admin(HttpMethod.POST, "/internal/polls/" + id + "/approve", null);

        assertThat(r.status()).isEqualTo(422);
        assertThat(r.body()).contains("RESULT_BASIS").contains("RESULTS");
        assertThat(admin(HttpMethod.GET, "/internal/polls/" + id, null).node(json).get("status").asString()).isEqualTo("DRAFT");
    }

    @Test
    void approvingPublishesInSourceOrderWithoutInternalFieldsAndRejectingWithdrawsIt() {
        long id = createDraft(pollBody(true));

        Resp approved = admin(HttpMethod.POST, "/internal/polls/" + id + "/approve", null);
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.node(json).get("status").asString()).isEqualTo("APPROVED");

        Resp publicView = call(HttpMethod.GET, "/api/v1/polls/" + id, null, null);
        assertThat(publicView.status()).isEqualTo(200);
        JsonNode results = publicView.node(json).get("results");
        assertThat(results.get(0).get("rawOptionName").asString()).isEqualTo("Second in source");
        assertThat(results.get(1).get("rawOptionName").asString()).isEqualTo("First in source");
        assertThat(publicView.body()).doesNotContain("internal partners", "composition", "optionKind");
        assertThat(publicView.node(json).get("sourceNote").asString()).isEqualTo("Only the parties above the threshold are listed.");

        assertThat(admin(HttpMethod.POST, "/internal/polls/" + id + "/reject", Map.of("note", " ")).status()).isEqualTo(400);
        Resp rejected = admin(HttpMethod.POST, "/internal/polls/" + id + "/reject", Map.of("note", "Numbers do not match the source"));
        assertThat(rejected.status()).isEqualTo(200);
        assertThat(rejected.node(json).get("status").asString()).isEqualTo("REJECTED");
        assertThat(rejected.body()).contains("Withdrawn from public");
        assertThat(call(HttpMethod.GET, "/api/v1/polls/" + id, null, null).status()).isEqualTo(404);
        assertThat(admin(HttpMethod.POST, "/internal/polls/" + id + "/reject", Map.of("note", "again")).status()).isEqualTo(422);
    }

    @Test
    void editingAPublishedPollKeepsItPublicAndLeavesAnAuditTrail() {
        long id = createDraft(pollBody(true));
        admin(HttpMethod.POST, "/internal/polls/" + id + "/approve", null);
        Map<String, Object> edited = pollBody(true);
        JsonNode current = admin(HttpMethod.GET, "/internal/polls/" + id, null).node(json);
        edited.put("sourceUrl", current.get("sourceUrl").asString());
        edited.put("title", "IT admin poll, corrected title");
        edited.put("sourceNote", "Corrected note.");

        Resp r = admin(HttpMethod.PUT, "/internal/polls/" + id, edited);

        assertThat(r.status()).as(r.body()).isEqualTo(200);
        JsonNode detail = r.node(json);
        assertThat(detail.get("status").asString()).isEqualTo("APPROVED");
        assertThat(detail.get("audit").get(0).get("action").asString()).isEqualTo("UPDATED");
        assertThat(detail.get("audit").get(0).get("details").asString())
                .contains("title: IT admin poll -> IT admin poll, corrected title")
                .contains("sourceNote: Only the parties above the threshold are listed. -> Corrected note.")
                .doesNotContain("results:");
        assertThat(call(HttpMethod.GET, "/api/v1/polls/" + id, null, null).body()).contains("corrected title");

        Resp unchanged = admin(HttpMethod.PUT, "/internal/polls/" + id, edited);
        assertThat(unchanged.node(json).get("audit").size()).isEqualTo(detail.get("audit").size());
    }

    @Test
    void invalidInputIsRejectedBeforeItCanReachThePublicSite() {
        Map<String, Object> jsUrl = pollBody(true);
        jsUrl.put("sourceUrl", "javascript:alert(1)");
        assertThat(admin(HttpMethod.POST, "/internal/polls", jsUrl).status()).isEqualTo(400);

        Map<String, Object> badMedia = pollBody(true);
        badMedia.put("mediaSources", List.of(Map.of("name", "X", "url", "data:text/html,hi")));
        assertThat(admin(HttpMethod.POST, "/internal/polls", badMedia).status()).isEqualTo(400);

        Map<String, Object> unknownPollster = pollBody(true);
        unknownPollster.put("pollsterSlug", "nope");
        assertThat(admin(HttpMethod.POST, "/internal/polls", unknownPollster).status()).isEqualTo(400);

        Map<String, Object> backwardsDates = pollBody(true);
        backwardsDates.put("fieldworkFrom", "2026-06-24");
        backwardsDates.put("fieldworkTo", "2026-06-10");
        assertThat(admin(HttpMethod.POST, "/internal/polls", backwardsDates).status()).isEqualTo(400);

        Map<String, Object> percentageOver100 = pollBody(true);
        percentageOver100.put("results", List.of(Map.of("rawOptionName", "A", "percentage", 120)));
        assertThat(admin(HttpMethod.POST, "/internal/polls", percentageOver100).status()).isEqualTo(400);

        assertThat(admin(HttpMethod.POST, "/internal/polls", "{not json").status()).isEqualTo(400);
    }

    @Test
    void theSameSourceCannotBeEnteredTwiceForAPollster() {
        Map<String, Object> body = pollBody(true);
        createDraft(body);

        assertThat(admin(HttpMethod.POST, "/internal/polls", body).status()).isEqualTo(422);
    }

    @Test
    void listCanBeFilteredByStatus() {
        long draft = createDraft(pollBody(true));
        long approved = createDraft(pollBody(true));
        admin(HttpMethod.POST, "/internal/polls/" + approved + "/approve", null);

        JsonNode pending = admin(HttpMethod.GET, "/internal/polls?status=DRAFT&status=DISCOVERED&size=200", null).node(json);
        JsonNode published = admin(HttpMethod.GET, "/internal/polls?status=APPROVED&size=200", null).node(json);

        List<Long> pendingIds = new ArrayList<>();
        pending.get("content").forEach(p -> pendingIds.add(p.get("id").asLong()));
        List<Long> publishedIds = new ArrayList<>();
        published.get("content").forEach(p -> publishedIds.add(p.get("id").asLong()));
        assertThat(pendingIds).contains(draft).doesNotContain(approved);
        assertThat(publishedIds).contains(approved).doesNotContain(draft);
    }

    @Test
    void aDiscoveredCandidateWaitsForReviewIsNeverPublicAndBecomesADraftWhenOpened() {
        Poll candidate = pollRepository.save(Poll.builder()
                .pollster(pollsterRepository.findBySlug("faktor-plus").orElseThrow())
                .title("IT discovered candidate")
                .publishedAt(Instant.parse("2026-09-18T10:00:00Z"))
                .sourceKind(SourceKind.SECONDARY)
                .sourceUrl("https://it.example/discovered-" + System.nanoTime())
                .status(PollStatus.DISCOVERED)
                .scrapedAt(Instant.now())
                .contentHash("a".repeat(64))
                .sourceSnapshot("Faktor plus: SNS bi osvojila 47,2 odsto")
                .build());
        createdIds.add(candidate.getId());
        long id = candidate.getId();

        assertThat(call(HttpMethod.GET, "/api/v1/polls/" + id, null, null).status()).isEqualTo(404);
        JsonNode pending = admin(HttpMethod.GET, "/internal/polls?status=DRAFT&status=DISCOVERED&size=200", null).node(json);
        boolean listed = false;
        for (JsonNode p : pending.get("content")) {
            listed |= p.get("id").asLong() == id && p.get("status").asString().equals("DISCOVERED");
        }
        assertThat(listed).isTrue();
        JsonNode detail = admin(HttpMethod.GET, "/internal/polls/" + id, null).node(json);
        assertThat(detail.get("sourceSnapshot").asString()).contains("47,2 odsto");
        assertThat(detail.get("readiness").get("ready").asBoolean()).isFalse();
        assertThat(admin(HttpMethod.POST, "/internal/polls/" + id + "/approve", null).status()).isEqualTo(422);

        Map<String, Object> body = pollBody(true);
        body.put("sourceUrl", candidate.getSourceUrl());
        Resp saved = admin(HttpMethod.PUT, "/internal/polls/" + id, body);

        assertThat(saved.status()).as(saved.body()).isEqualTo(200);
        assertThat(saved.node(json).get("status").asString()).isEqualTo("DRAFT");
        assertThat(saved.node(json).get("audit").get(0).get("details").asString()).contains("DISCOVERED -> DRAFT");
    }

    @Test
    void pollSourceStatusListsEveryDiscoverySourceEvenBeforeItsFirstRun() {
        JsonNode sources = admin(HttpMethod.GET, "/internal/poll-sources", null).node(json);

        List<String> names = new ArrayList<>();
        sources.forEach(s -> names.add(s.get("source").asString()));
        assertThat(names).containsExactly("POLL_CRTA", "POLL_CESID", "POLL_MEDIA");
        assertThat(call(HttpMethod.GET, "/internal/poll-sources", null, null).status()).isEqualTo(401);
    }
}
