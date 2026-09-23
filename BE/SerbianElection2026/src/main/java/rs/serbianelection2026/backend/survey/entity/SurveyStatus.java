package rs.serbianelection2026.backend.survey.entity;

/** Manual switch; a survey only takes answers when it is OPEN and inside its opening window. */
public enum SurveyStatus {
    DRAFT,
    OPEN,
    CLOSED
}
