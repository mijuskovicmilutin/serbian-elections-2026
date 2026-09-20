package rs.serbianelection2026.backend.poll.dto;

public record PollsterResponse(String slug, String name, String kind, String website, long approvedPollCount) {
}
