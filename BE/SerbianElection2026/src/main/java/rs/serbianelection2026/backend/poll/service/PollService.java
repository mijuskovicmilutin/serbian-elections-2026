package rs.serbianelection2026.backend.poll.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.serbianelection2026.backend.common.exception.NotFoundException;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.entity.Pollster;
import rs.serbianelection2026.backend.poll.repository.PollRepository;
import rs.serbianelection2026.backend.poll.repository.PollResultRepository;
import rs.serbianelection2026.backend.poll.repository.PollsterRepository;

/**
 * Read side of the polls domain. Only {@link PollStatus#APPROVED} polls ever leave this service:
 * a poll that is still a candidate, a draft or was rejected is indistinguishable from a missing one.
 */
@Slf4j
@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollResultRepository pollResultRepository;
    private final PollsterRepository pollsterRepository;

    public PollService(
            PollRepository pollRepository,
            PollResultRepository pollResultRepository,
            PollsterRepository pollsterRepository) {
        this.pollRepository = pollRepository;
        this.pollResultRepository = pollResultRepository;
        this.pollsterRepository = pollsterRepository;
    }

    @Transactional(readOnly = true)
    public Page<Poll> getApprovedPolls(String pollsterSlug, Pageable pageable) {
        log.info(
                "Fetching approved polls: pollster={}, page={}, size={}",
                pollsterSlug,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (pollsterSlug != null && !pollsterRepository.existsBySlugAndActiveTrue(pollsterSlug)) {
            log.warn("Unknown or inactive pollster requested: slug={}", pollsterSlug);
            throw new NotFoundException("Unknown pollster: " + pollsterSlug);
        }

        Page<Poll> result = pollsterSlug == null
                ? pollRepository.findByStatusOrderByPublishedAtDesc(PollStatus.APPROVED, pageable)
                : pollRepository.findByStatusAndPollster_SlugOrderByPublishedAtDesc(
                        PollStatus.APPROVED, pollsterSlug, pageable);

        log.info(
                "Successfully fetched {} approved poll(s) (page {} of {})",
                result.getNumberOfElements(),
                result.getNumber(),
                result.getTotalPages());
        return result;
    }

    @Transactional(readOnly = true)
    public Poll getApprovedPoll(Long id) {
        log.info("Fetching approved poll: id={}", id);

        Poll poll = pollRepository.findByIdAndStatus(id, PollStatus.APPROVED).orElseThrow(() -> {
            log.warn("No approved poll found: id={}", id);
            return new NotFoundException("Poll not found: " + id);
        });

        log.info("Successfully fetched approved poll: id={}, pollster={}", id, poll.getPollster().getSlug());
        return poll;
    }

    /** Results of the given polls in one query, grouped by poll id and ordered as the source published them. */
    @Transactional(readOnly = true)
    public Map<Long, List<PollResult>> getResultsByPollId(Collection<Poll> polls) {
        if (polls.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = polls.stream().map(Poll::getId).toList();
        log.info("Fetching results for {} poll(s)", ids.size());

        return pollResultRepository.findByPoll_IdInOrderByDisplayOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(result -> result.getPoll().getId()));
    }

    @Transactional(readOnly = true)
    public List<Pollster> getActivePollsters() {
        log.info("Fetching active pollsters");
        List<Pollster> pollsters = pollsterRepository.findByActiveTrueOrderByNameAsc();
        log.info("Successfully fetched {} active pollster(s)", pollsters.size());
        return pollsters;
    }

    /** Number of approved polls per pollster id; pollsters without any are simply absent from the map. */
    @Transactional(readOnly = true)
    public Map<Long, Long> getApprovedPollCounts() {
        Map<Long, Long> counts = new HashMap<>();
        pollRepository
                .countByPollsterForStatus(PollStatus.APPROVED)
                .forEach(row -> counts.put(row.getPollsterId(), row.getPollCount()));
        return counts;
    }
}
