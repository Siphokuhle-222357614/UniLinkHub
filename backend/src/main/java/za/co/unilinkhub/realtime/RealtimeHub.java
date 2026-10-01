package za.co.unilinkhub.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Pushes events (new notifications, new chat messages) to a user's open browser tabs the moment
 * they happen, over Server-Sent Events, instead of the browser asking every 30 seconds.
 *
 * Each logged-in tab holds one connection. Events are only sent once the database change behind
 * them has committed, so a tab is never told about a message that then gets rolled back.
 * Connections live in memory: fine for one app instance. With several instances, publish through
 * a shared broker (e.g. Redis pub/sub) instead.
 */
@Component
public class RealtimeHub {

    private static final Logger log = LoggerFactory.getLogger(RealtimeHub.class);
    private static final Duration CONNECTION_LIFETIME = Duration.ofMinutes(30);

    private final Map<UUID, Set<SseEmitter>> connections = new ConcurrentHashMap<>();

    public SseEmitter connect(UUID userId) {
        // The browser reconnects automatically when this expires; the limit stops dead tabs piling up.
        SseEmitter emitter = newEmitter(CONNECTION_LIFETIME.toMillis());
        Set<SseEmitter> mine = connections.computeIfAbsent(userId, id -> new CopyOnWriteArraySet<>());
        mine.add(emitter);
        Runnable remove = () -> {
            mine.remove(emitter);
            connections.computeIfPresent(userId, (id, set) -> set.isEmpty() ? null : set);
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        if (!send(emitter, "ready", Map.of("connected", true))) {
            remove.run();
        }
        return emitter;
    }

    /**
     * Sends {@code event} to every open tab of {@code userId} - after the current transaction commits, if there is one.
     * Never throws: a closed tab must not make the action that triggered the event (sending a message...) look failed.
     */
    public void publish(UUID userId, String event, Object data) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliverSafely(userId, event, data);
                }
            });
        } else {
            deliverSafely(userId, event, data);
        }
    }

    private void deliverSafely(UUID userId, String event, Object data) {
        try {
            deliver(userId, event, data);
        } catch (Exception ex) {
            log.warn("Couldn't push '{}' to user {}: {}", event, userId, ex.getMessage());
        }
    }

    /** Overridable so tests can simulate connections that die mid-send. */
    protected SseEmitter newEmitter(long timeoutMillis) {
        return new SseEmitter(timeoutMillis);
    }

    public int openConnections(UUID userId) {
        return connections.getOrDefault(userId, Set.of()).size();
    }

    private void deliver(UUID userId, String event, Object data) {
        Set<SseEmitter> mine = connections.get(userId);
        if (mine == null) return;
        // Each tab on its own: one dead connection must not stop the others getting the event.
        mine.removeIf(emitter -> !send(emitter, event, data));
        connections.computeIfPresent(userId, (id, set) -> set.isEmpty() ? null : set);
    }

    /** @return false if the connection is dead (and has been closed). */
    private boolean send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            return true;
        } catch (Exception ex) {
            close(emitter, ex);
            return false;
        }
    }

    private void close(SseEmitter emitter, Exception cause) {
        // The tab closed or the network dropped. Closing a half-dead connection can itself throw.
        log.debug("Dropping realtime connection: {}", cause.getMessage());
        try {
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // Already gone.
        }
    }

    /** A comment line every 25s keeps proxies (Render's included) from closing idle connections. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        connections.values().forEach(set -> set.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
                return false;
            } catch (Exception ex) {
                close(emitter, ex);
                return true;
            }
        }));
    }
}
