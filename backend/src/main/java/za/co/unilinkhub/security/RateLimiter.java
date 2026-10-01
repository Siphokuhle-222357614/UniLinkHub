package za.co.unilinkhub.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import za.co.unilinkhub.common.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A small in-memory sliding-window limiter: "at most N events per key in the last W". Used to slow
 * down password guessing and stop the email-sending endpoints being used to spam someone's inbox.
 *
 * In-memory is enough for one app instance (how UniLinkHub is deployed). If it ever runs on several
 * instances behind a load balancer, move these counters to a shared store such as Redis.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Throws (HTTP 429) if {@code key} has already used its {@code max} events in the window. Does not record anything. */
    public void requireCapacity(String key, int max, Duration window, String whatIsLimited) {
        Instant now = clock.instant();
        Deque<Instant> recent = events.get(key);
        if (recent == null) {
            return;
        }
        synchronized (recent) {
            prune(recent, now, window);
            if (recent.size() >= max) {
                long minutes = Math.max(1, Duration.between(now, recent.peekFirst().plus(window)).toMinutes() + 1);
                throw new TooManyRequestsException(whatIsLimited + " Please wait " + minutes + " minute" + (minutes == 1 ? "" : "s")
                        + " and try again - this limit protects accounts from people guessing passwords or spamming inboxes.");
            }
        }
    }

    public void record(String key, Duration window) {
        Instant now = clock.instant();
        Deque<Instant> recent = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (recent) {
            prune(recent, now, window);
            recent.addLast(now);
        }
    }

    /** Check and record in one step - for actions where every attempt counts, not just failures. */
    public void consume(String key, int max, Duration window, String whatIsLimited) {
        requireCapacity(key, max, window, whatIsLimited);
        record(key, window);
    }

    public void reset(String key) {
        events.remove(key);
    }

    private static void prune(Deque<Instant> recent, Instant now, Duration window) {
        Instant cutoff = now.minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.pollFirst();
        }
    }

    /** Drop keys with no recent events so the map can't grow forever. Windows here are at most an hour. */
    @Scheduled(fixedDelay = 600_000)
    void evictIdleKeys() {
        Instant cutoff = clock.instant().minus(Duration.ofHours(2));
        events.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(cutoff);
            }
        });
    }
}
