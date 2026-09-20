package rs.serbianelection2026.backend.poll.entity;

/** Review lifecycle of a poll; only APPROVED polls are ever exposed publicly. */
public enum PollStatus {
    DISCOVERED, DRAFT, APPROVED, REJECTED
}
