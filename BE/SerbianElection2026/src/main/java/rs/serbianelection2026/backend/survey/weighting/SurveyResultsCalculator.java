package rs.serbianelection2026.backend.survey.weighting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import rs.serbianelection2026.backend.survey.dto.OptionShare;
import rs.serbianelection2026.backend.survey.dto.ShareSet;
import rs.serbianelection2026.backend.survey.dto.StructureCategory;
import rs.serbianelection2026.backend.survey.dto.StructureDimension;
import rs.serbianelection2026.backend.survey.dto.SurveyResultsResponse;
import rs.serbianelection2026.backend.survey.dto.VoteIntentionResult;
import rs.serbianelection2026.backend.survey.dto.WeightingInfo;
import rs.serbianelection2026.backend.survey.entity.SurveyOptionKind;
import rs.serbianelection2026.backend.survey.entity.SurveyRole;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter.Dimension;

/**
 * Turns the collected answers into the numbers shown on the site: raw and weighted shares per option, the
 * effective sample size and the sample-versus-population table. Pure computation, no I/O.
 *
 * <p>Answers that chose an option that is no longer active (a list RIK rejected or withdrew) are left out of
 * question 1 altogether (as if that question had not been answered), but the respondent still counts for the
 * demographics, the weights and the turnout question.
 */
public final class SurveyResultsCalculator {

    /** The raking dimensions, in the order used for {@link Respondent#categories()}. */
    public static final List<SurveyRole> DIMENSIONS =
            List.of(SurveyRole.AGE, SurveyRole.SEX, SurveyRole.REGION, SurveyRole.SETTLEMENT);

    /** Below this effective sample size the weighted figures are flagged as a small sample. */
    public static final double SMALL_SAMPLE_EFFECTIVE = 300;

    private static final Set<String> LIKELY_TURNOUT = Set.of("SURE_YES", "PROBABLY_YES");

    /** An option of question 1 or 2. */
    public record OptionInfo(Long id, String code, String label, int position, SurveyOptionKind kind, boolean active) {
    }

    /** A category of a weighting dimension; {@code population} is null when no census figure is known. */
    public record CategoryInfo(String code, String label, Long population) {
    }

    /**
     * @param categories index of the respondent's category in each of {@link #DIMENSIONS}, in that order
     */
    public record Respondent(Long voteOptionId, String turnoutCode, int[] categories) {
    }

    public record Input(
            List<Respondent> respondents,
            List<OptionInfo> voteOptions,
            List<OptionInfo> turnoutOptions,
            Map<SurveyRole, List<CategoryInfo>> demographics,
            int minWeightedResponses) {
    }

    public record Meta(Long surveyId, Instant computedAt, Instant opensAt, Instant closesAt) {
    }

    private final RakingWeighter weighter;

    public SurveyResultsCalculator(RakingWeighter weighter) {
        this.weighter = weighter;
    }

    public SurveyResultsResponse compute(Meta meta, Input input) {
        List<Respondent> respondents = input.respondents();
        int n = respondents.size();

        RakingWeighter.Result rake = null;
        if (n > 0 && n >= input.minWeightedResponses() && allPopulationsKnown(input)) {
            rake = weighter.rake(n, rakingDimensions(input, respondents));
        }
        double[] weights = rake == null ? null : rake.weights();

        Map<Long, OptionInfo> voteById = new HashMap<>();
        input.voteOptions().forEach(o -> voteById.put(o.id(), o));

        Predicate<Respondent> validVote = r -> {
            OptionInfo o = voteById.get(r.voteOptionId());
            return o != null && o.active();
        };
        Predicate<Respondent> choseList = validVote.and(r -> voteById.get(r.voteOptionId()).kind() == SurveyOptionKind.CHOICE);
        Predicate<Respondent> likely = choseList.and(r -> LIKELY_TURNOUT.contains(r.turnoutCode()));

        List<OptionInfo> activeLists = input.voteOptions().stream()
                .filter(o -> o.active() && o.kind() == SurveyOptionKind.CHOICE)
                .toList();
        List<OptionInfo> activeOthers = input.voteOptions().stream()
                .filter(o -> o.active() && o.kind() != SurveyOptionKind.CHOICE)
                .toList();

        Function<Respondent, Object> voteKey = Respondent::voteOptionId;
        VoteIntentionResult vote = new VoteIntentionResult(
                shares(activeLists, respondents, weights, choseList, voteKey, OptionInfo::id),
                shares(activeLists, respondents, weights, likely, voteKey, OptionInfo::id),
                shares(activeOthers, respondents, weights, validVote, voteKey, OptionInfo::id));
        ShareSet turnout = shares(
                input.turnoutOptions(), respondents, weights, r -> true, Respondent::turnoutCode, OptionInfo::code);

        WeightingInfo info = rake == null
                ? null
                : new WeightingInfo(
                        round1(rake.effectiveSampleSize()),
                        round2(rake.designEffect()),
                        rake.converged(),
                        round2(rake.maxDeviation() * 100),
                        rake.usedDimensions(),
                        rake.droppedDimensions(),
                        rake.effectiveSampleSize() < SMALL_SAMPLE_EFFECTIVE);

        return new SurveyResultsResponse(
                meta.surveyId(),
                meta.computedAt(),
                meta.opensAt(),
                meta.closesAt(),
                n,
                input.minWeightedResponses(),
                rake != null,
                info,
                vote,
                turnout,
                structure(input, respondents));
    }

    private static boolean allPopulationsKnown(Input input) {
        for (SurveyRole role : DIMENSIONS) {
            List<CategoryInfo> categories = input.demographics().get(role);
            if (categories == null || categories.isEmpty()) {
                return false;
            }
            for (CategoryInfo c : categories) {
                if (c.population() == null || c.population() <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Dimension> rakingDimensions(Input input, List<Respondent> respondents) {
        List<Dimension> dimensions = new ArrayList<>();
        for (int d = 0; d < DIMENSIONS.size(); d++) {
            SurveyRole role = DIMENSIONS.get(d);
            List<CategoryInfo> categories = input.demographics().get(role);
            double total = categories.stream().mapToLong(CategoryInfo::population).sum();
            double[] targets = categories.stream().mapToDouble(c -> c.population() / total).toArray();
            int[] of = new int[respondents.size()];
            for (int i = 0; i < of.length; i++) {
                of[i] = respondents.get(i).categories()[d];
            }
            dimensions.add(new Dimension(role.name(), of, targets));
        }
        return dimensions;
    }

    private static ShareSet shares(
            List<OptionInfo> options,
            List<Respondent> respondents,
            double[] weights,
            Predicate<Respondent> inBase,
            Function<Respondent, Object> keyOfRespondent,
            Function<OptionInfo, Object> keyOfOption) {
        int base = 0;
        double weightedBase = 0;
        for (int i = 0; i < respondents.size(); i++) {
            if (inBase.test(respondents.get(i))) {
                base++;
                weightedBase += weights == null ? 1 : weights[i];
            }
        }

        List<OptionShare> result = new ArrayList<>();
        for (OptionInfo option : options) {
            int count = 0;
            double weightedCount = 0;
            for (int i = 0; i < respondents.size(); i++) {
                Respondent r = respondents.get(i);
                if (inBase.test(r) && keyOfRespondent.apply(r).equals(keyOfOption.apply(option))) {
                    count++;
                    weightedCount += weights == null ? 1 : weights[i];
                }
            }
            Double raw = base == 0 ? null : round1(100.0 * count / base);
            Double weighted = weights == null || weightedBase == 0 ? null : round1(100.0 * weightedCount / weightedBase);
            result.add(new OptionShare(
                    option.id(), option.label(), option.position(), option.kind().name(), count, raw, weighted));
        }
        return new ShareSet(base, result);
    }

    private static List<StructureDimension> structure(Input input, List<Respondent> respondents) {
        int n = respondents.size();
        List<StructureDimension> result = new ArrayList<>();
        for (int d = 0; d < DIMENSIONS.size(); d++) {
            SurveyRole role = DIMENSIONS.get(d);
            List<CategoryInfo> categories = input.demographics().getOrDefault(role, List.of());
            int[] counts = new int[categories.size()];
            for (Respondent r : respondents) {
                counts[r.categories()[d]]++;
            }
            long total = categories.stream().map(CategoryInfo::population).filter(p -> p != null).mapToLong(Long::longValue).sum();
            List<StructureCategory> rows = new ArrayList<>();
            for (int k = 0; k < categories.size(); k++) {
                CategoryInfo c = categories.get(k);
                Double samplePct = n == 0 ? null : round1(100.0 * counts[k] / n);
                Double populationPct = c.population() == null || total == 0 ? null : round1(100.0 * c.population() / total);
                rows.add(new StructureCategory(c.code(), c.label(), counts[k], samplePct, populationPct));
            }
            result.add(new StructureDimension(role.name(), rows));
        }
        return result;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
