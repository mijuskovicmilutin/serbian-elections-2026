package rs.serbianelection2026.backend.election.mapper;

import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.election.dto.ElectionResponse;
import rs.serbianelection2026.backend.election.entity.Election;

@Component
public class ElectionMapper {

    public ElectionResponse toResponse(Election election) {
        return new ElectionResponse(election.getId(), election.getName(), election.getElectionDate());
    }
}
