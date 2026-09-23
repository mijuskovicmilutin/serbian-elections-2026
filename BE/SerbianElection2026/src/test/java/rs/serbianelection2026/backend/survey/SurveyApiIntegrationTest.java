package rs.serbianelection2026.backend.survey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import rs.serbianelection2026.backend.survey.entity.Survey;
import rs.serbianelection2026.backend.survey.entity.SurveyOption;
import rs.serbianelection2026.backend.survey.entity.SurveyOptionKind;
import rs.serbianelection2026.backend.survey.entity.SurveyQuestion;
import rs.serbianelection2026.backend.survey.entity.SurveyRole;
import rs.serbianelection2026.backend.survey.entity.SurveyStatus;
import rs.serbianelection2026.backend.survey.repository.SurveyOptionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyQuestionRepository;
import rs.serbianelection2026.backend.survey.repository.SurveyRepository;
import rs.serbianelection2026.backend.survey.service.SurveyResultsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs against the real local Postgres; creates its own survey (with the census category codes so the population
 * margins apply) and removes it afterwards, which cascades to everything created through the API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "rik.import-enabled=false",
            "news.import-enabled=false",
            "polymarket.import-enabled=false",
            "polls.discovery.enabled=false",
            "survey.jobs-enabled=false",
            "survey.hash-key=integration-test-key",
            "survey.trust-forwarded-header=true"
        })
class SurveyApiIntegrationTest {

    private record Reply(int status, String body) {
    }

    @LocalServerPort
    private int port;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository questionRepository;

    @Autowired
    private SurveyOptionRepository optionRepository;

    @Autowired
    private SurveyResultsService resultsService;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<SurveyRole, Long> questionIds = new EnumMap<>(SurveyRole.class);
    private final Map<String, Long> optionIds = new HashMap<>();
    private RestClient client;
    private Survey survey;

    @BeforeEach
    void setUp() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
        survey = createSurvey(Instant.now().plus(Duration.ofDays(1)), 2, 2);
    }

    @AfterEach
    void cleanUp() {
        surveyRepository.deleteAllById(createdSurveys);
    }

    private final List<Long> createdSurveys = new java.util.ArrayList<>();

    private Survey createSurvey(Instant closesAt, int minWeighted, int maxPerNetwork) {
        Survey s = surveyRepository.save(Survey.builder()
                .slug("it-" + UUID.randomUUID())
                .title("IT survey")
                .status(SurveyStatus.OPEN)
                .opensAt(Instant.now().minus(Duration.ofDays(1)))
                .closesAt(closesAt)
                .minWeightedResponses(minWeighted)
                .maxPerNetwork(maxPerNetwork)
                .build());
        createdSurveys.add(s.getId());

        int position = 1;
        for (SurveyRole role : SurveyRole.values()) {
            SurveyQuestion q = questionRepository.save(SurveyQuestion.builder()
                    .surveyId(s.getId()).position(position++).role(role).text("Question " + role).build());
            questionIds.put(role, q.getId());
        }
        option(SurveyRole.VOTE_INTENTION, "IT_A", 1, SurveyOptionKind.CHOICE, true);
        option(SurveyRole.VOTE_INTENTION, "IT_B", 2, SurveyOptionKind.CHOICE, true);
        option(SurveyRole.VOTE_INTENTION, "IT_C", 3, SurveyOptionKind.CHOICE, false);
        option(SurveyRole.VOTE_INTENTION, "UNDECIDED", 1001, SurveyOptionKind.UNDECIDED, true);
        for (String code : List.of("SURE_YES", "PROBABLY_YES", "PROBABLY_NO", "SURE_NO")) {
            option(SurveyRole.TURNOUT, code, 1, SurveyOptionKind.CHOICE, true);
        }
        for (String code : List.of("AGE_18_29", "AGE_30_44", "AGE_45_59", "AGE_60_PLUS")) {
            option(SurveyRole.AGE, code, 1, SurveyOptionKind.CHOICE, true);
        }
        for (String code : List.of("SEX_MALE", "SEX_FEMALE")) {
            option(SurveyRole.SEX, code, 1, SurveyOptionKind.CHOICE, true);
        }
        for (String code : List.of("REGION_BELGRADE", "REGION_VOJVODINA", "REGION_SUMADIJA_WEST", "REGION_SOUTH_EAST")) {
            option(SurveyRole.REGION, code, 1, SurveyOptionKind.CHOICE, true);
        }
        for (String code : List.of("SETTLEMENT_URBAN", "SETTLEMENT_OTHER")) {
            option(SurveyRole.SETTLEMENT, code, 1, SurveyOptionKind.CHOICE, true);
        }
        return s;
    }

    private void option(SurveyRole role, String code, int position, SurveyOptionKind kind, boolean active) {
        SurveyOption o = optionRepository.save(SurveyOption.builder()
                .questionId(questionIds.get(role)).position(position).code(code).label(code).kind(kind).active(active).build());
        optionIds.put(code, o.getId());
    }

    /** A complete, valid questionnaire choosing the given list. */
    private String answers(String voteCode) {
        return "{\"answers\":["
                + answer(SurveyRole.VOTE_INTENTION, voteCode) + ","
                + answer(SurveyRole.TURNOUT, "SURE_YES") + ","
                + answer(SurveyRole.AGE, "AGE_30_44") + ","
                + answer(SurveyRole.SEX, "SEX_FEMALE") + ","
                + answer(SurveyRole.REGION, "REGION_BELGRADE") + ","
                + answer(SurveyRole.SETTLEMENT, "SETTLEMENT_URBAN") + "]}";
    }

    private String answer(SurveyRole role, String optionCode) {
        return "{\"questionId\":" + questionIds.get(role) + ",\"optionId\":" + optionIds.get(optionCode) + "}";
    }

    private Reply post(String body, String network) {
        return client.post()
                .uri("/api/v1/surveys/" + survey.getId() + "/responses")
                .header("X-Forwarded-For", network)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((req, res) -> new Reply(res.getStatusCode().value(), res.bodyTo(String.class)));
    }

    private Reply get(String path) {
        return client.get().uri(path).exchange((req, res) -> new Reply(res.getStatusCode().value(), res.bodyTo(String.class)));
    }

    private JsonNode results() {
        return json.readTree(get("/api/v1/surveys/" + survey.getId() + "/results").body());
    }

    @Test
    void aValidQuestionnaireIsAcceptedAnonymouslyAndCountedInTheResults() {
        Reply reply = post(answers("IT_A"), "198.51.100.1");

        assertThat(reply.status()).isEqualTo(201);
        assertThat(json.readTree(reply.body()).get("accepted").asBoolean()).isTrue();
        assertThat(reply.body()).doesNotContain("id", "count"); // nothing that identifies the response

        JsonNode r = results();
        assertThat(r.get("responseCount").asInt()).isEqualTo(1);
        assertThat(r.get("voteIntention").get("lists").get("base").asInt()).isEqualTo(1);
        assertThat(r.get("voteIntention").get("lists").get("options").get(0).get("rawPct").asDouble()).isEqualTo(100.0);
    }

    @Test
    void incompleteOrInvalidQuestionnairesAreRejected() {
        String missingOne = answers("IT_A").replace(answer(SurveyRole.SETTLEMENT, "SETTLEMENT_URBAN"), "").replace(",]}", "]}");
        String unknownOption = answers("IT_A").replace(
                "\"optionId\":" + optionIds.get("IT_A"), "\"optionId\":999999999");
        String optionOfAnotherQuestion = answers("IT_A").replace(
                "\"optionId\":" + optionIds.get("IT_A"), "\"optionId\":" + optionIds.get("AGE_18_29"));
        String withdrawnList = answers("IT_C");
        String duplicated = answers("IT_A").replace("]}", "," + answer(SurveyRole.SEX, "SEX_MALE") + "]}");

        assertThat(post(missingOne, "198.51.100.2").status()).isEqualTo(400);
        assertThat(post(unknownOption, "198.51.100.2").status()).isEqualTo(400);
        assertThat(post(optionOfAnotherQuestion, "198.51.100.2").status()).isEqualTo(400);
        assertThat(post(withdrawnList, "198.51.100.2").status()).isEqualTo(400);
        assertThat(post(duplicated, "198.51.100.2").status()).isEqualTo(400);
        assertThat(post("{\"answers\":[]}", "198.51.100.2").status()).isEqualTo(400);
        assertThat(post("not json", "198.51.100.2").status()).isEqualTo(400);
        assertThat(results().get("responseCount").asInt()).isZero();
    }

    @Test
    void oneNetworkMayAnswerOnlyUpToTheLimitAndInvalidAnswersDoNotUseItUp() {
        String network = "198.51.100.3";

        assertThat(post("{\"answers\":[]}", network).status()).isEqualTo(400); // does not count
        assertThat(post(answers("IT_A"), network).status()).isEqualTo(201);
        assertThat(post(answers("IT_B"), network).status()).isEqualTo(201);
        Reply third = post(answers("IT_A"), network);
        assertThat(third.status()).isEqualTo(429);
        assertThat(json.readTree(third.body()).get("code").asString()).isEqualTo("TOO_MANY_REQUESTS");

        assertThat(post(answers("IT_A"), "198.51.100.4").status()).isEqualTo(201); // another network is fine
        assertThat(results().get("responseCount").asInt()).isEqualTo(3);
    }

    @Test
    void neitherTheAddressNorATimeIsStoredAnywhere() {
        String network = "203.0.113.77";
        assertThat(post(answers("IT_A"), network).status()).isEqualTo(201);

        List<String> hashes = jdbc.sql("SELECT ip_hash FROM survey_dedupe WHERE survey_id = :id")
                .param("id", survey.getId()).query(String.class).list();
        assertThat(hashes).hasSize(1);
        assertThat(hashes.get(0)).matches("[0-9a-f]{64}").doesNotContain("203");

        List<String> dedupeColumns = columns("survey_dedupe");
        assertThat(dedupeColumns).containsExactlyInAnyOrder("survey_id", "ip_hash", "count"); // no response id, no time
        assertThat(columns("survey_response")).containsExactlyInAnyOrder("id", "survey_id", "submitted_on", "excluded");
        String submittedOnType = jdbc.sql("SELECT data_type FROM information_schema.columns "
                        + "WHERE table_name = 'survey_response' AND column_name = 'submitted_on'")
                .query(String.class).single();
        assertThat(submittedOnType).isEqualTo("date"); // the day only
    }

    private List<String> columns(String table) {
        return jdbc.sql("SELECT column_name FROM information_schema.columns WHERE table_name = :t AND table_schema = current_schema()")
                .param("t", table).query(String.class).list().stream().collect(Collectors.toList());
    }

    @Test
    void aClosedSurveyTakesNoAnswersAndShowsNoResults() {
        survey = createSurvey(Instant.now().minus(Duration.ofMinutes(5)), 2, 2);

        assertThat(post(answers("IT_A"), "198.51.100.5").status()).isEqualTo(422);
        assertThat(get("/api/v1/surveys/" + survey.getId() + "/results").status()).isEqualTo(404);
        JsonNode detail = json.readTree(get("/api/v1/surveys/" + survey.getId()).body());
        assertThat(detail.get("acceptingAnswers").asBoolean()).isFalse();
        assertThat(detail.get("resultsVisible").asBoolean()).isFalse();
    }

    @Test
    void aWithdrawnListDropsOutOfTheResultsOfQuestionOne() {
        assertThat(post(answers("IT_A"), "198.51.100.6").status()).isEqualTo(201);
        assertThat(post(answers("IT_B"), "198.51.100.7").status()).isEqualTo(201);
        assertThat(results().get("voteIntention").get("lists").get("base").asInt()).isEqualTo(2);

        SurveyOption b = optionRepository.findById(optionIds.get("IT_B")).orElseThrow();
        b.setActive(false);
        optionRepository.save(b);
        resultsService.computeAndStore(survey);

        JsonNode r = results();
        assertThat(r.get("voteIntention").get("lists").get("base").asInt()).isEqualTo(1);
        assertThat(r.get("voteIntention").get("lists").get("options")).hasSize(1);
        assertThat(r.get("voteIntention").get("lists").get("options").get(0).get("rawPct").asDouble()).isEqualTo(100.0);
        assertThat(r.get("turnout").get("base").asInt()).isEqualTo(2); // still a respondent for the other questions
        assertThat(r.get("responseCount").asInt()).isEqualTo(2);
    }

    @Test
    void weightedFiguresAppearFromTheMinimumAndReportDroppedDimensions() {
        assertThat(post(answers("IT_A"), "198.51.100.8").status()).isEqualTo(201);
        assertThat(results().get("weightedAvailable").asBoolean()).isFalse();

        assertThat(post(answers("IT_B"), "198.51.100.9").status()).isEqualTo(201);
        resultsService.computeAndStore(survey);
        JsonNode r = results();

        assertThat(r.get("weightedAvailable").asBoolean()).isTrue();
        // two respondents cannot fill five per category, so every dimension is left out and said so
        assertThat(r.get("weighting").get("droppedDimensions")).hasSize(4);
        assertThat(r.get("weighting").get("smallSample").asBoolean()).isTrue();
        assertThat(r.get("structure")).hasSize(4);
    }

    @Test
    void theSurveyDetailListsQuestionsAndOnlyActiveOptions() {
        JsonNode detail = json.readTree(get("/api/v1/surveys/" + survey.getId()).body());

        assertThat(detail.get("acceptingAnswers").asBoolean()).isTrue();
        assertThat(detail.get("botCheckRequired").asBoolean()).isFalse(); // no Turnstile secret in tests
        assertThat(detail.get("questions")).hasSize(6);
        JsonNode vote = detail.get("questions").get(0);
        assertThat(vote.get("role").asString()).isEqualTo("VOTE_INTENTION");
        List<String> labels = vote.get("options").valueStream().map(o -> o.get("label").asString()).toList();
        assertThat(labels).contains("IT_A", "IT_B", "UNDECIDED").doesNotContain("IT_C");
    }

    @Test
    void theCurrentSurveyEndpointServesTheSeededSurvey() {
        Reply reply = get("/api/v1/surveys/current");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(json.readTree(reply.body()).get("questions")).hasSize(6);
    }
}
