package game.effects;



import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all status effects applied to LivingEntity.
 *
 * Lifecycle:
 *   onApply()  → called once when effect is first applied
 *   onTick()   → called every tickInterval ms (if isTicking)
 *   onExpire() → called once when duration ends or effect is removed
 */
@Getter
public abstract class Effect {

    protected final int effectId;
    protected final int sourceId;      // caster entity id
    protected final EffectType type;

    protected int durationMs;          // total duration in ms (-1 = permanent)
    protected int elapsedMs;           // how much time has passed

    @Setter
    protected boolean active = true;

    protected Effect(int effectId, int sourceId, EffectType type, int durationMs) {
        this.effectId  = effectId;
        this.sourceId  = sourceId;
        this.type      = type;
        this.durationMs = durationMs;
    }

    // ==================== LIFECYCLE ====================

    /** Called when the effect is first applied to an entity. */
    public abstract void onApply(EffectContext ctx);

    /**
     * Called every game tick. Return true if the effect did something
     * (used by EffectManager to broadcast updates if needed).
     */
    public abstract boolean onTick(EffectContext ctx, int deltaMs);

    /** Called when the effect expires naturally or is forcibly removed. */
    public abstract void onExpire(EffectContext ctx);

    // ==================== TICK HANDLING ====================

    /**
     * Advances time on this effect. Returns true when the effect has expired.
     * EffectManager calls this every update cycle.
     */
    public final boolean update(EffectContext ctx, int deltaMs) {
        if (!active) return true;

        if (durationMs > 0) {
            elapsedMs += deltaMs;
            if (elapsedMs >= durationMs) {
                active = false;
                onExpire(ctx);
                return true; // expired
            }
        }

        onTick(ctx, deltaMs);
        return false;
    }

    // ==================== HELPERS ====================

    public int getRemainingMs()  { return Math.max(0, durationMs - elapsedMs); }
    public boolean isPermanent() { return durationMs < 0; }
    public boolean isExpired()   { return !active; }

    /** Refresh duration — resets elapsed time, re-applies modifiers. */
    public void refresh(EffectContext ctx) {
        onExpire(ctx);
        elapsedMs = 0;
        active    = true;
        onApply(ctx);
    }
}