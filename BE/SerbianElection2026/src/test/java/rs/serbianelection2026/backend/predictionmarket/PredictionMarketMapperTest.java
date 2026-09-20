package rs.serbianelection2026.backend.predictionmarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketResponse;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarketOutcome;
import rs.serbianelection2026.backend.predictionmarket.mapper.PredictionMarketMapper;
import tools.jackson.databind.json.JsonMapper;

class PredictionMarketMapperTest {

    private final PredictionMarketMapper mapper = new PredictionMarketMapper(JsonMapper.builder().build());

    private PredictionMarket market() {
        return PredictionMarket.builder()
                .id(1L)
                .provider(ImportSource.POLYMARKET)
                .marketName("Next PM?")
                .sourceUrl("https://polymarket.com/event/x")
                .updatedAt(Instant.parse("2026-09-20T20:00:00Z"))
                .volume(new BigDecimal("370756.55"))
                .build();
    }

    @Test
    void noPriceIsTheComplementOfTheBestBidAndHistoryIsParsed() {
        PredictionMarketOutcome outcome = PredictionMarketOutcome.builder()
                .name("A")
                .price(new BigDecimal("0.5175"))
                .bestAsk(new BigDecimal("0.523"))
                .bestBid(new BigDecimal("0.512"))
                .priceHistory("[{\"t\":1782950411,\"p\":0.45},{\"t\":1789940049,\"p\":0.5175}]")
                .build();

        PredictionMarketResponse response = mapper.toResponse(market(), List.of(outcome));

        var out = response.outcomes().get(0);
        assertThat(out.yesPrice()).isEqualByComparingTo("0.523");
        assertThat(out.noPrice()).isEqualByComparingTo("0.488");
        assertThat(out.priceHistory()).hasSize(2);
        assertThat(out.priceHistory().get(1).p()).isEqualByComparingTo("0.5175");
    }

    @Test
    void missingBidAndHistoryStayNullInsteadOfFailing() {
        PredictionMarketOutcome outcome = PredictionMarketOutcome.builder()
                .name("B").price(new BigDecimal("0.01")).build();

        var out = mapper.toResponse(market(), List.of(outcome)).outcomes().get(0);

        assertThat(out.noPrice()).isNull();
        assertThat(out.priceHistory()).isNull();
    }

    @Test
    void corruptStoredHistoryIsOmittedRatherThanBreakingTheEndpoint() {
        PredictionMarketOutcome outcome = PredictionMarketOutcome.builder()
                .name("C").price(new BigDecimal("0.02")).priceHistory("not json").build();

        var out = mapper.toResponse(market(), List.of(outcome)).outcomes().get(0);

        assertThat(out.priceHistory()).isNull();
    }
}
