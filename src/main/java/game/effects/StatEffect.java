package game.effects;

import game.buff.BuffEffect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import service.NetworkService;

import java.util.List;


/**
 * A passive effect that applies one or more StatModifiers while active.
 * Examples: +DEF buff, -ATK debuff, +HP% medal bonus.
 * <p>
 * Stats are applied via recalculateStats() — Stats.recalculateStats() reads
 * active modifiers from EffectManager during each recalculation pass.
 */
@Slf4j
@Getter
public class StatEffect extends Effect implements StatModifierProvider {

    private final List<StatModifier> modifiers;

    public StatEffect(int effectId, int sourceId, EffectType type,
                      int durationMs, List<StatModifier> modifiers) {
        super(effectId, sourceId, type, durationMs);
        this.modifiers = List.copyOf(modifiers);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onApply(EffectContext ctx) {
        ctx.getEntity().getZone().broadcast(
                n -> {

                    NetworkService.gI().sendEffect(n, 2, this, ctx.getEntity());

                    var buff = new BuffEffect(
                            1,
                            31,
                            4,
                            durationMs
                    );
                    modifiers.forEach(mod -> buff.getStatModifiers().put(mod.getType(), mod.getValue()));
                    NetworkService.gI().sendBuff(n, ctx.getEntity(), buff);
                }
        );
        ctx.recalculateStats(); // stats will pick up this effect's modifiers
    }

    @Override
    public boolean onTick(EffectContext ctx, int deltaMs) {
        return false; // passive — nothing to tick
    }

    @Override
    public void onExpire(EffectContext ctx) {
        log.debug("{} EXPIRED ", effectId);
        ctx.recalculateStats(); // remove modifiers from stats
    }

    // ==================== FACTORY SHORTCUTS ====================

    public static StatEffect buff(int effectId, int sourceId, int durationMs, StatModifier... mods) {
        return new StatEffect(effectId, sourceId, EffectType.BUFF, durationMs, List.of(mods));
    }

    public static StatEffect debuff(int effectId, int sourceId, int durationMs, StatModifier... mods) {
        return new StatEffect(effectId, sourceId, EffectType.DEBUFF, durationMs, List.of(mods));
    }

    public static StatEffect medal(int effectId, int sourceId, StatModifier... mods) {
        return new StatEffect(effectId, sourceId, EffectType.MEDAL, -1, List.of(mods)); // permanent
    }
}