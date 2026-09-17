package rs.serbianelection2026.backend.ingestion.predictionmarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.NormalizedPredictionMarket;
import rs.serbianelection2026.backend.ingestion.predictionmarket.dto.PolymarketEventRecord;
import tools.jackson.databind.json.JsonMapper;

class PolymarketNormalizerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final PolymarketNormalizer normalizer = new PolymarketNormalizer(jsonMapper);

    @Test
    void dropsUntradedPlaceholderSlotsAndKeepsRealCandidatesSortedAsFetched() throws IOException {
        PolymarketEventRecord[] events = jsonMapper.readValue(loadFixture(), PolymarketEventRecord[].class);

        NormalizedPredictionMarket result = normalizer.normalize(events[0]);

        assertThat(result.externalId()).isEqualTo("655483");
        assertThat(result.title()).isEqualTo("Next Prime Minister of Serbia?");
        assertThat(result.sourceUrl()).isEqualTo(
                "https://polymarket.com/event/next-prime-minister-of-serbia-20260629223938642");

        // "Person C" and "Other" are untraded placeholder slots (volume 0) and must be dropped.
        assertThat(result.outcomes()).hasSize(3);
        assertThat(result.outcomes()).extracting("name")
                .containsExactly("Aleksandar Vučić", "Ilija Srdanović", "Vladan Đokić");
        assertThat(result.outcomes().get(0).price()).isEqualByComparingTo(new BigDecimal("0.5605"));
    }

    private String loadFixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/predictionmarket/next-pm-serbia-event.json")) {
            if (in == null) {
                throw new IllegalStateException("Fixture not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
