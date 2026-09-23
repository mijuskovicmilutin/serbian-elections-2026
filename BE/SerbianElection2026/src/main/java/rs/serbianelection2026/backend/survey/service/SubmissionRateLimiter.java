package rs.serbianelection2026.backend.survey.service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sliding-window limit on answers per address. Lives in memory only: the addresses are never written to the
 * database or a log, and disappear on restart.
 */
@Component
public class SubmissionRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_TRACKED_ADDRESSES = 50_000;

    private final ConcurrentMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxPerWindow;

    @Autowired
    public SubmissionRateLimiter(SurveyProperties properties) {
        this(Clock.systemUTC(), properties.getRateLimitPerHour());
    }

    SubmissionRateLimiter(Clock clock, int maxPerWindow) {
        this.clock = clock;
        this.maxPerWindow = maxPerWindow;
    }

    /** True if the address may send one more answer now (and counts it). */
    public boolean tryAcquire(String address) {
        long now = clock.millis();
        long cutoff = now - WINDOW.toMillis();
        Deque<Long> times = hits.computeIfAbsent(address, k -> new ArrayDeque<>());
        boolean allowed;
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() <= cutoff) {
                times.pollFirst();
            }
            allowed = times.size() < maxPerWindow;
            if (allowed) {
                times.addLast(now);
            }
        }
        if (hits.size() > MAX_TRACKED_ADDRESSES) {
            evict(cutoff);
        }
        return allowed;
    }

    private void evict(long cutoff) {
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || last <= cutoff;
            }
        });
        if (hits.size() > MAX_TRACKED_ADDRESSES) {
            hits.clear(); // under a flood it is better to forget than to grow without bound
        }
    }
}
