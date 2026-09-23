package rs.serbianelection2026.backend.survey.dto;

/**
 * Question 1. {@code lists}: percentages among those who chose a list. {@code likelyVoters}: the same among those
 * who will surely or probably vote. {@code others}: undecided / will not vote / no answer, as a share of everyone
 * who answered the question. Options are in ballot order, never sorted by result.
 */
public record VoteIntentionResult(ShareSet lists, ShareSet likelyVoters, ShareSet others) {
}
