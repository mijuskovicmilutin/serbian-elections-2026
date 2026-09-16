package rs.serbianelection2026.backend.election.mapper;

import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.election.dto.ElectoralListResponse;
import rs.serbianelection2026.backend.election.entity.ElectoralList;

@Component
public class ElectoralListMapper {

    public ElectoralListResponse toResponse(ElectoralList list) {
        return new ElectoralListResponse(
                list.getId(),
                list.getName(),
                list.getBallotNumber(),
                list.getStatus().name(),
                list.getSourceUrl(),
                list.getPublishedAt(),
                list.getLastSeenAt());
    }
}
