package rs.serbianelection2026.backend.survey.service;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * "One answer per network" bookkeeping. One row per keyed hash, with a counter and nothing else: no response id,
 * no timestamp, no sequence, so a row cannot be tied to an answer. Rows are removed when the survey closes.
 */
@Component
public class SurveyDedupeStore {

    private final JdbcClient jdbc;

    public SurveyDedupeStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Counts one more answer for this hash unless it already reached {@code max}; true if it was counted. */
    public boolean tryCount(long surveyId, String networkHash, int max) {
        if (max < 1) {
            return false;
        }
        int changed = jdbc.sql("""
                        INSERT INTO survey_dedupe (survey_id, ip_hash, count) VALUES (:surveyId, :hash, 1)
                        ON CONFLICT (survey_id, ip_hash)
                        DO UPDATE SET count = survey_dedupe.count + 1 WHERE survey_dedupe.count < :max
                        """)
                .param("surveyId", surveyId)
                .param("hash", networkHash)
                .param("max", max)
                .update();
        return changed == 1;
    }

    public int deleteAll(long surveyId) {
        return jdbc.sql("DELETE FROM survey_dedupe WHERE survey_id = :surveyId").param("surveyId", surveyId).update();
    }
}
