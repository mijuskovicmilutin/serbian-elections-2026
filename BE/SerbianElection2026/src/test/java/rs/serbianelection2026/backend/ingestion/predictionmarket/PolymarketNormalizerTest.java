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

    @Test
    void mapsVolumeImageDailyChangeBidAskAndYesTokenFromTheMarketRecord() {
        String json = """
                [{"id":"1","slug":"s","title":"T","volume":370756.55,"endDate":"2028-06-30T23:59:00Z","markets":[
                  {"id":"10","groupItemTitle":"A","outcomePrices":"[\\"0.5175\\", \\"0.4825\\"]","volume":"254782.98",
                   "image":"https://img/a.jpg","oneDayPriceChange":0.034,"bestAsk":0.523,"bestBid":0.512,
                   "clobTokenIds":"[\\"111\\", \\"222\\"]"}]}]
                """;
        PolymarketEventRecord[] events = jsonMapper.readValue(json, PolymarketEventRecord[].class);

        NormalizedPredictionMarket result = normalizer.normalize(events[0]);

        assertThat(result.volume()).isEqualByComparingTo("370756.55");
        assertThat(result.endDate()).isEqualTo(java.time.Instant.parse("2028-06-30T23:59:00Z"));
        var outcome = result.outcomes().get(0);
        assertThat(outcome.imageUrl()).isEqualTo("https://img/a.jpg");
        assertThat(outcome.volume()).isEqualByComparingTo("254782.98");
        assertThat(outcome.oneDayPriceChange()).isEqualByComparingTo("0.034");
        assertThat(outcome.bestAsk()).isEqualByComparingTo("0.523");
        assertThat(outcome.bestBid()).isEqualByComparingTo("0.512");
        assertThat(outcome.yesTokenId()).isEqualTo("111");
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
