package rs.serbianelection2026.backend.poll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MediaSourceInput(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String url) {
}
