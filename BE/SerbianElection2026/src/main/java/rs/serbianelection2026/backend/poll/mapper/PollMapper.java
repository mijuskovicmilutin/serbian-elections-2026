package rs.serbianelection2026.backend.poll.mapper;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.poll.dto.MediaSourceResponse;
import rs.serbianelection2026.backend.poll.dto.PollResponse;
import rs.serbianelection2026.backend.poll.dto.PollResultResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterRefResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterResponse;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.Pollster;
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
        Pollster pollster = poll.getPollster();
        return new PollResponse(
                poll.getId(),
                new PollsterRefResponse(pollster.getSlug(), pollster.getName(), pollster.getKind().name()),
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
