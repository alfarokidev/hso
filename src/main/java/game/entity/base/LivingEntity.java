package game.entity.base;


import game.buff.BuffEffect;
import game.buff.BuffManager;
import game.effects.EffectManager;
import game.effects.StunEffect;
import game.entity.player.PlayerEntity;
import game.inventory.InventoryManager;
import game.level.ExpCalculator;
import game.skill.DamageContext;
import game.skill.DamageEffect;
import game.skill.SkillEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import game.stat.Stats;
import game.entity.DamageType;
import lombok.extern.slf4j.Slf4j;
import game.effects.Effect;
import model.skill.LvSkill;
import game.stat.StatCalculator;
import service.NetworkService;
import utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class LivingEntity extends GameObject {

    // Core attributes
    protected int level;
    protected long experience;

    protected Stats stats;
    protected InventoryManager inventoryManager;

    protected EffectManager effectManager;
    protected BuffManager buffManager;

    // Health & Resources
    protected int hp;
    protected int maxHp;
    protected int mp;
    protected int maxMp;

    protected byte typePK;
    // Movement
    protected boolean isMoving;

    // Combat
    protected LivingEntity target;
    protected long lastAttackTime;
    protected long attackCooldown;
    protected long lastChangeMapTime;
    protected int moveSpeed;

    protected boolean isClone;
    protected Map<Byte, SkillEntity> skillData;

    private long regenAccumulator = 0;
    private static final long REGEN_INTERVAL = 4000; // 2 second

    protected ExpCalculator expCalculator;


    protected LivingEntity() {
        super();
        this.stats = new Stats();
        this.expCalculator = new ExpCalculator();
        effectManager = new EffectManager(this);
    }

    protected LivingEntity(GameObjectType type) {
        super(type);
        this.stats = new Stats();
        this.expCalculator = new ExpCalculator();
        effectManager = new EffectManager(this);
    }

    // ==================== ABSTRACT METHODS ====================

    public abstract void attack(LivingEntity target);

    public abstract void die(LivingEntity attacker);

    public abstract void respawn();

    public abstract void recalculateStats();

    // ==================== COMBAT METHODS ====================


    public void useSkill(byte skillId, LivingEntity target) {
        if (skillData == null) {
            log.warn("skillData is not initialized!");
            return;
        }

        SkillEntity skill = skillData.get(skillId);
        if (skill == null) {
            log.warn("Skill with given id {} not found!", skillId);
            return;
        }

        // Check cooldown
        if (skill.isOnCooldown()) {
            long remaining = skill.getRemainingCooldown() / 1000;
            log.warn("Skill on cooldown: {}s", remaining);
            return;
        }

        // Check mana cost
        LvSkill levelData = skill.getCurrentLevelData();
        if (levelData != null && levelData.mpCost > 0) {
            if (mp < levelData.mpCost) {
                return;
            }

            consumeMp(levelData.mpCost);
        }
        byte skillType = skill.getType();
        switch (skillType) {
            case 0 -> {
                if (skillId == 17) {
                    // 3 Explode
                    // 4 Stun Akar Pohon
                    // 5 Stun
                    if (this instanceof PlayerEntity p) {
                        StunEffect stunEffect = switch (p.getRole()) {
                            case 0 -> new StunEffect(7, -1, 10000); // DIZZY
                            case 1 -> new StunEffect(4, -1, 10000); // AKAR
                            case 2 -> new StunEffect(5, -1, 10000); // FREEZE
                            case 3 -> new StunEffect(6, -1, 10000); // SLEEP
                            default -> null;
                        };
                        if (stunEffect != null) {
                            target.applyEffect(stunEffect);
                        }
                    }


                } else {
                    dealDamageTo(target, skill);
                }
            }
            case 1 -> {

                LivingEntity buffTarget = (target != null && target.isAlive()) ? target : this;

                BuffEffect buff = skill.createBuff();
                if (buff != null) {
                    buffTarget.applyBuff(buff);

                    // Notify clients
                    if (zone != null) {
                        zone.broadcast(player ->
                                NetworkService.gI().sendBuff(player, buffTarget, buff)
                        );
                    }
                }

            }
        }

        // Mark skill as used
        skill.onUse();

    }

    public void useSkillAoE(byte skillId, LivingEntity primaryTarget, List<LivingEntity> targets) {
        if (skillData == null) {
            log.warn("skillData is not initialized!");
            return;
        }

        SkillEntity skill = skillData.get(skillId);
        if (skill == null) {
            log.warn("Skill with given id {} not found!", skillId);
            return;
        }

        // Check cooldown
        if (skill.isOnCooldown()) {
            long remaining = skill.getRemainingCooldown() / 1000;
            log.warn("Skill on cooldown: {}s", remaining);
            return;
        }

        // Check mana cost (only once for AoE)
        LvSkill levelData = skill.getCurrentLevelData();
        if (levelData != null && levelData.mpCost > 0) {
            if (mp < levelData.mpCost) {
                log.warn("Not enough mana. Required: {}, Current: {}", levelData.mpCost, mp);
                return;
            }

            consumeMp(levelData.mpCost);
        }
        dealDamageTo(target, skill);

        // Deal damage to all targets
        for (LivingEntity aoe : targets) {
            if (aoe != null && aoe.isAlive()) {
                dealDamageTo(aoe, skill);
            }
        }

        // Mark skill as used
        skill.onUse();
    }

    public void takeDamage(DamageContext ctx) {
        if (isDead() || ctx == null) return;

        if (getType() == GameObjectType.PLAYER) {
            // Check evasion
            if (StatCalculator.isEvaded(stats)) {
                ctx.setFinalDamage(0);
                onEvade(ctx);
                return;
            }
        }

        if (!ctx.isPenetration())
            // Calculate mitigated damage and store as final damage
            ctx.setFinalDamage(stats.calculateMitigatedDamage(ctx));
        else
            ctx.setFinalDamage(ctx.getDamage());

        ctx.setDefenderHpBeforeHit(hp);
        // Apply final damage
        hp = Math.max(0, hp - ctx.getFinalDamage());

        // Handle reflection
        if (stats.getReflectDamagePercent() > 0 && ctx.getAttacker() != null) {
            int reflected = stats.calculateReflectedDamage(ctx.getFinalDamage());
            ctx.getAttacker().takeReflectDamage(reflected);
            ctx.addEffect(new DamageEffect(5, reflected));
        }

        // Pass context with both raw and final damage to hook
        onDamageTaken(ctx);


        // Check death
        if (isDead()) {
            die(ctx.getAttacker());
        }

    }

    public void dealDamageTo(LivingEntity target) {
        dealDamageTo(target, null);
    }

    public void dealDamageTo(LivingEntity target, SkillEntity skill) {
        if (target == null || target.isDead()) return;
        DamageType damageType = skill != null ? skill.getDamageType() : DamageType.PHYSICAL;
        int baseDamage = StatCalculator.calculateBaseDamage(stats, damageType);

        int totalDmg = baseDamage;

        // Apply skill damage
        if (skill != null) {
            int skillBonus = StatCalculator.calculateSkillDamage(baseDamage, skill);
            totalDmg = baseDamage + skillBonus;

        }

        // Create damage context with raw damage
        DamageContext ctx = new DamageContext(this, target, skill, totalDmg);
        ctx.setFinalDamage(totalDmg);   // Will be updated by target's takeDamage()

        // Normal Damage Effect
        ctx.addEffect(new DamageEffect(0, ctx.getDamage()));

        if (StatCalculator.isPenetrationHit((stats))) {
            ctx.setPenetration(true);
            ctx.addEffect(new DamageEffect(1, totalDmg));
        }

        if (StatCalculator.isCriticalHit(stats)) {
            totalDmg += totalDmg * 2;
            ctx.setDamage(totalDmg);
            // Critical Damage Effect
            ctx.addEffect(new DamageEffect(4, totalDmg));
        }

        // Lifesteal based on final damage dealt
        if (stats.getHpLifesteal() > 0) {
            int healed = stats.calculateLifesteal(ctx.getDamage());
            restoreHp(healed);
            ctx.addEffect(new DamageEffect(2, healed));
        }

        // Manasteal based on final damage dealt
        if (stats.getManaLifesteal() > 0) {
            int restored = stats.calculateManasteal(ctx.getDamage());
            restoreMp(restored);
            ctx.addEffect(new DamageEffect(3, restored));
        }

        lastAttackTime = System.currentTimeMillis();
        // Apply damage to target (target will calculate final damage)
        target.takeDamage(ctx);

        onDamageDealt(ctx);
    }

    protected void takeReflectDamage(int damage) {
        if (isDead()) return;
        hp = Math.max(0, hp - damage);


    }

    // ==================== RESOURCE MANAGEMENT ====================

    public void restoreHp(int amount) {
        if (isDead()) return;
        int actualRestored = Math.min(amount, maxHp - hp);
        hp += actualRestored;
        if (zone != null && actualRestored > 0) {
            zone.broadcast(player -> NetworkService.gI().sendPotionEffect(player, this, 0, amount));
        }

    }

    public void restoreMp(int amount) {
        if (isDead()) return;
        int actualRestored = Math.min(amount, maxMp - mp);
        mp += actualRestored;

        if (zone != null && actualRestored > 0) {
            zone.broadcast(player -> NetworkService.gI().sendPotionEffect(player, this, 1, actualRestored));
        }
    }

    public void consumeMp(int amount) {
        mp = Math.max(0, mp - amount);
    }

    public void applyRegen(long deltaTime) {
        if (isDead()) return;

        regenAccumulator += deltaTime;

        if (regenAccumulator >= REGEN_INTERVAL) {
            int seconds = (int) (regenAccumulator / REGEN_INTERVAL);
            regenAccumulator %= REGEN_INTERVAL;

            // HP Regen: percentage of max HP
            int hpRegenAmount = (maxHp * stats.getHpRegen() / 10000) * seconds;
            if (hpRegenAmount > 0) {
                restoreHp(hpRegenAmount);
            }

            if (type == GameObjectType.MONSTER)
                return;

            // MP Regen: percentage of max MP
            int mpRegenAmount = (maxMp * stats.getManaRegen() / 10000) * seconds;
            if (mpRegenAmount > 0) {
                restoreMp(mpRegenAmount);
            }
        }
    }

    // ==================== STAT CALCULATIONS ====================

    public void updateMaxHp() {
        int baseHp = calculateBaseHp() + stats.getHp();  // Base + flat HP from VIT
        int bonusHp = NumberUtils.increaseBy(baseHp, stats.getHpPercent());
        maxHp = baseHp + bonusHp;

        // Clamp current HP
        if (hp > maxHp) hp = maxHp;
    }

    public void updateMaxMp() {
        int baseMp = calculateBaseMp() + stats.getMp();  // Base + flat MP from VIT+INT
        int bonusMp = NumberUtils.increaseBy(baseMp, stats.getMpPercent());  // MP% from equipment (100 = 1%)
        maxMp = baseMp + bonusMp;

        // Clamp current MP
        if (mp > maxMp) mp = maxMp;
    }

    protected int calculateBaseHp() {
        return 100 + (level * 1500);
    }

    protected int calculateBaseMp() {
        return 50 + (level * 25);
    }

    // ==================== STATUS CHECKS ====================

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public boolean canAttack() {
        long now = System.currentTimeMillis();
        return now >= lastAttackTime + attackCooldown;
    }

    public int getHpPercent() {
        return maxHp > 0 ? (hp * 100) / maxHp : 0;
    }

    public int getMpPercent() {
        return maxMp > 0 ? (mp * 100) / maxMp : 0;
    }

    public int getLevelPercent() {
        return Math.toIntExact((experience * 1000) / calculateRequiredExp(level));
    }

    protected long calculateRequiredExp(int level) {
        // return (long) (1000 * Math.pow(level, 3));
        return expCalculator.calculateRequiredExp(level);
    }


    // ==================== LEVEL & EXP ====================

    public void addExperience(long exp) {
        if (isDead()) return;

        experience += exp;
        long requiredExp = calculateRequiredExp(level);

        while (experience >= requiredExp) {
            experience -= requiredExp;
            levelUp();
            requiredExp = calculateRequiredExp(level);
        }
    }

    protected void levelUp() {
        level++;
        recalculateStats();
        updateMaxHp();
        updateMaxMp();

        // Full heal on level up
        hp = maxHp;
        mp = maxMp;

        onLevelUp();
    }


    // ==================== EVENT HOOKS ====================

    protected void onDamageTaken(DamageContext context) {
        // Override in subclasses
    }

    protected void onDamageDealt(DamageContext context) {
        // Override in subclasses
    }

    protected void onEvade(DamageContext context) {
        // Override in subclasses
    }

    protected void onLevelUp() {
        // Override in subclasses
    }

    public void broadcastMovement() {
        if (zone != null) {
            zone.broadcast(
                    player -> NetworkService.gI().sendMove(player, this)
            );
        }
    }

    public void applyBuff(BuffEffect newBuff) {
        buffManager.applyBuff(newBuff);
    }

    public void applyEffect(Effect newEffect) {
        effectManager.apply(newEffect);
    }

    // ==================== UPDATE ====================

    @Override
    public void onUpdate(long deltaTime) {
        lastUpdateTime = System.currentTimeMillis();
        effectManager.update((int) deltaTime);
        applyRegen(deltaTime);

    }


}