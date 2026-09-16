package rs.serbianelection2026.backend.common.exception;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
}
