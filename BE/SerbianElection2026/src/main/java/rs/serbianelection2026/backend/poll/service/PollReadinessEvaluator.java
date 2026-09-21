package rs.serbianelection2026.backend.poll.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import rs.serbianelection2026.backend.poll.dto.PollReadiness;
import rs.serbianelection2026.backend.poll.dto.PollWarning;
import rs.serbianelection2026.backend.poll.entity.OptionKind;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.ResultBasis;

/**
 * Decides whether a poll may be published and what a reviewer should double-check. The publish
 * conditions (codes PERIOD, RESULT_BASIS, SOURCE, RESULTS) are hard; warnings never block.
 */
public final class PollReadinessEvaluator {

    public static final String PERIOD = "PERIOD";
    public static final String RESULT_BASIS = "RESULT_BASIS";
    public static final String SOURCE = "SOURCE";
    public static final String RESULTS = "RESULTS";

    public static final String SUM_OFF = "SUM_OFF";
    public static final String PARTNERS_UNKNOWN = "PARTNERS_UNKNOWN";
    public static final String EXACT_DATES_MISSING = "EXACT_DATES_MISSING";

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DECIDED_TOLERANCE = BigDecimal.ONE;
    private static final BigDecimal OVER_TOLERANCE = new BigDecimal("0.5");

    private PollReadinessEvaluator() {
    }

    public static PollReadiness evaluate(Poll poll, List<PollResult> results) {
        List<String> missing = new ArrayList<>();
        boolean hasNote = poll.getFieldworkNote() != null && !poll.getFieldworkNote().isBlank();
        boolean hasDates = poll.getFieldworkFrom() != null || poll.getFieldworkTo() != null;
        if (!hasNote && !hasDates) {
            missing.add(PERIOD);
        }
        if (poll.getResultBasis() == null) {
            missing.add(RESULT_BASIS);
        }
        if (poll.getSourceUrl() == null || poll.getSourceUrl().isBlank()) {
            missing.add(SOURCE);
        }
        if (results.isEmpty()) {
            missing.add(RESULTS);
        }

        List<PollWarning> warnings = new ArrayList<>();
        if (!results.isEmpty()) {
            BigDecimal sum = results.stream().map(PollResult::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean decided = poll.getResultBasis() == ResultBasis.DECIDED_VOTERS;
            BigDecimal gap = sum.subtract(HUNDRED).abs();
            boolean off = decided ? gap.compareTo(DECIDED_TOLERANCE) > 0 : sum.subtract(HUNDRED).compareTo(OVER_TOLERANCE) > 0;
            if (off) {
                warnings.add(new PollWarning(SUM_OFF, sum.setScale(1, RoundingMode.HALF_UP).toPlainString()));
            }
            long unspecified = results.stream().filter(r -> r.getOptionKind() == OptionKind.UNSPECIFIED).count();
            if (unspecified > 0) {
                warnings.add(new PollWarning(PARTNERS_UNKNOWN, Long.toString(unspecified)));
            }
        }
        if (hasNote && !hasDates) {
            warnings.add(new PollWarning(EXACT_DATES_MISSING, null));
        }

        return new PollReadiness(missing.isEmpty(), List.copyOf(missing), List.copyOf(warnings));
    }
}
