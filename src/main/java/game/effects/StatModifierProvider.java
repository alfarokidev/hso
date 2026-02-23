package game.effects;


import java.util.List;

/**
 * Implemented by any Effect that carries StatModifiers.
 * EffectManager uses this interface to collect all active modifiers
 * without coupling to specific effect types.
 */
public interface StatModifierProvider {
    List<StatModifier> getModifiers();
}