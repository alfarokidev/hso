package game.skill;

import game.entity.DamageType;
import game.entity.base.LivingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Context object for damage calculations and combat events
 * Contains all information about a damage instance from start to finish
 */

@Getter
@Setter
public class DamageContext {
    /** Entity dealing the damage */
    private LivingEntity attacker;

    /** Entity receiving the damage */
    private LivingEntity defender;

    /** Skill used (null for basic attacks) */
    private SkillEntity skill;

    /** Type of damage (Physical, Magical, True) */
    private DamageType damageType;

    /** Raw damage before mitigation/reduction */
    private int damage;

    /** Final damage after mitigation/reduction */
    private int finalDamage;

    private boolean penetration;

    private int defenderHpBeforeHit;

    /** Whether the attack was a critical hit, penetration, evade, etc.. put each separate damage here
     * DamageEffect(idEffect, damageEffect)
     * */
    private List<DamageEffect> effect = new ArrayList<>();


    /**
     * Create damage context with initial values
     */
    public DamageContext(LivingEntity attacker, LivingEntity defender, int damage, DamageType type) {

        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.finalDamage = damage; // Initialize final damage same as raw
        this.damageType = type;
    }

    /**
     * Create damage context for skill damage
     */
    public DamageContext(LivingEntity attacker, LivingEntity defender, SkillEntity skill, int damage) {
        this(attacker, defender, damage, skill != null ? skill.getDamageType() : DamageType.PHYSICAL);
        this.skill = skill;
    }

    public void addEffect(DamageEffect effect) {
        this.effect.add(effect);
    }

    /**
     * Get damage reduction percentage
     */
    public int getDamageReduction() {
        if (damage == 0) return 0;
        return ((damage - finalDamage) * 100) / damage;
    }


}