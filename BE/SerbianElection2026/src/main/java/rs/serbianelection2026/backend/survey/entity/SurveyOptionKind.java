package rs.serbianelection2026.backend.survey.entity;

/** Only meaningful for the vote-intention question; every other option is a plain CHOICE. */
public enum SurveyOptionKind {
    CHOICE,
    UNDECIDED,
    WONT_VOTE,
    NO_ANSWER
}
