package game.effects;


import game.entity.base.LivingEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active effects on a LivingEntity.
 * <p>
 * Key design:
 * - Keyed by effectId → only one instance per effect type (refresh on re-apply)
 * - Separate tracking for DEBUFF/BUFF/MEDAL for fast queries
 * - update() drives all lifecycle callbacks
 * - getActiveModifiers() feeds into Stats.recalculateStats()
 */
@Slf4j
public class EffectManager {

    // effectId → active effect
    private final Map<Integer, Effect> effects = new ConcurrentHashMap<>();
    private final LivingEntity owner;

    public EffectManager(LivingEntity owner) {
        this.owner = owner;
    }


    /**
     * Applies an effect. If an effect with the same effectId already exists,
     * it refreshes (resets duration) instead of stacking.
     */
    public void apply(Effect newEffect) {
        EffectContext ctx = new EffectContext(owner);
        Effect existing = effects.get(newEffect.getEffectId());

        if (existing != null && existing.isActive()) {
            log.debug("[EffectManager] Refreshing effect {} on {}", newEffect.getEffectId(), owner.getId());
            existing.refresh(ctx);
            return;
        }

        effects.put(newEffect.getEffectId(), newEffect);
        newEffect.onApply(ctx);
        log.debug("[EffectManager] Applied effect {} ({}) on {}", newEffect.getEffectId(), newEffect.getType(), owner.getId());
    }

    // ==================== REMOVE ====================

    /**
     * Forcibly remove an effect by its id (e.g. cleanse, purge).
     */
    public void remove(int effectId) {
        Effect effect = effects.remove(effectId);
        if (effect != null && effect.isActive()) {
            effect.setActive(false);
            effect.onExpire(new EffectContext(owner));
            log.debug("[EffectManager] Removed effect {} from {}", effectId, owner.getId());
        }
    }

    /**
     * Remove all DEBUFF effects (e.g. cleanse skill).
     */
    public void cleanseDebuffs() {
        effects.values().stream()
                .filter(e -> e.isActive() && e.getType() == EffectType.DEBUFF)
                .forEach(e -> remove(e.getEffectId()));
    }

    /**
     * Remove all BUFF effects (e.g. dispel skill).
     */
    public void dispelBuffs() {
        effects.values().stream()
                .filter(e -> e.isActive() && e.getType() == EffectType.BUFF)
                .forEach(e -> remove(e.getEffectId()));
    }

    /**
     * Remove all non-permanent effects (death / zone change).
     */
    public void clearAll() {
        EffectContext ctx = new EffectContext(owner);
        effects.values().forEach(e -> {
            if (!e.isPermanent()) {
                e.setActive(false);
                e.onExpire(ctx);
            }
        });
        effects.entrySet().removeIf(entry -> !entry.getValue().isPermanent());
    }

    // ==================== UPDATE ====================

    /**
     * Called every game tick from LivingEntity.onUpdate().
     * Advances timers, fires periodic ticks, removes expired effects.
     */
    public void update(int deltaMs) {
        if (effects.isEmpty()) return;

        EffectContext ctx = new EffectContext(owner);
        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, Effect> entry : effects.entrySet()) {
            Effect effect = entry.getValue();
            if (!effect.isActive()) {
                toRemove.add(entry.getKey());
                continue;
            }

            boolean expired = effect.update(ctx, deltaMs);
            if (expired) toRemove.add(entry.getKey());
        }

        toRemove.forEach(effects::remove);
    }

    // ==================== QUERIES ====================

    /**
     * Returns all active StatModifiers from BUFF/DEBUFF/MEDAL effects.
     */
    public List<StatModifier> getActiveModifiers() {
        List<StatModifier> result = new ArrayList<>();
        for (Effect e : effects.values()) {
            if (e.isActive() && e instanceof StatModifierProvider provider) {
                result.addAll(provider.getModifiers());
            }
        }
        return result;
    }

    public boolean isStunned() {
        return effects.values().stream()
                .anyMatch(e -> e.isActive() && e instanceof StunEffect);
    }

    public boolean hasEffect(int effectId) {
        Effect e = effects.get(effectId);
        return e != null && e.isActive();
    }

    public boolean hasEffectType(EffectType type) {
        return effects.values().stream().anyMatch(e -> e.isActive() && e.getType() == type);
    }

    public Collection<Effect> getAll() {
        return Collections.unmodifiableCollection(effects.values());
    }

    public int getEffectCount() {
        return (int) effects.values().stream().filter(Effect::isActive).count();
    }
}