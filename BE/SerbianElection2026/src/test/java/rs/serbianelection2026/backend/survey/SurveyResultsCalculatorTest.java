package rs.serbianelection2026.backend.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.survey.dto.OptionShare;
import rs.serbianelection2026.backend.survey.dto.StructureDimension;
import rs.serbianelection2026.backend.survey.dto.SurveyResultsResponse;
import rs.serbianelection2026.backend.survey.entity.SurveyOptionKind;
import rs.serbianelection2026.backend.survey.entity.SurveyRole;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.CategoryInfo;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Input;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Meta;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.OptionInfo;
import rs.serbianelection2026.backend.survey.weighting.SurveyResultsCalculator.Respondent;

class SurveyResultsCalculatorTest {

    private static final long LIST_A = 1;
    private static final long LIST_B = 2;
    private static final long LIST_C_WITHDRAWN = 3;
    private static final long UNDECIDED = 4;

    private final SurveyResultsCalculator calculator = new SurveyResultsCalculator(new RakingWeighter());
    private final Meta meta = new Meta(1L, Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-09-23T00:00:00Z"), Instant.parse("2026-10-22T22:00:00Z"));

    private static List<OptionInfo> voteOptions() {
        return List.of(
                new OptionInfo(LIST_A, "LIST_1", "List A", 1, SurveyOptionKind.CHOICE, true),
                new OptionInfo(LIST_B, "LIST_2", "List B", 2, SurveyOptionKind.CHOICE, true),
                new OptionInfo(LIST_C_WITHDRAWN, "LIST_3", "List C", 3, SurveyOptionKind.CHOICE, false),
                new OptionInfo(UNDECIDED, "UNDECIDED", "Undecided", 1001, SurveyOptionKind.UNDECIDED, true));
    }

    private static List<OptionInfo> turnoutOptions() {
        return List.of(
                new OptionInfo(10L, "SURE_YES", "Sure yes", 1, SurveyOptionKind.CHOICE, true),
                new OptionInfo(11L, "PROBABLY_YES", "Probably yes", 2, SurveyOptionKind.CHOICE, true),
                new OptionInfo(12L, "PROBABLY_NO", "Probably no", 3, SurveyOptionKind.CHOICE, true),
                new OptionInfo(13L, "SURE_NO", "Sure no", 4, SurveyOptionKind.CHOICE, true));
    }

    /** Population: age 4 x 25%, sex 50/50, region 4 x 25%, settlement 60/40. */
    private static Map<SurveyRole, List<CategoryInfo>> demographics(boolean withPopulation) {
        Map<SurveyRole, List<CategoryInfo>> map = new EnumMap<>(SurveyRole.class);
        map.put(SurveyRole.AGE, categories(withPopulation, "AGE", 25, 25, 25, 25));
        map.put(SurveyRole.SEX, categories(withPopulation, "SEX", 50, 50));
        map.put(SurveyRole.REGION, categories(withPopulation, "REGION", 25, 25, 25, 25));
        map.put(SurveyRole.SETTLEMENT, categories(withPopulation, "SETTLEMENT", 60, 40));
        return map;
    }

    private static List<CategoryInfo> categories(boolean withPopulation, String prefix, long... populations) {
        List<CategoryInfo> list = new ArrayList<>();
        for (int i = 0; i < populations.length; i++) {
            list.add(new CategoryInfo(prefix + "_" + i, prefix + " " + i, withPopulation ? populations[i] : null));
        }
        return list;
    }

    private static Respondent respondent(long vote, String turnout, int age, int sex, int region, int settlement) {
        return new Respondent(vote, turnout, new int[] {age, sex, region, settlement});
    }

    private Input input(List<Respondent> respondents, int minWeighted, boolean withPopulation) {
        return new Input(respondents, voteOptions(), turnoutOptions(), demographics(withPopulation), minWeighted);
    }

    private static OptionShare share(List<OptionShare> shares, long optionId) {
        return shares.stream().filter(s -> s.optionId() == optionId).findFirst().orElseThrow();
    }

    @Test
    void rawSharesAreTakenAmongThoseWhoChoseAnActiveList() {
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 4; i++) rs.add(respondent(LIST_A, "SURE_YES", 0, 0, 0, 0));
        for (int i = 0; i < 3; i++) rs.add(respondent(LIST_B, "SURE_YES", 0, 0, 0, 0));
        rs.add(respondent(LIST_C_WITHDRAWN, "SURE_YES", 0, 0, 0, 0));
        for (int i = 0; i < 2; i++) rs.add(respondent(UNDECIDED, "SURE_YES", 0, 0, 0, 0));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 100, true));

        assertThat(r.responseCount()).isEqualTo(10);
        assertThat(r.weightedAvailable()).isFalse();
        assertThat(r.weighting()).isNull();
        assertThat(r.voteIntention().lists().base()).isEqualTo(7); // the withdrawn list's answer is out
        assertThat(r.voteIntention().lists().options()).extracting(OptionShare::optionId).containsExactly(LIST_A, LIST_B);
        assertThat(share(r.voteIntention().lists().options(), LIST_A).rawPct()).isEqualTo(57.1);
        assertThat(share(r.voteIntention().lists().options(), LIST_B).rawPct()).isEqualTo(42.9);
        assertThat(share(r.voteIntention().lists().options(), LIST_A).weightedPct()).isNull();
        // "others" are a share of everyone whose choice is still valid: 9 respondents, 2 undecided
        assertThat(r.voteIntention().others().base()).isEqualTo(9);
        assertThat(r.voteIntention().others().options()).hasSize(1);
        assertThat(r.voteIntention().others().options().get(0).rawPct()).isEqualTo(22.2);
    }

    @Test
    void likelyVotersBasisLeavesOutThoseWhoProbablyWillNotVote() {
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 3; i++) rs.add(respondent(LIST_A, "SURE_YES", 0, 0, 0, 0));
        for (int i = 0; i < 1; i++) rs.add(respondent(LIST_B, "PROBABLY_YES", 0, 0, 0, 0));
        for (int i = 0; i < 4; i++) rs.add(respondent(LIST_B, "PROBABLY_NO", 0, 0, 0, 0));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 100, true));

        assertThat(r.voteIntention().lists().base()).isEqualTo(8);
        assertThat(share(r.voteIntention().lists().options(), LIST_B).rawPct()).isEqualTo(62.5);
        assertThat(r.voteIntention().likelyVoters().base()).isEqualTo(4);
        assertThat(share(r.voteIntention().likelyVoters().options(), LIST_A).rawPct()).isEqualTo(75.0);
        assertThat(share(r.voteIntention().likelyVoters().options(), LIST_B).rawPct()).isEqualTo(25.0);
    }

    @Test
    void turnoutIsSharedOverEveryone() {
        List<Respondent> rs = List.of(
                respondent(LIST_A, "SURE_YES", 0, 0, 0, 0),
                respondent(LIST_A, "SURE_YES", 0, 0, 0, 0),
                respondent(UNDECIDED, "SURE_NO", 0, 0, 0, 0),
                respondent(LIST_C_WITHDRAWN, "PROBABLY_YES", 0, 0, 0, 0));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 100, true));

        assertThat(r.turnout().base()).isEqualTo(4); // includes the one whose list was withdrawn
        assertThat(r.turnout().options().get(0).rawPct()).isEqualTo(50.0);
        assertThat(r.turnout().options().get(1).rawPct()).isEqualTo(25.0);
        assertThat(r.turnout().options().get(3).rawPct()).isEqualTo(25.0);
    }

    @Test
    void weightingCorrectsAnOverrepresentedGroup() {
        // 80 men and 20 women against a 50/50 population; only the men choose list A.
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int sex = i < 80 ? 0 : 1;
            rs.add(respondent(sex == 0 ? LIST_A : LIST_B, "SURE_YES", i % 4, sex, (i / 3) % 4, i % 10 < 6 ? 0 : 1));
        }

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 50, true));

        assertThat(r.weightedAvailable()).isTrue();
        assertThat(share(r.voteIntention().lists().options(), LIST_A).rawPct()).isEqualTo(80.0);
        assertThat(share(r.voteIntention().lists().options(), LIST_A).weightedPct()).isCloseTo(50.0, within(2.0));
        assertThat(r.weighting().converged()).isTrue();
        assertThat(r.weighting().usedDimensions()).hasSize(4);
        assertThat(r.weighting().effectiveSampleSize()).isLessThan(100.0);
        assertThat(r.weighting().designEffect()).isGreaterThan(1.0);
        assertThat(r.weighting().smallSample()).isTrue();
    }

    @Test
    void weightedFiguresAppearOnlyFromTheMinimumNumberOfAnswers() {
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 49; i++) rs.add(respondent(LIST_A, "SURE_YES", i % 4, i % 2, i % 4, i % 2));

        SurveyResultsResponse before = calculator.compute(meta, input(rs, 50, true));
        assertThat(before.weightedAvailable()).isFalse();

        rs.add(respondent(LIST_B, "SURE_YES", 0, 0, 0, 0));
        SurveyResultsResponse after = calculator.compute(meta, input(rs, 50, true));
        assertThat(after.weightedAvailable()).isTrue();
        assertThat(after.weighting()).isNotNull();
    }

    @Test
    void dimensionsWithTooFewRespondentsInACategoryAreDroppedAndReported() {
        // Everybody in one region: three of the four regions are empty, so REGION cannot be used.
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 60; i++) rs.add(respondent(LIST_A, "SURE_YES", i % 4, i % 2, 0, i % 10 < 6 ? 0 : 1));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 50, true));

        assertThat(r.weightedAvailable()).isTrue();
        assertThat(r.weighting().droppedDimensions()).containsExactly("REGION");
        assertThat(r.weighting().usedDimensions()).containsExactlyInAnyOrder("AGE", "SEX", "SETTLEMENT");
    }

    @Test
    void withoutCensusFiguresThereAreNoWeightedResults() {
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 60; i++) rs.add(respondent(LIST_A, "SURE_YES", i % 4, i % 2, i % 4, i % 2));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 50, false));

        assertThat(r.weightedAvailable()).isFalse();
        assertThat(r.structure().get(0).categories().get(0).populationPct()).isNull();
    }

    @Test
    void structureTableComparesSampleWithPopulation() {
        List<Respondent> rs = new ArrayList<>();
        for (int i = 0; i < 10; i++) rs.add(respondent(LIST_A, "SURE_YES", i < 7 ? 0 : 3, 0, 0, 1));

        SurveyResultsResponse r = calculator.compute(meta, input(rs, 100, true));

        StructureDimension age = r.structure().get(0);
        assertThat(age.dimension()).isEqualTo("AGE");
        assertThat(age.categories().get(0).sampleCount()).isEqualTo(7);
        assertThat(age.categories().get(0).samplePct()).isEqualTo(70.0);
        assertThat(age.categories().get(0).populationPct()).isEqualTo(25.0);
        StructureDimension sex = r.structure().get(1);
        assertThat(sex.categories().get(0).samplePct()).isEqualTo(100.0);
        assertThat(sex.categories().get(1).samplePct()).isEqualTo(0.0);
    }

    @Test
    void noAnswersGivesEmptyResults() {
        SurveyResultsResponse r = calculator.compute(meta, input(List.of(), 50, true));

        assertThat(r.responseCount()).isZero();
        assertThat(r.weightedAvailable()).isFalse();
        assertThat(r.voteIntention().lists().base()).isZero();
        assertThat(share(r.voteIntention().lists().options(), LIST_A).rawPct()).isNull();
        assertThat(r.structure().get(0).categories().get(0).samplePct()).isNull();
    }
}
