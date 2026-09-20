package rs.serbianelection2026.backend.poll.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaSourceResponse(String name, String url) {
}
