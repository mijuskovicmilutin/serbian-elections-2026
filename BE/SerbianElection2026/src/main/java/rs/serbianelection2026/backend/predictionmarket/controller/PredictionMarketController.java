package rs.serbianelection2026.backend.predictionmarket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.predictionmarket.dto.PredictionMarketResponse;
import rs.serbianelection2026.backend.predictionmarket.entity.PredictionMarket;
import rs.serbianelection2026.backend.predictionmarket.mapper.PredictionMarketMapper;
import rs.serbianelection2026.backend.predictionmarket.service.PredictionMarketService;

@RestController
@RequestMapping("/api/v1/prediction-markets")
public class PredictionMarketController {

    private final PredictionMarketService predictionMarketService;
    private final PredictionMarketMapper predictionMarketMapper;

    public PredictionMarketController(
            PredictionMarketService predictionMarketService, PredictionMarketMapper predictionMarketMapper) {
        this.predictionMarketService = predictionMarketService;
        this.predictionMarketMapper = predictionMarketMapper;
    }

    @GetMapping("/current")
    public PredictionMarketResponse getCurrentMarket() {
        PredictionMarket market = predictionMarketService.getCurrentMarket();
        return predictionMarketMapper.toResponse(market, predictionMarketService.getOutcomes(market.getId()));
    }
}
