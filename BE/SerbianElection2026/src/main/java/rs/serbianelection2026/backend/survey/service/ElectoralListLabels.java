package rs.serbianelection2026.backend.survey.service;

import java.util.regex.Pattern;

/** Same display name as the site shows for a list: RIK's title without the leading "N. ИЗБОРНА ЛИСТА". */
final class ElectoralListLabels {

    private static final Pattern PREFIX = Pattern.compile("^\\d+\\.\\s*ИЗБОРНА\\s+ЛИСТА\\s*(?:[-–]\\s*)?", Pattern.CASE_INSENSITIVE);

    private ElectoralListLabels() {
    }

    static String displayName(String rawName) {
        return PREFIX.matcher(rawName).replaceFirst("").trim();
    }
}
