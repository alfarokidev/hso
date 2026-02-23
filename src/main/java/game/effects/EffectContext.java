package game.effects;


import game.entity.base.LivingEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import service.NetworkService;

/**
 * Carries the entity being affected into Effect lifecycle callbacks.
 * Keeps Effect decoupled from direct LivingEntity imports where possible.
 */
@Getter
@RequiredArgsConstructor
public class EffectContext {

    private final LivingEntity entity;

    // Convenience delegates

    public void recalculateStats() {
        entity.recalculateStats();
    }

    public void dealDamage(int dmg) {
        // Direct internal HP reduction — bypasses full damage pipeline
        // to avoid feedback loops (poison shouldn't trigger lifesteal etc.)
        int current = entity.getHp();
        entity.setHp(Math.max(0, current - dmg));
        if (entity.isDead()) entity.die(null);
    }

    public void restoreHp(int amount) {
        entity.restoreHp(amount);
    }

    public void restoreMp(int amount) {
        entity.restoreMp(amount);
    }
}