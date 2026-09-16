package rs.serbianelection2026.backend.election.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.serbianelection2026.backend.election.dto.ElectionResponse;
import rs.serbianelection2026.backend.election.dto.ElectoralListResponse;
import rs.serbianelection2026.backend.election.mapper.ElectionMapper;
import rs.serbianelection2026.backend.election.mapper.ElectoralListMapper;
import rs.serbianelection2026.backend.election.service.ElectionService;

@RestController
@RequestMapping("/api/v1/elections")
public class ElectionController {

    private final ElectionService electionService;
    private final ElectionMapper electionMapper;
    private final ElectoralListMapper electoralListMapper;

    public ElectionController(
            ElectionService electionService, ElectionMapper electionMapper, ElectoralListMapper electoralListMapper) {
        this.electionService = electionService;
        this.electionMapper = electionMapper;
        this.electoralListMapper = electoralListMapper;
    }

    @GetMapping("/current")
    public ElectionResponse getCurrentElection() {
        return electionMapper.toResponse(electionService.getCurrentElection());
    }

    @GetMapping("/current/lists")
    public List<ElectoralListResponse> getCurrentElectoralLists() {
        return electionService.getCurrentElectoralLists().stream()
                .map(electoralListMapper::toResponse)
                .toList();
    }
}
