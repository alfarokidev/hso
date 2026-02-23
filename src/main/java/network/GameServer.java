package network;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import manager.SessionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GameServer {

    private static volatile GameServer instance;

    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * -- GETTER --
     *  Get session manager
     */
    // Core managers
    @Getter
    private SessionManager sessionManager;
    private ExecutorService acceptorPool;

    private Thread acceptorThread;

    private GameServer(int port) {
        this.port = port;
    }


    public static GameServer getInstance(int port) {
        if (instance == null) {
            synchronized (GameServer.class) {
                if (instance == null) {
                    instance = new GameServer(port);
                }
            }
        }
        return instance;
    }

    /**
     * Initialize and start the server
     */
    public void start() {
        if (running.get()) {
            log.warn("Server is already running");
            return;
        }

        log.info("Starting Game Server on port {}...", port);

        try {
            // Initialize core managers
            initializeManagers();

            // Create server socket
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            running.set(true);

            // Start acceptor thread
            acceptorThread = new Thread(this::acceptLoop, "Acceptor");
            acceptorThread.start();

            log.info("Game Server started successfully on port {}", port);
            log.info("Waiting for connections...");

        } catch (IOException e) {
            log.error("Failed to start server", e);
            shutdown();
        }
    }

    /**
     * Initialize all managers
     */
    private void initializeManagers() {


        // Initialize SessionManager with config
        int maxSessionsPerIp = 50;
        long sessionTimeout = 300000; // 5 minutes
        sessionManager = new SessionManager(maxSessionsPerIp, sessionTimeout);

        // Initialize thread pool for handling new connections
        acceptorPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "ConnectionHandler");
                    t.setDaemon(false);
                    return t;
                }
        );

        log.info("Managers initialized successfully");
    }

    /**
     * Accept incoming connections
     */
    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                String clientIp = socket.getInetAddress().getHostAddress();

                log.info("New connection from: {}", clientIp);

                // Check if IP has reached connection limit
                if (sessionManager.isIpLimitReached(clientIp)) {
                    log.warn("Connection rejected: IP limit reached for {}", clientIp);
                    socket.close();
                    continue;
                }

                // Handle connection in thread pool
                acceptorPool.submit(() -> handleNewConnection(socket));

            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error accepting connection", e);
                }
            }
        }
    }

    /**
     * Handle new connection
     */
    private void handleNewConnection(Socket socket) {
        Session session = null;
        try {
            // Create new session
            session = new Session(socket, this);

            // Register session
            if (!sessionManager.register(session)) {
                log.warn("Failed to register session from {}", socket.getInetAddress().getHostAddress());
                session.close();
                return;
            }

            // Start session threads
            session.start();

            log.info("Session started for {}", session.getIpAddress());

        } catch (IOException e) {
            log.error("Error creating session", e);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Unregister a session (called by Session.close())
     */
    public void unregister(Session session) {
        sessionManager.unregister(session);
    }

    /**
     * Broadcast message to all sessions
     */
    public void broadcast(Message message) {
        sessionManager.broadcast(message);
    }

    /**
     * Get server statistics
     */
    public ServerStats getStats() {
        SessionManager.SessionStats sessionStats = sessionManager.getStats();
        return new ServerStats(
                running.get(),
                port,
                sessionStats.totalSessions(),
                sessionStats.uniqueIps()
        );
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Shutdown the server
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        log.info("Shutting down Game Server...");

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }

        // Wait for acceptor thread
        if (acceptorThread != null) {
            acceptorThread.interrupt();
            try {
                acceptorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown session manager (this closes all sessions)
        if (sessionManager != null) {
            sessionManager.shutdown();
        }

        // Shutdown acceptor pool
        if (acceptorPool != null) {
            acceptorPool.shutdown();
            try {
                if (!acceptorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    acceptorPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                acceptorPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Game Server shutdown complete");
    }

    /**
         * Server statistics holder
         */
        public record ServerStats(boolean running, int port, int activeSessions, int uniqueIps) {

        @Override
            public String toString() {
                return String.format("ServerStats{running=%s, port=%d, activeSessions=%d, uniqueIps=%d}",
                        running, port, activeSessions, uniqueIps);
            }
        }

}