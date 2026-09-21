package rs.serbianelection2026.backend.poll.dto;

import java.util.List;

/** {@code missing} lists the publish conditions (codes) not yet met; empty means the poll can be approved. */
public record PollReadiness(boolean ready, List<String> missing, List<PollWarning> warnings) {
}
