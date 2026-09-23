package rs.serbianelection2026.backend.survey.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.survey.dto.SubmitAnswersRequest;
import rs.serbianelection2026.backend.survey.dto.SubmitAnswersResponse;
import rs.serbianelection2026.backend.survey.dto.SurveyDetailResponse;
import rs.serbianelection2026.backend.survey.dto.SurveyResultsResponse;
import rs.serbianelection2026.backend.survey.service.ClientIpResolver;
import rs.serbianelection2026.backend.survey.service.SurveyService;

@RestController
@RequestMapping("/api/v1/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    private final ClientIpResolver clientIpResolver;

    public SurveyController(SurveyService surveyService, ClientIpResolver clientIpResolver) {
        this.surveyService = surveyService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/current")
    public SurveyDetailResponse getCurrent() {
        return surveyService.getCurrent();
    }

    @GetMapping("/{id}")
    public SurveyDetailResponse getSurvey(@PathVariable Long id) {
        return surveyService.getDetail(id);
    }

    @GetMapping("/{id}/results")
    public SurveyResultsResponse getResults(@PathVariable Long id) {
        return surveyService.getResults(id);
    }

    @PostMapping("/{id}/responses")
    public ResponseEntity<SubmitAnswersResponse> submit(
            @PathVariable Long id, @Valid @RequestBody SubmitAnswersRequest request, HttpServletRequest http) {
        surveyService.submit(id, request, clientIpResolver.resolve(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(new SubmitAnswersResponse(true));
    }
}
