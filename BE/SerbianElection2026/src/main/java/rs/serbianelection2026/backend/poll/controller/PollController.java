package rs.serbianelection2026.backend.poll.controller;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.common.dto.PageResponse;
import rs.serbianelection2026.backend.poll.dto.PollResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterResponse;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollResult;
import rs.serbianelection2026.backend.poll.mapper.PollMapper;
import rs.serbianelection2026.backend.poll.service.PollService;

@RestController
@RequestMapping("/api/v1")
public class PollController {

    private final PollService pollService;
    private final PollMapper pollMapper;

    public PollController(PollService pollService, PollMapper pollMapper) {
        this.pollService = pollService;
        this.pollMapper = pollMapper;
    }

    @GetMapping("/polls")
    public PageResponse<PollResponse> getPolls(
            @RequestParam(required = false) String pollster, @PageableDefault(size = 20) Pageable pageable) {
        Page<Poll> page = pollService.getApprovedPolls(pollster, pageable);
        Map<Long, List<PollResult>> results = pollService.getResultsByPollId(page.getContent());
        return PageResponse.of(
                page.map(poll -> pollMapper.toResponse(poll, results.getOrDefault(poll.getId(), List.of()))));
    }

    @GetMapping("/polls/{id}")
    public PollResponse getPoll(@PathVariable Long id) {
        Poll poll = pollService.getApprovedPoll(id);
        return pollMapper.toResponse(poll, pollService.getResultsByPollId(List.of(poll)).getOrDefault(id, List.of()));
    }

    @GetMapping("/pollsters")
    public List<PollsterResponse> getPollsters() {
        Map<Long, Long> counts = pollService.getApprovedPollCounts();
        return pollService.getActivePollsters().stream()
                .map(p -> pollMapper.toPollsterResponse(p, counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }
}
