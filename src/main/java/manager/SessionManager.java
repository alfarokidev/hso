package manager;

import database.DataSaver;
import game.party.Party;
import game.party.PartyManager;
import lombok.extern.slf4j.Slf4j;
import network.Message;
import network.Session;
import game.entity.player.PlayerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class SessionManager {

    private final Map<String, Session> sessionsByIp = new ConcurrentHashMap<>();
    private final Map<Integer, Session> sessionsById = new ConcurrentHashMap<>();
    private final AtomicInteger sessionIdGenerator = new AtomicInteger(0);

    private final ScheduledExecutorService cleanupScheduler;
    private final int maxSessionsPerIp;
    private final long sessionTimeoutMs;

    public SessionManager() {
        this(50, 300000); // 50 sessions per IP, 5 minute timeout
    }

    public SessionManager(int maxSessionsPerIp, long sessionTimeoutMs) {
        this.maxSessionsPerIp = maxSessionsPerIp;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionCleanup");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic cleanup every 60 seconds
        cleanupScheduler.scheduleAtFixedRate(this::cleanupInactiveSessions, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Register a new session
     */
    public boolean register(Session session) {
        if (session == null) {
            log.warn("Attempted to register null session");
            return false;
        }

        String ip = session.getIpAddress();

        // Check IP connection limit - FIX: use startsWith to count all sessions from this IP
        long connectionsFromIp = sessionsByIp.keySet().stream()
                .filter(key -> key.startsWith(ip + ":"))
                .count();

        if (connectionsFromIp >= maxSessionsPerIp) {
            log.warn("Connection limit reached for IP: {} ({}/{})", ip, connectionsFromIp, maxSessionsPerIp);
            return false;
        }

        int sessionId = sessionIdGenerator.incrementAndGet();
        sessionsById.put(sessionId, session);
        sessionsByIp.put(ip + ":" + sessionId, session);

        log.info("Session registered: ID={}, IP={}, Total sessions: {}",
                sessionId, ip, sessionsById.size());

        return true;
    }

    /**
     * Unregister a session
     */
    public void unregister(Session session) {
        if (session == null) {
            return;
        }

        String ip = session.getIpAddress();

        // Remove from both maps
        sessionsById.values().removeIf(s -> s == session);
        sessionsByIp.entrySet().removeIf(entry -> entry.getValue() == session);

        PlayerEntity p = session.getPlayer();
        if (p == null) {
            return;
        }
        if (!p.isModeBot()) {
            WorldManager.getInstance().leaveMap(p);
            if (p.getParty() != null) {
                Party party = p.getParty();
                PartyManager.getInstance().leaveParty(party.getId(), p);
                party.broadcastPartyInfo();
            }
        }

        session.unbindPlayer();

        DataSaver.savePlayerData(p);

        log.info("Session unregistered: IP={}, Remaining sessions: {}", ip, sessionsById.size());
    }

    /**
     * Get session by ID
     */
    public Session getSessionById(int sessionId) {
        return sessionsById.get(sessionId);
    }

    /**
     * Get all sessions from a specific IP
     */
    public Collection<Session> getSessionsByIp(String ip) {
        return sessionsByIp.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ip + ":"))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Get all active sessions
     */
    public Collection<Session> getAllSessions() {
        return sessionsById.values();
    }

    /**
     * Get total number of active sessions
     */
    public int getSessionCount() {
        return sessionsById.size();
    }

    /**
     * Check if an IP has reached connection limit
     */
    public boolean isIpLimitReached(String ip) {
        long count = sessionsByIp.keySet().stream()
                .filter(key -> key.startsWith(ip + ":"))
                .count();
        return count >= maxSessionsPerIp;
    }

    /**
     * Broadcast message to all sessions
     */
    public void broadcast(Message message) {
        sessionsById.values().forEach(session -> session.send(message));
        log.debug("Broadcasted message to {} sessions", sessionsById.size());
    }

    /**
     * Broadcast message to all sessions except one
     */
    public void broadcastExcept(Message message, Session excludeSession) {
        sessionsById.values().stream()
                .filter(session -> session != excludeSession)
                .forEach(session -> session.send(message));
    }

    /**
     * Broadcast message with custom filter
     */
    public void broadcastFiltered(Message message, java.util.function.Predicate<Session> filter) {
        sessionsById.values().stream()
                .filter(filter)
                .forEach(session -> session.send(message));
    }

    /**
     * Execute action on all sessions
     */
    public void forEachSession(Consumer<Session> action) {
        sessionsById.values().forEach(action);
    }

    /**
     * Close all sessions
     */
    public void closeAll() {
        log.info("Closing all sessions: {}", sessionsById.size());
        sessionsById.values().forEach(Session::close);
        sessionsById.clear();
        sessionsByIp.clear();
    }

    /**
     * Close all sessions from a specific IP
     */
    public void closeSessionsByIp(String ip) {
        Collection<Session> sessions = getSessionsByIp(ip);
        log.info("Closing {} sessions from IP: {}", sessions.size(), ip);
        sessions.forEach(Session::close);
    }

    /**
     * Close specific session by ID
     */
    public void closeSessionById(int sessionId) {
        Session session = sessionsById.get(sessionId);
        if (session != null) {
            log.info("Closing session: {}", sessionId);
            session.close();
        }
    }

    /**
     * Periodic cleanup of inactive/dead sessions
     */
    private void cleanupInactiveSessions() {
        try {
            int initialCount = sessionsById.size();

            // Remove sessions that are closed or disconnected
            sessionsById.entrySet().removeIf(entry -> {
                Session session = entry.getValue();
                String ip = session.getIpAddress();
                return ip.equals("disconnected");
            });

            sessionsByIp.entrySet().removeIf(entry -> {
                Session session = entry.getValue();
                String ip = session.getIpAddress();
                return ip.equals("disconnected");
            });

            int removedCount = initialCount - sessionsById.size();
            if (removedCount > 0) {
                log.debug("Cleaned up {} inactive sessions. Active sessions: {}",
                        removedCount, sessionsById.size());
            }
        } catch (Exception e) {
            log.error("Error during session cleanup", e);
        }
    }

    /**
     * Get session statistics
     */
    public SessionStats getStats() {
        Map<String, Integer> connectionsByIp = new ConcurrentHashMap<>();

        sessionsByIp.keySet().forEach(key -> {
            String ip = key.substring(0, key.lastIndexOf(':'));
            connectionsByIp.merge(ip, 1, Integer::sum);
        });

        return new SessionStats(
                sessionsById.size(),
                connectionsByIp.size(),
                connectionsByIp
        );
    }

    /**
     * Shutdown the session manager
     */
    public void shutdown() {
        log.info("Shutting down SessionManager");

        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        closeAll();
        log.info("SessionManager shutdown complete");
    }

    /**
     * Session statistics holder
     */
    public record SessionStats(int totalSessions, int uniqueIps, Map<String, Integer> connectionsByIp) {

        @Override
        public String toString() {
            return String.format("SessionStats{totalSessions=%d, uniqueIps=%d, connectionsByIp=%s}",
                    totalSessions, uniqueIps, connectionsByIp);
        }
    }
}