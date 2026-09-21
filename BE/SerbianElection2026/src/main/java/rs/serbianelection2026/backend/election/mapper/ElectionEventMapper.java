package rs.serbianelection2026.backend.election.mapper;

import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.election.dto.ElectionEventResponse;
import rs.serbianelection2026.backend.election.entity.ElectionEvent;

@Component
public class ElectionEventMapper {

    public ElectionEventResponse toResponse(ElectionEvent event) {
        return new ElectionEventResponse(
                event.getId(),
                event.getType().name(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getSourceUrl());
    }
}
