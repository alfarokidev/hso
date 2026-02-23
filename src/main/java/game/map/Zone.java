package game.map;

import game.entity.base.LivingEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import game.entity.monster.MonsterEntity;
import game.entity.player.PlayerEntity;
import game.entity.Position;
import model.item.BaseItem;
import model.map.MapData;
import service.NetworkService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public class Zone {
    private final int id;
    private final int maxPlayers;

    private final ConcurrentHashMap<Integer, PlayerEntity> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, MonsterEntity> monsters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DropItem> dropItems = new ConcurrentHashMap<>();

    private final AtomicInteger playerCount = new AtomicInteger(0);
    private final AtomicInteger dropIdCounter = new AtomicInteger(0);

    private final ReentrantReadWriteLock capacityLock = new ReentrantReadWriteLock();

    // Drop item configuration
    private static final long DEFAULT_LOCK_TIME_MS = 15_000;      // 10 seconds owner lock
    private static final long DEFAULT_LIFETIME_MS = 30_000;      // 2 minutes total lifetime
    private static final int MAX_DROP_ITEMS = 100;               // Prevent too many drops

    private final MapData mapData;


    public Zone(int id, int maxPlayers, MapData mapData) {
        this.id = id;
        this.maxPlayers = maxPlayers;
        this.mapData = mapData;
    }

    // ==================== PLAYER OPERATIONS ====================

    public boolean addPlayer(PlayerEntity player) {
        if (player == null || players.containsKey(player.getId())) {
            return false;
        }

        capacityLock.writeLock().lock();
        try {
            if (players.containsKey(player.getId()) || playerCount.get() >= maxPlayers) {
                return false;
            }

            if (players.putIfAbsent(player.getId(), player) == null) {
                playerCount.incrementAndGet();
                player.setZone(this);
                log.debug("Player {} added to zone {} ({}/{})",
                        player.getId(), id, playerCount.get(), maxPlayers);
                return true;
            }
            return false;
        } finally {
            capacityLock.writeLock().unlock();
        }
    }

    public boolean removePlayer(PlayerEntity player) {
        return player != null && removePlayer(player.getId());
    }

    public boolean removePlayer(int playerId) {
        capacityLock.writeLock().lock();
        try {
            PlayerEntity removed = players.remove(playerId);
            if (removed != null) {
                removed.setZone(null);

                for (MonsterEntity monster : getMonsters()) {
                    if (monster.getTarget() == removed) {
                        monster.setTarget(null);
                    }
                }

                playerCount.decrementAndGet();
                return true;
            }
            return false;
        } finally {
            capacityLock.writeLock().unlock();
        }
    }

    public void clearPlayers() {
        capacityLock.writeLock().lock();
        try {
            players.values().forEach(p -> p.setZone(null));
            players.clear();
            playerCount.set(0);
            log.debug("All players cleared from zone {}", id);
        } finally {
            capacityLock.writeLock().unlock();
        }
    }

    // ==================== MONSTER OPERATIONS ====================

    public boolean addMonster(MonsterEntity monster) {
        if (monster == null) return false;

        if (monsters.putIfAbsent(monster.getId(), monster) == null) {
            monster.setZone(this);
            monster.onSpawn();
            return true;
        }
        return false;
    }

    public boolean removeMonster(MonsterEntity monster) {
        return monster != null && removeMonster(monster.getId());
    }

    public boolean removeMonster(int monsterId) {
        MonsterEntity removed = monsters.remove(monsterId);
        if (removed != null) {
            removed.setZone(null);
            log.debug("Monster {} removed from zone {}", monsterId, id);
            return true;
        }
        return false;
    }

    public void clearMonsters() {
        monsters.values().forEach(m -> m.setZone(null));
        monsters.clear();
        log.debug("All monsters cleared from zone {}", id);
    }

    // ==================== DROP ITEM OPERATIONS ====================

    /**
     * Drop an item in the zone with default lock and lifetime
     */
    public void dropItem(BaseItem item, int mobId, Position position, int ownerId) {
        dropItem(item, mobId, position, ownerId, DEFAULT_LOCK_TIME_MS, DEFAULT_LIFETIME_MS);
    }

    /**
     * Drop an item in the zone with custom lock and lifetime
     */
    public void dropItem(BaseItem item, int mobId, Position position, int ownerId,
                         long lockMs, long lifetimeMs) {
        if (item == null || position == null) {
            log.warn("Cannot drop null item or null position");
            return;
        }

        // Check drop limit
        if (dropItems.size() >= MAX_DROP_ITEMS) {
            log.warn("Zone {} reached max drop items ({}), cleaning expired items",
                    id, MAX_DROP_ITEMS);
            cleanExpiredDrops();

            // Still full after cleanup
            if (dropItems.size() >= MAX_DROP_ITEMS) {
                log.error("Zone {} drop items still full after cleanup", id);
                return;
            }
        }

        int dropId = dropIdCounter.incrementAndGet();
        DropItem dropItem = new DropItem(dropId, mobId, item, position, ownerId, lockMs, lifetimeMs);

        dropItems.put(dropId, dropItem);

        broadcast(player -> NetworkService.gI().sendDropItem(player, dropItem));
        log.debug("Item {} dropped in zone {} at ({}, {}) by player {} (drop ID: {})",
                item.getName(), id, position.getX(), position.getY(), ownerId, dropId);

    }

    /**
     * Pick up a drop item
     */
    public DropItem pickupItem(int dropId, int playerId) {
        DropItem drop = dropItems.get(dropId);

        if (drop == null) {
            log.debug("Drop item {} not found", dropId);
            return null;
        }

        // Check if expired
        if (drop.isExpired()) {
            dropItems.remove(dropId);
            log.debug("Drop item {} expired, removed", dropId);
            return null;
        }

        // Check if player can pickup
        if (!drop.canPickup(playerId)) {
            long remaining = (drop.getLockUntil() - System.currentTimeMillis()) / 1000;
            log.debug("Player {} cannot pickup drop {} (locked for {}s)",
                    playerId, dropId, remaining);
            return null;
        }

        // Remove and return
        dropItems.remove(dropId);
        log.debug("Player {} picked up drop {} ({})", playerId, dropId, drop.getItem().getName());

        return drop;
    }

    /**
     * Remove a specific drop item
     */
    public boolean removeDropItem(int dropId) {
        DropItem removed = dropItems.remove(dropId);
        if (removed != null) {
            log.debug("Drop item {} removed from zone {}", dropId, id);
            return true;
        }
        return false;
    }

    /**
     * Get drop item by ID
     */
    public DropItem getDropItem(int dropId) {
        return dropItems.get(dropId);
    }

    /**
     * Get all drop items in the zone
     */
    public Collection<DropItem> getDropItems() {
        return dropItems.values();
    }

    /**
     * Get drop items in radius
     */
    public List<DropItem> getDropItemsInRadius(int x, int y, int radius) {
        int r2 = radius * radius;
        return dropItems.values().stream()
                .filter(drop -> distanceSquared(drop.getPosition().getX(),
                        drop.getPosition().getY(), x, y) <= r2)
                .collect(Collectors.toList());
    }

    /**
     * Get drop items near player
     */
    public List<DropItem> getDropItemsNearPlayer(PlayerEntity player, int radius) {
        if (player == null || player.getPosition() == null) {
            return List.of();
        }
        return getDropItemsInRadius(player.getPosition().getX(),
                player.getPosition().getY(), radius);
    }

    /**
     * Clean up expired drop items
     */
    public void cleanExpiredDrops() {
        List<Integer> expiredIds = dropItems.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .map(Map.Entry::getKey)
                .toList();

        expiredIds.forEach(dropItems::remove);

        if (!expiredIds.isEmpty()) {
            log.debug("Cleaned {} expired drops from zone {}", expiredIds.size(), id);
        }

    }

    // ==================== QUERIES ====================

    public PlayerEntity getPlayer(int playerId) {
        return players.get(playerId);
    }

    public Collection<PlayerEntity> getPlayers() {
        return players.values();
    }

    public PlayerEntity findPlayerByName(String username) {
        if (username == null || username.isEmpty()) return null;

        return players.values().stream()
                .filter(p -> username.equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElse(null);
    }

    public PlayerEntity findPlayerById(int id) {
        return players.values().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<PlayerEntity> getPlayersInRadius(LivingEntity entity, int radius) {
        int r2 = radius * radius;
        return players.values().stream()
                .filter(p -> p.getId() != entity.getId())
                .filter(p -> p.distanceTo(entity) <= r2)
                .collect(Collectors.toList());
    }

    public MonsterEntity getMonster(int monsterId) {
        return monsters.get(monsterId);
    }

    public Collection<MonsterEntity> getMonsters() {
        return monsters.values();
    }

    public List<MonsterEntity> getMonstersInRadius(LivingEntity entity, int radius) {
        int r2 = radius * radius;
        return monsters.values().stream()
                .filter(m -> m.distanceTo(entity) <= r2)
                .collect(Collectors.toList());
    }

    // ==================== CAPACITY ====================

    public int getPlayerCount() {
        return playerCount.get();
    }

    public boolean isFull() {
        return playerCount.get() >= maxPlayers;
    }

    public boolean isEmpty() {
        return playerCount.get() == 0;
    }

    public int getAvailableSlots() {
        return Math.max(0, maxPlayers - playerCount.get());
    }

    public double getFillPercentage() {
        return maxPlayers == 0 ? 0.0 : (double) playerCount.get() / maxPlayers * 100.0;
    }

    public boolean hasPlayer(int playerId) {
        return players.containsKey(playerId);
    }

    public int getDropItemCount() {
        return dropItems.size();
    }

    // ==================== BROADCASTING ====================

    public void broadcast(Consumer<PlayerEntity> action) {
        if (action == null) return;

        // Create snapshot to avoid concurrent modification
        List<PlayerEntity> snapshot = List.copyOf(players.values());

        for (PlayerEntity p : snapshot) {
            if (!p.isOnline()) continue;
            if (p.isModeBot()) continue;
            if (p.getZone() != this) continue; // Player left zone during broadcast

            try {
                action.accept(p);
            } catch (Exception e) {
                log.error("Error broadcasting to player {}: {}", p.getId(), e.getMessage(), e);
            }
        }
    }

    public void broadcastExcept(PlayerEntity except, Consumer<PlayerEntity> action) {
        if (action == null) return;

        int exceptId = except != null ? except.getId() : -1;

        // Create snapshot to avoid concurrent modification
        List<PlayerEntity> snapshot = List.copyOf(players.values());

        for (PlayerEntity p : snapshot) {
            if (p.getId() == exceptId) continue;
            if (!p.isOnline()) continue;
            if (p.getZone() != this) continue;

            try {
                action.accept(p);
            } catch (Exception e) {
                log.error("Error broadcasting to player {}: {}", p.getId(), e.getMessage(), e);
            }
        }
    }


    public void broadcastInRadius(LivingEntity entity, int radius, Consumer<PlayerEntity> action) {
        if (action == null) return;

        // getPlayersInRadius already creates a new list, so it's safe
        List<PlayerEntity> playersInRange = getPlayersInRadius(entity, radius).stream().map(obj -> (PlayerEntity) obj).toList();

        for (PlayerEntity p : playersInRange) {
            if (!p.isOnline()) continue;
            if (p.getZone() != this) continue;

            try {
                action.accept(p);
            } catch (Exception e) {
                log.error("Error broadcasting to player {}: {}", p.getId(), e.getMessage(), e);
            }
        }
    }

    // ==================== UPDATE ====================

    /**
     * Update zone state (called periodically)
     * Cleans up expired drops
     */
    public void update(long delta) {

        getPlayers().forEach(p -> {
            if (!p.isOnline()) return;
            if (p.getZone() != this) return;
            p.onUpdate(delta);
        });

        getMonsters().forEach(m -> {
            if (m.getZone() != this) return;
            m.onUpdate(delta);
        });

        cleanExpiredDrops();
    }

    // ==================== UTILITIES ====================

    public boolean validate() {
        int actual = players.size();
        int recorded = playerCount.get();

        if (actual != recorded) {
            log.error("Zone {} integrity error: recorded={}, actual={}", id, recorded, actual);
            playerCount.set(actual);
            return false;
        }
        return true;
    }

    public void cleanup() {
        capacityLock.writeLock().lock();
        try {
            players.values().forEach(p -> p.setZone(null));
            players.clear();
            playerCount.set(0);

            monsters.values().forEach(m -> m.setZone(null));
            monsters.clear();

            dropItems.clear();

            log.info("Zone {} cleaned up", id);
        } finally {
            capacityLock.writeLock().unlock();
        }
    }

    public String getStats() {
        return String.format("Zone[id=%d, players=%d/%d, monsters=%d, drops=%d, fill=%.1f%%]",
                id, playerCount.get(), maxPlayers, monsters.size(),
                dropItems.size(), getFillPercentage());
    }

    private int distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    @Override
    public String toString() {
        return String.format("Zone[id=%d, players=%d/%d, monsters=%d, drops=%d]",
                id, playerCount.get(), maxPlayers, monsters.size(), dropItems.size());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Zone && id == ((Zone) obj).id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}