

import database.DataSaver;
import game.event.EventManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ServerLoop {

    private static final int TARGET_TPS = 20;
    private static final long TICK_MS = 50; // 1000 / 20 TPS
    private static final long SAVE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    private final ScheduledExecutorService scheduler;
    private final ExecutorService saveExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "AutoSave"));

    private final AtomicBoolean running;

    @Getter
    private long tickCount;
    private long lastSaveTime;

    private ServerLoop() {
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "GameServer-Tick");
            t.setDaemon(false);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.lastSaveTime = System.currentTimeMillis();
    }

    private static class Holder {
        private static final ServerLoop INSTANCE = new ServerLoop();
    }

    public static ServerLoop getInstance() {
        return Holder.INSTANCE;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::tick, 0, TICK_MS, TimeUnit.MILLISECONDS);
            log.info("ServerLoop started at {} TPS ({}ms per tick)", TARGET_TPS, TICK_MS);
        } else {
            log.warn("ServerLoop already running");
        }
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        log.info("Stopping ServerLoop...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Forcing scheduler shutdown");
                scheduler.shutdownNow();
            }
            log.info("ServerLoop stopped after {} ticks", tickCount);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("ServerLoop interrupted during shutdown", e);
        }
    }

    private void tick() {
        if (!running.get()) return;

        long tickStart = System.currentTimeMillis();

        try {
            updateWorld();
            updateEvents(tickStart);
            if (tickStart - lastSaveTime >= SAVE_INTERVAL_MS) {
                autoSaveAsync();
                lastSaveTime = tickStart;
            }

            tickCount++;

            long tickDuration = System.currentTimeMillis() - tickStart;
            if (tickDuration > TICK_MS) {
                log.warn("Tick #{} took {}ms (target: {}ms)", tickCount, tickDuration, TICK_MS);
            }

        } catch (Exception e) {
            log.error("Critical error in tick #{}", tickCount, e);
        }
    }

    private void updateWorld() {
        WorldManager.getInstance().gameMaps.values()
                .forEach(map -> map.update(TICK_MS));
    }

    private void updateEvents(long currentTime) {
        EventManager.getInstance().onUpdate(currentTime);
    }

    private void autoSaveAsync() {
        saveExecutor.submit(() -> {
            AtomicInteger saved = new AtomicInteger();
            long start = System.currentTimeMillis();

            WorldManager.getInstance().gameMaps.values().forEach(map ->
                    map.getZones().forEach(zone -> {
                        var snapshot = List.copyOf(zone.getPlayers());
                        for (var player : snapshot) {
                            if (player.isClone()) continue;

                            try {
                                DataSaver.savePlayerData(player);

                                saved.incrementAndGet();
                            } catch (Exception e) {
                                log.error("Save failed {}", player.getId(), e);
                            }
                        }
                    })
            );

            DataSaver.saveGlobalData();

            log.debug("Auto-saved {} players in {}ms", saved, System.currentTimeMillis() - start);
        });
    }

    public int getTPS() {
        return TARGET_TPS;
    }
}