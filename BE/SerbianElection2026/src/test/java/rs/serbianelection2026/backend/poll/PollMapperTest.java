package rs.serbianelection2026.backend.poll;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.poll.dto.PollResponse;
import rs.serbianelection2026.backend.poll.entity.OptionKind;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.entity.PollsterKind;
import rs.serbianelection2026.backend.poll.entity.ResultBasis;
import rs.serbianelection2026.backend.poll.entity.SourceKind;
import rs.serbianelection2026.backend.poll.mapper.PollMapper;
import tools.jackson.databind.json.JsonMapper;

class PollMapperTest {

    private final PollMapper mapper = new PollMapper(JsonMapper.builder().build());

    private Poll poll(String mediaSources) {
        Pollster pollster = Pollster.builder()
                .id(1L).slug("faktor-plus").name("Faktor Plus").kind(PollsterKind.SECONDARY_VIA_MEDIA).active(true).build();
        return Poll.builder()
                .id(10L)
                .pollster(pollster)
                .title("Faktor plus: septembar")
                .publishedAt(Instant.parse("2026-09-04T08:00:00Z"))
                .fieldworkNote("avgust–septembar 2026.")
                .sampleSize(1200)
                .resultBasis(ResultBasis.DECIDED_VOTERS)
                .undecidedPct(new BigDecimal("31"))
                .sourceKind(SourceKind.SECONDARY)
                .sourceUrl("https://www.danas.rs/x")
                .mediaSources(mediaSources)
                .status(PollStatus.APPROVED)
                .reviewNote("internal note")
                .contentHash("abc")
                .sourceSnapshot("raw snapshot")
                .build();
    }

    private PollResult result(String name, String pct, int order) {
        return PollResult.builder()
                .rawOptionName(name)
                .percentage(new BigDecimal(pct))
                .displayOrder(order)
                .optionKind(OptionKind.COALITION)
                .composition("internal partners")
                .build();
    }

    @Test
    void mapsPublicFieldsKeepsUnknownAsNullAndKeepsSourceOrder() {
        PollResponse response = mapper.toResponse(
                poll("[{\"name\":\"Danas\",\"url\":\"https://www.danas.rs/x\"},{\"name\":\"RTS\",\"url\":\"https://rts.rs/y\"}]"),
                List.of(result("СНС – Александар Вучић", "47.2", 1), result("Студентска листа", "31.5", 2)));

        assertThat(response.pollster().slug()).isEqualTo("faktor-plus");
        assertThat(response.pollster().kind()).isEqualTo("SECONDARY_VIA_MEDIA");
        assertThat(response.resultBasis()).isEqualTo("DECIDED_VOTERS");
        assertThat(response.sourceKind()).isEqualTo("SECONDARY");
        assertThat(response.fieldworkFrom()).isNull();
        assertThat(response.marginOfError()).isNull();
        assertThat(response.commissionedBy()).isNull();
        assertThat(response.mediaSources()).extracting("name").containsExactly("Danas", "RTS");
        assertThat(response.results()).extracting("rawOptionName")
                .containsExactly("СНС – Александар Вучић", "Студентска листа");
        assertThat(response.results().get(0).percentage()).isEqualByComparingTo("47.2");
    }

    @Test
    void internalOptionClassificationAndReviewStateAreNotPartOfThePublicResponse() throws Exception {
        PollResponse response = mapper.toResponse(poll(null), List.of(result("СНС", "35.7", 1)));

        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(response);

        assertThat(json)
                .doesNotContain("optionKind", "composition", "internal partners")
                .doesNotContain("status", "reviewNote", "internal note", "contentHash", "sourceSnapshot", "raw snapshot");
    }

    @Test
    void missingOrCorruptMediaSourcesBecomeAnEmptyListInsteadOfBreakingTheEndpoint() {
        assertThat(mapper.toResponse(poll(null), List.of()).mediaSources()).isEmpty();
        assertThat(mapper.toResponse(poll("   "), List.of()).mediaSources()).isEmpty();
        assertThat(mapper.toResponse(poll("not json"), List.of()).mediaSources()).isEmpty();
    }

    @Test
    void unsetResultBasisStaysNull() {
        Poll draft = poll(null);
        draft.setResultBasis(null);
        draft.setFieldworkFrom(LocalDate.of(2026, 6, 10));

        PollResponse response = mapper.toResponse(draft, List.of());

        assertThat(response.resultBasis()).isNull();
        assertThat(response.fieldworkFrom()).isEqualTo(LocalDate.of(2026, 6, 10));
    }
}
