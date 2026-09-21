package rs.serbianelection2026.backend.poll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(@NotBlank @Size(max = 2000) String note) {
}
