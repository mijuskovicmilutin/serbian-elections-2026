package rs.serbianelection2026.backend.poll;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.poll.dto.PollReadiness;
import rs.serbianelection2026.backend.poll.entity.OptionKind;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.ResultBasis;
import rs.serbianelection2026.backend.poll.service.PollReadinessEvaluator;

class PollReadinessEvaluatorTest {

    private Poll.PollBuilder completePoll() {
        return Poll.builder()
                .fieldworkNote("avgust–septembar 2026.")
                .resultBasis(ResultBasis.DECIDED_VOTERS)
                .sourceUrl("https://example.org/x");
    }

    private PollResult row(String pct, OptionKind kind) {
        return PollResult.builder().rawOptionName("opt").percentage(new BigDecimal(pct)).displayOrder(1).optionKind(kind).build();
    }

    @Test
    void completePollIsReady() {
        PollReadiness r = PollReadinessEvaluator.evaluate(completePoll().build(), List.of(row("100", OptionKind.PARTY)));

        assertThat(r.ready()).isTrue();
        assertThat(r.missing()).isEmpty();
    }

    @Test
    void reportsEveryMissingPublishCondition() {
        PollReadiness r = PollReadinessEvaluator.evaluate(Poll.builder().sourceUrl(" ").build(), List.of());

        assertThat(r.ready()).isFalse();
        assertThat(r.missing()).containsExactly("PERIOD", "RESULT_BASIS", "SOURCE", "RESULTS");
    }

    @Test
    void aKnownMonthIsEnoughForThePeriodButExactDatesAreFlagged() {
        PollReadiness monthOnly = PollReadinessEvaluator.evaluate(completePoll().build(), List.of(row("100", OptionKind.PARTY)));
        assertThat(monthOnly.missing()).doesNotContain("PERIOD");
        assertThat(monthOnly.warnings()).extracting("code").contains("EXACT_DATES_MISSING");

        Poll dated = completePoll().fieldworkNote(null).fieldworkFrom(LocalDate.of(2026, 6, 10)).build();
        PollReadiness exact = PollReadinessEvaluator.evaluate(dated, List.of(row("100", OptionKind.PARTY)));
        assertThat(exact.missing()).isEmpty();
        assertThat(exact.warnings()).extracting("code").doesNotContain("EXACT_DATES_MISSING");
    }

    @Test
    void sumOffWarnsForDecidedVotersButNeverBlocks() {
        PollReadiness r = PollReadinessEvaluator.evaluate(
                completePoll().build(), List.of(row("47.2", OptionKind.PARTY), row("50.4", OptionKind.PARTY)));

        assertThat(r.ready()).isTrue();
        assertThat(r.warnings()).anySatisfy(w -> {
            assertThat(w.code()).isEqualTo("SUM_OFF");
            assertThat(w.value()).isEqualTo("97.6");
        });
    }

    @Test
    void underHundredIsFineWhenTheBasisIsNotDecidedVoters() {
        Poll allRespondents = completePoll().resultBasis(ResultBasis.ALL_RESPONDENTS).build();

        PollReadiness r = PollReadinessEvaluator.evaluate(allRespondents, List.of(row("60", OptionKind.PARTY)));

        assertThat(r.warnings()).extracting("code").doesNotContain("SUM_OFF");
    }

    @Test
    void overHundredAlwaysWarns() {
        Poll allRespondents = completePoll().resultBasis(ResultBasis.ALL_RESPONDENTS).build();

        PollReadiness r = PollReadinessEvaluator.evaluate(allRespondents, List.of(row("70", OptionKind.PARTY), row("35", OptionKind.PARTY)));

        assertThat(r.warnings()).extracting("code").contains("SUM_OFF");
    }

    @Test
    void countsOptionsWithUnknownPartnersAsAWarning() {
        PollReadiness r = PollReadinessEvaluator.evaluate(
                completePoll().build(),
                List.of(row("50", OptionKind.UNSPECIFIED), row("48", OptionKind.UNSPECIFIED), row("2", OptionKind.PARTY)));

        assertThat(r.warnings()).anySatisfy(w -> {
            assertThat(w.code()).isEqualTo("PARTNERS_UNKNOWN");
            assertThat(w.value()).isEqualTo("2");
        });
    }
}
