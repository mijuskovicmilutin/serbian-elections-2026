package rs.serbianelection2026.backend.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter.Config;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter.Dimension;
import rs.serbianelection2026.backend.survey.weighting.RakingWeighter.Result;

class RakingWeighterTest {

    private final RakingWeighter weighter = new RakingWeighter();

    /** The worked example from PLAN.md: 80/20 sample against a 40/60 population. */
    @Test
    void reproducesThePlanExampleForOneDimension() {
        int[] category = new int[1000];
        Arrays.fill(category, 0, 800, 0);
        Arrays.fill(category, 800, 1000, 1);

        Result result = weighter.rake(1000, List.of(new Dimension("age", category, new double[] {0.4, 0.6})));

        assertThat(result.converged()).isTrue();
        assertThat(result.weights()[0]).isCloseTo(0.5, within(1e-9));
        assertThat(result.weights()[999]).isCloseTo(3.0, within(1e-9));
        assertThat(result.effectiveSampleSize()).isCloseTo(500.0, within(1e-6));
        assertThat(result.designEffect()).isCloseTo(2.0, within(1e-9));
        assertThat(result.usedDimensions()).containsExactly("age");
        assertThat(result.droppedDimensions()).isEmpty();
    }

    @Test
    void weightedShareOfOptionMatchesThePlanExample() {
        int[] category = new int[1000];
        Arrays.fill(category, 800, 1000, 1);
        Result result = weighter.rake(1000, List.of(new Dimension("age", category, new double[] {0.4, 0.6})));

        // 70% of the young and 30% of the old choose option A: 62% raw, 46% weighted.
        double raw = 0;
        double weighted = 0;
        double totalWeight = 0;
        for (int i = 0; i < 1000; i++) {
            boolean young = category[i] == 0;
            int position = young ? i : i - 800;
            boolean choosesA = young ? position < 560 : position < 60;
            raw += choosesA ? 1 : 0;
            weighted += choosesA ? result.weights()[i] : 0;
            totalWeight += result.weights()[i];
        }
        assertThat(raw / 1000).isCloseTo(0.62, within(1e-9));
        assertThat(weighted / totalWeight).isCloseTo(0.46, within(1e-9));
    }

    @Test
    void weightsAreNormalisedToMeanOne() {
        int[] category = new int[100];
        Arrays.fill(category, 60, 100, 1);
        Result result = weighter.rake(100, List.of(new Dimension("d", category, new double[] {0.2, 0.8})));

        assertThat(Arrays.stream(result.weights()).average().orElseThrow()).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void twoDimensionsConvergeToBothMargins() {
        // Joint counts: (a0,b0)=300 (a0,b1)=100 (a1,b0)=100 (a1,b1)=50
        int[] a = new int[550];
        int[] b = new int[550];
        int idx = 0;
        idx = fill(a, b, idx, 300, 0, 0);
        idx = fill(a, b, idx, 100, 0, 1);
        idx = fill(a, b, idx, 100, 1, 0);
        fill(a, b, idx, 50, 1, 1);

        double[] targetA = {0.5, 0.5};
        double[] targetB = {0.4, 0.6};
        Result result = weighter.rake(550, List.of(new Dimension("a", a, targetA), new Dimension("b", b, targetB)));

        assertThat(result.converged()).isTrue();
        assertThat(result.maxDeviation()).isLessThan(0.001);
        assertThat(share(result.weights(), a, 0)).isCloseTo(0.5, within(0.001));
        assertThat(share(result.weights(), b, 0)).isCloseTo(0.4, within(0.001));
        assertThat(result.effectiveSampleSize()).isLessThan(550.0);
        assertThat(result.designEffect()).isGreaterThan(1.0);
    }

    @Test
    void trimsExtremeWeightsAndReportsThatTheMarginIsNotMet() {
        int[] category = new int[1000];
        Arrays.fill(category, 5, 1000, 1); // only 5 respondents in category 0, but half the population

        Result result = weighter.rake(1000, List.of(new Dimension("d", category, new double[] {0.5, 0.5})));

        assertThat(result.converged()).isFalse();
        assertThat(result.maxDeviation()).isGreaterThan(0.001);
        double ratio = result.weights()[0] / result.weights()[999];
        assertThat(ratio).isLessThanOrEqualTo(25.0 + 1e-9); // 5x mean over 0.2x mean
        assertThat(ratio).isGreaterThan(1.0);
    }

    @Test
    void dropsADimensionWhenACategoryHasTooFewRespondents() {
        int[] a = new int[200];
        Arrays.fill(a, 0, 100, 0);
        Arrays.fill(a, 100, 200, 1);
        int[] b = new int[200];
        Arrays.fill(b, 0, 197, 0);
        Arrays.fill(b, 197, 200, 1); // 3 < 5

        Result result = weighter.rake(200, List.of(
                new Dimension("a", a, new double[] {0.3, 0.7}), new Dimension("b", b, new double[] {0.5, 0.5})));

        assertThat(result.droppedDimensions()).containsExactly("b");
        assertThat(result.usedDimensions()).containsExactly("a");
        assertThat(result.converged()).isTrue();
        assertThat(result.weights()[150] / result.weights()[0]).isCloseTo(0.7 / 0.3, within(1e-9));
    }

    @Test
    void withNoUsableDimensionEveryWeightIsOne() {
        int[] a = new int[20];
        Arrays.fill(a, 0, 17, 0);
        Arrays.fill(a, 17, 20, 1);

        Result result = weighter.rake(20, List.of(new Dimension("a", a, new double[] {0.5, 0.5})));

        assertThat(result.usedDimensions()).isEmpty();
        assertThat(result.droppedDimensions()).containsExactly("a");
        assertThat(result.converged()).isTrue();
        assertThat(result.weights()).containsOnly(1.0);
        assertThat(result.effectiveSampleSize()).isCloseTo(20.0, within(1e-9));
        assertThat(result.designEffect()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void aSampleThatAlreadyMatchesTheTargetsKeepsEqualWeights() {
        int[] a = new int[100];
        Arrays.fill(a, 0, 40, 0);
        Arrays.fill(a, 40, 100, 1);

        Result result = weighter.rake(100, List.of(new Dimension("a", a, new double[] {0.4, 0.6})));

        for (double w : result.weights()) {
            assertThat(w).isCloseTo(1.0, within(1e-9));
        }
        assertThat(result.iterations()).isEqualTo(1);
    }

    @Test
    void targetSharesMustSumToOne() {
        assertThatThrownBy(() -> weighter.rake(10, List.of(new Dimension("a", new int[10], new double[] {0.5, 0.6}))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1");
    }

    @Test
    void categoryIndexMustExist() {
        int[] a = new int[10];
        a[3] = 2;
        assertThatThrownBy(() -> weighter.rake(10, List.of(new Dimension("a", a, new double[] {0.5, 0.5}))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of range");
    }

    @Test
    void respondentCountMustMatchEveryDimension() {
        assertThatThrownBy(() -> weighter.rake(10, List.of(new Dimension("a", new int[9], new double[] {0.5, 0.5}))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atLeastOneRespondentIsRequired() {
        assertThatThrownBy(() -> weighter.rake(0, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configRejectsNonsense() {
        assertThatThrownBy(() -> new Config(0, 0.001, 0.2, 5, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Config(10, 0, 0.2, 5, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Config(10, 0.001, 5, 0.2, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    private static int fill(int[] a, int[] b, int from, int count, int catA, int catB) {
        for (int i = from; i < from + count; i++) {
            a[i] = catA;
            b[i] = catB;
        }
        return from + count;
    }

    private static double share(double[] w, int[] category, int k) {
        double part = 0;
        double total = 0;
        for (int i = 0; i < w.length; i++) {
            total += w[i];
            if (category[i] == k) {
                part += w[i];
            }
        }
        return part / total;
    }
}
