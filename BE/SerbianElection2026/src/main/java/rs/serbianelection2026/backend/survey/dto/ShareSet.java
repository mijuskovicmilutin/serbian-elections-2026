package rs.serbianelection2026.backend.survey.dto;

import java.util.List;

/** {@code base} is the number of respondents the percentages are taken over. */
public record ShareSet(int base, List<OptionShare> options) {
}
