package rs.serbianelection2026.backend.survey.weighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Raking (iterative proportional fitting) of respondent weights to known population margins.
 *
 * <p>Pure computation, no I/O. Each {@link Dimension} says which category every respondent falls into and what
 * share of the population each category has. Starting from weight 1, the weights are repeatedly rescaled so that
 * the weighted share of every category matches its target, dimension after dimension, until the largest
 * deviation is below {@link Config#tolerance()}.
 *
 * <p>Two safeguards keep a small self-selected sample from producing nonsense:
 * <ul>
 *   <li>a dimension in which some category has fewer than {@link Config#minPerCategory()} respondents is
 *       <b>dropped</b> (and reported), because its weights would rest on a handful of people;</li>
 *   <li>after every full pass the weights are clamped to [{@link Config#minRelativeWeight()},
 *       {@link Config#maxRelativeWeight()}] times the mean weight, so a few rare respondents cannot decide the
 *       result. Trimming can stop the margins from being met exactly; that is reported as
 *       {@code converged = false} together with the remaining deviation.</li>
 * </ul>
 */
public final class RakingWeighter {

    /** Tuning knobs; the defaults are the values in PLAN.md (V6). */
    public record Config(
            int maxIterations, double tolerance, double minRelativeWeight, double maxRelativeWeight, int minPerCategory) {

        public Config {
            if (maxIterations < 1) {
                throw new IllegalArgumentException("maxIterations must be at least 1");
            }
            if (tolerance <= 0) {
                throw new IllegalArgumentException("tolerance must be positive");
            }
            if (minRelativeWeight <= 0 || maxRelativeWeight < minRelativeWeight) {
                throw new IllegalArgumentException("weight bounds must satisfy 0 < min <= max");
            }
            if (minPerCategory < 0) {
                throw new IllegalArgumentException("minPerCategory must not be negative");
            }
        }

        public static Config defaults() {
            return new Config(100, 0.001, 0.2, 5.0, 5);
        }
    }

    /**
     * One weighting dimension.
     *
     * @param name only used for reporting
     * @param categoryOfRespondent for every respondent, the index of their category in {@code targetShares}
     * @param targetShares population share of every category; must be non-negative and sum to 1
     */
    public record Dimension(String name, int[] categoryOfRespondent, double[] targetShares) {
    }

    /**
     * @param weights one per respondent, normalised to mean 1
     * @param converged every used margin is within tolerance
     * @param maxDeviation largest remaining |weighted share - target| over the used dimensions (0 if none)
     * @param effectiveSampleSize {@code (sum w)^2 / sum w^2}
     * @param designEffect {@code n / effectiveSampleSize}
     * @param usedDimensions names of the dimensions that took part
     * @param droppedDimensions names of the dimensions skipped because a category had too few respondents
     */
    public record Result(
            double[] weights,
            boolean converged,
            int iterations,
            double maxDeviation,
            double effectiveSampleSize,
            double designEffect,
            List<String> usedDimensions,
            List<String> droppedDimensions) {
    }

    private final Config config;

    public RakingWeighter() {
        this(Config.defaults());
    }

    public RakingWeighter(Config config) {
        this.config = config;
    }

    public Result rake(int respondents, List<Dimension> dimensions) {
        if (respondents < 1) {
            throw new IllegalArgumentException("At least one respondent is required");
        }

        List<Dimension> used = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (Dimension dimension : dimensions) {
            validate(respondents, dimension);
            if (hasEnoughRespondentsInEveryCategory(dimension)) {
                used.add(normalised(dimension));
            } else {
                dropped.add(dimension.name());
            }
        }

        double[] w = new double[respondents];
        Arrays.fill(w, 1.0);

        int iterations = 0;
        boolean converged = used.isEmpty();
        double deviation = 0;

        while (!converged && iterations < config.maxIterations()) {
            iterations++;
            for (Dimension dimension : used) {
                adjust(w, dimension);
            }
            trim(w);
            deviation = maxDeviation(w, used);
            converged = deviation < config.tolerance();
        }

        normaliseToMeanOne(w);
        double sum = Arrays.stream(w).sum();
        double sumSquares = Arrays.stream(w).map(x -> x * x).sum();
        double effective = sum * sum / sumSquares;

        return new Result(
                w,
                converged,
                iterations,
                deviation,
                effective,
                respondents / effective,
                used.stream().map(Dimension::name).toList(),
                List.copyOf(dropped));
    }

    private void validate(int respondents, Dimension dimension) {
        if (dimension.categoryOfRespondent().length != respondents) {
            throw new IllegalArgumentException(
                    "Dimension " + dimension.name() + " has " + dimension.categoryOfRespondent().length
                            + " respondents, expected " + respondents);
        }
        double[] targets = dimension.targetShares();
        double total = 0;
        for (double t : targets) {
            if (t < 0) {
                throw new IllegalArgumentException("Negative target share in dimension " + dimension.name());
            }
            total += t;
        }
        if (Math.abs(total - 1.0) > 1e-6) {
            throw new IllegalArgumentException(
                    "Target shares of dimension " + dimension.name() + " must sum to 1, got " + total);
        }
        for (int category : dimension.categoryOfRespondent()) {
            if (category < 0 || category >= targets.length) {
                throw new IllegalArgumentException("Category index out of range in dimension " + dimension.name());
            }
        }
    }

    private boolean hasEnoughRespondentsInEveryCategory(Dimension dimension) {
        int[] counts = new int[dimension.targetShares().length];
        for (int category : dimension.categoryOfRespondent()) {
            counts[category]++;
        }
        for (int k = 0; k < counts.length; k++) {
            if (dimension.targetShares()[k] > 0 && counts[k] < config.minPerCategory()) {
                return false;
            }
        }
        return true;
    }

    private static Dimension normalised(Dimension dimension) {
        double total = Arrays.stream(dimension.targetShares()).sum();
        double[] targets = Arrays.stream(dimension.targetShares()).map(t -> t / total).toArray();
        return new Dimension(dimension.name(), dimension.categoryOfRespondent(), targets);
    }

    /** One raking step: scale every category so its weighted share equals the target. */
    private static void adjust(double[] w, Dimension dimension) {
        double[] targets = dimension.targetShares();
        double[] sums = new double[targets.length];
        double total = 0;
        for (int i = 0; i < w.length; i++) {
            sums[dimension.categoryOfRespondent()[i]] += w[i];
            total += w[i];
        }
        double[] factor = new double[targets.length];
        for (int k = 0; k < targets.length; k++) {
            factor[k] = sums[k] > 0 ? targets[k] * total / sums[k] : 1.0;
        }
        for (int i = 0; i < w.length; i++) {
            w[i] *= factor[dimension.categoryOfRespondent()[i]];
        }
    }

    private void trim(double[] w) {
        double mean = Arrays.stream(w).sum() / w.length;
        double low = config.minRelativeWeight() * mean;
        double high = config.maxRelativeWeight() * mean;
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.min(high, Math.max(low, w[i]));
        }
    }

    private static double maxDeviation(double[] w, List<Dimension> dimensions) {
        double worst = 0;
        double total = Arrays.stream(w).sum();
        for (Dimension dimension : dimensions) {
            double[] sums = new double[dimension.targetShares().length];
            for (int i = 0; i < w.length; i++) {
                sums[dimension.categoryOfRespondent()[i]] += w[i];
            }
            for (int k = 0; k < sums.length; k++) {
                worst = Math.max(worst, Math.abs(sums[k] / total - dimension.targetShares()[k]));
            }
        }
        return worst;
    }

    private static void normaliseToMeanOne(double[] w) {
        double mean = Arrays.stream(w).sum() / w.length;
        for (int i = 0; i < w.length; i++) {
            w[i] /= mean;
        }
    }
}
