package rs.serbianelection2026.backend.poll.mapper;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.poll.dto.AdminPollResponse;
import rs.serbianelection2026.backend.poll.dto.AdminPollResultResponse;
import rs.serbianelection2026.backend.poll.dto.AdminPollSummaryResponse;
import rs.serbianelection2026.backend.poll.dto.MediaSourceResponse;
import rs.serbianelection2026.backend.poll.dto.PollAuditEntryResponse;
import rs.serbianelection2026.backend.poll.dto.PollSourceStatusResponse;
import rs.serbianelection2026.backend.poll.dto.PollResponse;
import rs.serbianelection2026.backend.poll.dto.PollResultResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterRefResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterResponse;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.service.AdminPollService.PollDetail;
import rs.serbianelection2026.backend.poll.service.AdminPollService.SourceStatus;
import rs.serbianelection2026.backend.poll.service.PollReadinessEvaluator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class PollMapper {

    private final ObjectMapper objectMapper;

    public PollMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PollResponse toResponse(Poll poll, List<PollResult> results) {
        return new PollResponse(
                poll.getId(),
                pollsterRef(poll.getPollster()),
                poll.getTitle(),
                poll.getPublishedAt(),
                poll.getFieldworkFrom(),
                poll.getFieldworkTo(),
                poll.getFieldworkNote(),
                poll.getSampleSize(),
                poll.getPopulation(),
                poll.getMethod(),
                poll.getConductedBy(),
                poll.getCommissionedBy(),
                poll.getMarginOfError(),
                poll.getResultBasis() == null ? null : poll.getResultBasis().name(),
                poll.getDecidedSharePct(),
                poll.getUndecidedPct(),
                poll.getWontVotePct(),
                poll.getWillVotePct(),
                poll.getSourceKind().name(),
                poll.getSourceUrl(),
                poll.getOriginalDocumentUrl(),
                parseMediaSources(poll),
                results.stream().map(this::toResultResponse).toList());
    }

    public PollsterResponse toPollsterResponse(Pollster pollster, long approvedPollCount) {
        return new PollsterResponse(
                pollster.getSlug(), pollster.getName(), pollster.getKind().name(), pollster.getWebsite(), approvedPollCount);
    }

    public AdminPollSummaryResponse toAdminSummary(Poll poll) {
        return new AdminPollSummaryResponse(
                poll.getId(),
                pollsterRef(poll.getPollster()),
                poll.getTitle(),
                poll.getPublishedAt(),
                poll.getStatus().name(),
                poll.getSourceKind().name(),
                poll.getModifiedAt());
    }

    public AdminPollResponse toAdminResponse(PollDetail detail) {
        Poll poll = detail.poll();
        return new AdminPollResponse(
                poll.getId(),
                pollsterRef(poll.getPollster()),
                poll.getStatus().name(),
                poll.getTitle(),
                poll.getPublishedAt(),
                poll.getFieldworkFrom(),
                poll.getFieldworkTo(),
                poll.getFieldworkNote(),
                poll.getSampleSize(),
                poll.getPopulation(),
                poll.getMethod(),
                poll.getConductedBy(),
                poll.getCommissionedBy(),
                poll.getMarginOfError(),
                poll.getResultBasis() == null ? null : poll.getResultBasis().name(),
                poll.getDecidedSharePct(),
                poll.getUndecidedPct(),
                poll.getWontVotePct(),
                poll.getWillVotePct(),
                poll.getSourceKind().name(),
                poll.getSourceUrl(),
                poll.getOriginalDocumentUrl(),
                parseMediaSources(poll),
                poll.getReviewedAt(),
                poll.getReviewNote(),
                poll.getScrapedAt(),
                poll.getContentHash(),
                poll.getSourceSnapshot(),
                detail.results().stream().map(this::toAdminResult).toList(),
                PollReadinessEvaluator.evaluate(poll, detail.results()),
                detail.audit().stream()
                        .map(a -> new PollAuditEntryResponse(a.getAction().name(), a.getDetails(), a.getCreatedAt()))
                        .toList());
    }

    public PollSourceStatusResponse toSourceStatus(SourceStatus s) {
        var run = s.lastRun();
        return new PollSourceStatusResponse(
                s.source().name(),
                run == null ? null : run.getStartedAt(),
                run == null ? null : run.getStatus().name(),
                run == null ? null : run.getRecordsFound(),
                run == null ? null : run.getRecordsCreated(),
                run == null ? null : run.getErrorMessage());
    }

    private AdminPollResultResponse toAdminResult(PollResult r) {
        return new AdminPollResultResponse(
                r.getRawOptionName(),
                r.getPercentage(),
                r.getDisplayOrder(),
                r.getOptionKind().name(),
                r.getComposition(),
                r.getElectoralList() == null ? null : r.getElectoralList().getId());
    }

    private PollsterRefResponse pollsterRef(Pollster pollster) {
        return new PollsterRefResponse(pollster.getSlug(), pollster.getName(), pollster.getKind().name());
    }

    private PollResultResponse toResultResponse(PollResult result) {
        return new PollResultResponse(result.getRawOptionName(), result.getPercentage(), result.getDisplayOrder());
    }

    private List<MediaSourceResponse> parseMediaSources(Poll poll) {
        if (poll.getMediaSources() == null || poll.getMediaSources().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(poll.getMediaSources(), new TypeReference<List<MediaSourceResponse>>() {
            });
        } catch (RuntimeException e) {
            log.warn("Unreadable media sources for poll id={}, omitting them", poll.getId(), e);
            return List.of();
        }
    }
}
