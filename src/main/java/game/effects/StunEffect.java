package game.effects;


import service.NetworkService;

/**
 * Stuns the entity — sets isMoving=false, blocks attack/skill actions.
 * LivingEntity checks {@code isStunned()} via EffectManager before any action.
 */
public class StunEffect extends Effect {

    public StunEffect(int effectId, int sourceId, int durationMs) {
        super(effectId, sourceId, EffectType.DEBUFF, durationMs);
    }

    @Override
    public void onApply(EffectContext ctx) {
        ctx.getEntity().setMoving(false);
        ctx.getEntity().getZone().broadcast(notify ->
                NetworkService.gI().sendEffServer(notify, ctx.getEntity(), ctx.getEntity(), effectId, durationMs/1000, 0)
        );
    }

    @Override
    public boolean onTick(EffectContext ctx, int deltaMs) {
        return false; // purely state-based
    }

    @Override
    public void onExpire(EffectContext ctx) {
        // movement restored by entity logic — no auto-restore needed
    }
}