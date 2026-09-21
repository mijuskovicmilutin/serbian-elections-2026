package rs.serbianelection2026.backend.poll.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.common.dto.PageResponse;
import rs.serbianelection2026.backend.poll.dto.AdminPollResponse;
import rs.serbianelection2026.backend.poll.dto.AdminPollSummaryResponse;
import rs.serbianelection2026.backend.poll.dto.PollInput;
import rs.serbianelection2026.backend.poll.dto.PollSourceStatusResponse;
import rs.serbianelection2026.backend.poll.dto.PollsterResponse;
import rs.serbianelection2026.backend.poll.dto.RejectRequest;
import rs.serbianelection2026.backend.poll.entity.Poll;
import rs.serbianelection2026.backend.poll.entity.PollStatus;
import rs.serbianelection2026.backend.poll.mapper.PollMapper;
import rs.serbianelection2026.backend.poll.service.AdminPollService;
import rs.serbianelection2026.backend.poll.service.PollService;

/** Review workflow for polls. Guarded by {@code AdminKeyFilter}; never reachable without the admin key. */
@RestController
@RequestMapping("/internal")
public class AdminPollController {

    private final AdminPollService adminPollService;
    private final PollService pollService;
    private final PollMapper pollMapper;

    public AdminPollController(AdminPollService adminPollService, PollService pollService, PollMapper pollMapper) {
        this.adminPollService = adminPollService;
        this.pollService = pollService;
        this.pollMapper = pollMapper;
    }

    @GetMapping("/polls")
    public PageResponse<AdminPollSummaryResponse> list(
            @RequestParam(name = "status", required = false) List<PollStatus> statuses,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<Poll> page = adminPollService.list(statuses, pageable);
        return PageResponse.of(page.map(pollMapper::toAdminSummary));
    }

    @GetMapping("/polls/{id}")
    public AdminPollResponse get(@PathVariable Long id) {
        return pollMapper.toAdminResponse(adminPollService.get(id));
    }

    @PostMapping("/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPollResponse create(@Valid @RequestBody PollInput input) {
        return pollMapper.toAdminResponse(adminPollService.create(input));
    }

    @PutMapping("/polls/{id}")
    public AdminPollResponse update(@PathVariable Long id, @Valid @RequestBody PollInput input) {
        return pollMapper.toAdminResponse(adminPollService.update(id, input));
    }

    @PostMapping("/polls/{id}/approve")
    public AdminPollResponse approve(@PathVariable Long id) {
        return pollMapper.toAdminResponse(adminPollService.approve(id));
    }

    @PostMapping("/polls/{id}/reject")
    public AdminPollResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return pollMapper.toAdminResponse(adminPollService.reject(id, request.note()));
    }

    @GetMapping("/poll-sources")
    public List<PollSourceStatusResponse> pollSources() {
        return adminPollService.getDiscoverySources().stream().map(pollMapper::toSourceStatus).toList();
    }

    @GetMapping("/pollsters")
    public List<PollsterResponse> pollsters() {
        Map<Long, Long> counts = pollService.getApprovedPollCounts();
        return pollService.getActivePollsters().stream()
                .map(p -> pollMapper.toPollsterResponse(p, counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }
}
