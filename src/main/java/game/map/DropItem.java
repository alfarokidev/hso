package game.map;

import game.entity.Position;
import lombok.Getter;
import model.item.BaseItem;
import model.item.EquipmentItem;
import model.item.ItemCategory;

/**
 * Represents an item dropped on the ground
 * - Owner lock: Only owner can pickup for X seconds
 * - Lifetime: Item expires after X seconds
 */
@Getter
public class DropItem {

    private final int dropId;           // Unique drop ID
    private final int mobId;            // MonsterId
    private final BaseItem item;        // The item
    private final int quantity;         // Item quantity
    private final Position position;    // Drop location

    private final int ownerId;          // Player who dropped it
    private final long lockUntil;       // Time when lock expires (ms)
    private final long expireAt;        // Time when item expires (ms)
    private final long droppedAt;       // Time when dropped (ms)

    /**
     * Create a drop item
     */
    public DropItem(int dropId, int mobId, BaseItem item, Position position,
                    int ownerId, long lockMs, long lifetimeMs) {
        this(dropId, mobId, item, 1, position, ownerId, lockMs, lifetimeMs);
    }

    /**
     * Create a drop item with grade and quantity
     */
    public DropItem(int dropId, int mobId, BaseItem item, int quantity,
                    Position position, int ownerId, long lockMs, long lifetimeMs) {
        long now = System.currentTimeMillis();

        this.dropId = dropId;
        this.mobId = mobId;
        this.item = item;
        this.quantity = quantity;
        this.position = position;
        this.ownerId = ownerId;
        this.lockUntil = now + lockMs;
        this.expireAt = now + lifetimeMs;
        this.droppedAt = now;
    }

    /**
     * Check if a player can pickup this item
     */
    public boolean canPickup(int playerId) {
        // Owner can always pickup
        if (playerId == ownerId) return true;

        // Others can pickup after lock expires
        return System.currentTimeMillis() >= lockUntil;
    }

    /**
     * Check if item has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }

    /**
     * Check if item is still locked
     */
    public boolean isLocked() {
        return System.currentTimeMillis() < lockUntil;
    }

    /**
     * Get remaining lock time in milliseconds
     */
    public long getRemainingLockTime() {
        long remaining = lockUntil - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get remaining lock time in seconds
     */
    public int getRemainingLockSeconds() {
        return (int) Math.ceil(getRemainingLockTime() / 1000.0);
    }

    /**
     * Get remaining lifetime in milliseconds
     */
    public long getRemainingLifetime() {
        long remaining = expireAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get remaining lifetime in seconds
     */
    public int getRemainingLifetimeSeconds() {
        return (int) Math.ceil(getRemainingLifetime() / 1000.0);
    }

    /**
     * Get elapsed time since dropped (ms)
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - droppedAt;
    }

    public int getColor() {
        if (item.getCategory() == ItemCategory.EQUIPMENT) {
            return ((EquipmentItem) item).getColor();
        }

        return 0;
    }

}