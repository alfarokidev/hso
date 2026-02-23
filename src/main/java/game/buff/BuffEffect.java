package game.buff;

import game.stat.StatType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single buff effect with stats and duration
 */
@Getter
@Setter
public class BuffEffect {
    private int id;              // Unique identifier (e.g., "fire_shield_001")
    private int iconId;             // Icon for UI
    private int type;          // 1 = SELF BUF OR TEAM BUFF 2 = NEGATIVE (STUN, BURN, POISON, FREEZE, ETC)
    private long startTime;         // When buff was applied
    private int duration;          // Duration in milliseconds

    private Map<StatType, Integer> statModifiers; // Stats this buff provides
    private int sourceEntityId;     // Who applied this buff

    public BuffEffect(int id, int type, int duration) {
        this(id, -1,type, duration);
    }

    public BuffEffect(int id, int iconId, int type, int duration) {
        this.id = id;
        this.iconId = iconId;
        this.type = type;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.statModifiers = new HashMap<>();
    }

    /**
     * Add a stat modifier to this buff
     */
    public void addStatModifier(StatType statType, int value) {
        statModifiers.put(statType, value);
    }

    /**
     * Check if buff has expired
     */
    public boolean isExpired() {
        if (duration <= 0) return false; // Permanent buff
        return System.currentTimeMillis() - startTime >= duration;
    }

    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        if (duration <= 0) return -1; // Permanent
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }

    /**
     * Refresh the buff duration (restart timer)
     */
    public void refresh() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Check if this buff can stack with another
     */
    public boolean canStackWith(BuffEffect other) {
        return !(this.id == other.id);
    }
}