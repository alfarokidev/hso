package game.map;

import game.guild.GuildManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import manager.MonsterManager;
import manager.WorldManager;
import model.map.MapData;
import model.map.Point;
import model.map.Vgo;
import model.monster.GuildMine;
import model.monster.Monster;
import game.entity.monster.MonsterEntity;
import game.entity.player.PlayerEntity;
import game.entity.Position;
import model.player.Player;
import service.PlayerService;


import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public class GameMap {
    private final int id;
    private final MapData mapData;


    private final ConcurrentHashMap<Integer, Zone> zones = new ConcurrentHashMap<>();
    private final AtomicInteger nextMonsterId = new AtomicInteger(3000);
    private final ReentrantLock zoneCreationLock = new ReentrantLock();

    private static final int WARP_RANGE = 45;

    private PathFinding pathFinding;

    public GameMap(int id, MapData mapData) {
        this.id = id;
        this.mapData = mapData;
        zones.put(0, createZone(0));
        pathFinding = new PathFinding(mapData.getTileData());
    }

    public Zone assignZone(PlayerEntity player) {
        // Find available zone
        for (Zone zone : zones.values()) {
            if (!zone.isFull() && zone.addPlayer(player)) {
                return zone;
            }
        }

        // Create new zone if all full
        zoneCreationLock.lock();
        try {
            // Double-check pattern
            for (Zone zone : zones.values()) {
                if (!zone.isFull() && zone.addPlayer(player)) {
                    return zone;
                }
            }

            int zoneId = zones.size() + 1;
            Zone zone = createZone(zoneId);
            zones.put(zoneId, zone);
            zone.addPlayer(player);

            log.info("Created zone {} for map {} (total: {})", zoneId, id, zones.size());
            return zone;
        } finally {
            zoneCreationLock.unlock();
        }
    }

    private Zone createZone(int zoneId) {
        Zone zone = new Zone(zoneId, mapData.getMaxPlayer(), mapData);

        List<Point> spawns = mapData.getMobData();
        if (spawns != null) {
            spawns.forEach(spawn -> {
                Monster template = MonsterManager.getInstance().getMonster(spawn.getId());
                if (template != null) {
                    MonsterEntity monster = createMonster(template, spawn);
                    monster.setMap(this);
                    zone.addMonster(monster);
                }
            });
        }
        return zone;
    }

    private MonsterEntity createMonster(Monster template, Point spawn) {
        MonsterEntity monster = new MonsterEntity();
        monster.setId(nextMonsterId.getAndIncrement());
        monster.setTemplateId(template.getMid());
        monster.setName(template.getName());
        monster.setMaxHp(template.getHp());
        monster.setTypeMove(template.getTypeMove());
        monster.setLevel(template.getLevel());
        monster.setSpawnPosition(new Position((short) spawn.getX(), (short) spawn.getY()));

        return monster;
    }

    public void update(long delta) {
        zones.values().forEach(zone -> {
            zone.update(delta);

        });
    }

    private void cleanupZone(int zoneId) {
        Zone zone = zones.remove(zoneId);
        if (zone != null) {
            zone.cleanup();
            log.info("Cleaned up zone {} from map {}", zoneId, id);
        }
    }

    public Zone getZone(int zoneId) {
        return zones.get(zoneId);
    }

    public Collection<Zone> getZones() {
        return zones.values();
    }

    public int getZoneCount() {
        return zones.size();
    }


    // ==================== PLAYER QUERIES ====================

    public int getPlayerCount() {
        return zones.values().stream().mapToInt(Zone::getPlayerCount).sum();
    }

    public PlayerEntity findPlayer(int playerId) {
        return zones.values().stream()
                .map(z -> z.getPlayer(playerId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public PlayerEntity findPlayer(String username) {
        return zones.values().stream()
                .map(z -> z.findPlayerByName(username))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public List<PlayerEntity> getAllPlayers() {
        return zones.values().stream()
                .flatMap(z -> z.getPlayers().stream())
                .collect(Collectors.toList());
    }

    public boolean hasPlayer(int playerId) {
        return findPlayer(playerId) != null;
    }

    // ==================== WARP POINTS ====================

    public boolean isWarp(short x, short y) {
        return getWarpAt(x, y) != null;
    }

    public Vgo getWarpAt(short x, short y) {
        List<Vgo> warps = mapData.getWarpPoint();
        if (warps == null) return null;

        return warps.stream()
                .filter(w -> inRange(x, y, w.getX(), w.getY()))
                .findFirst()
                .orElse(null);
    }

    private boolean inRange(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) < WARP_RANGE && Math.abs(y1 - y2) < WARP_RANGE;
    }


    // ==================== BROADCASTING ====================

    public void broadcast(Consumer<PlayerEntity> action) {
        if (action != null) {
            zones.values().forEach(z -> z.broadcast(action));
        }
    }

    public void broadcastExcept(PlayerEntity except, Consumer<PlayerEntity> action) {
        if (action != null && except != null) {
            zones.values().forEach(z -> z.broadcastExcept(except, action));
        }
    }

    // ==================== MAP INFO ====================

    public int getWidth() {
        return mapData.getWidth();
    }

    public int getHeight() {
        return mapData.getHeight();
    }

    public String getName() {
        return mapData.getName();
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < mapData.getWidth() * 24 &&
                y >= 0 && y < mapData.getHeight() * 24;
    }


    public Position getNearestWarpPoint(Position position) {
        Position nearestWarp = null;
        double minDistance = Double.MAX_VALUE;
        for (Vgo vgo : mapData.getWarpPoint()) {
            Position warpPos = new Position(mapData.getId(), vgo.getX(), vgo.getY());
            if (position.distanceTo(warpPos) < minDistance) {
                nearestWarp = warpPos;
            }
        }

        return nearestWarp;
    }

    public boolean isBattleMap() {
        return id == 104 || id == 105 || id == 106 || id == 107 || id == 108;
    }

    public void cleanup() {
        zones.values().forEach(Zone::cleanup);
        zones.clear();
        log.info("Map {} cleaned up", id);
    }

}