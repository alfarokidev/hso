package game.stat;

import game.buff.BuffManager;
import game.effects.StatModifier;
import game.entity.DamageType;
import game.pet.Pet;
import game.skill.DamageContext;
import game.skill.SkillEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import manager.ConfigManager;
import model.config.Attribute;
import model.config.AttributeConfig;
import model.item.EquipmentItem;
import model.item.Option;
import game.entity.player.PlayerEntity;
import model.item.PetOption;
import model.skill.LvSkill;

import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class Stats {
    private static final int PERCENT_SCALE = 10000;

    // Base Attributes
    private int bonusSTR;
    private int bonusDEX;
    private int bonusVIT;
    private int bonusINT;

    // Basic Damage (Flat)
    private int basicDmg;

    // Elemental Damage (Flat)
    private int physicalDmg;
    private int fireDmg;
    private int iceDmg;
    private int poisonDmg;
    private int lightingDmg;
    private int lightDmg;
    private int darkDmg;

    // Elemental Damage (%)
    private int physicalDmgPercent;
    private int fireDmgPercent;
    private int iceDmgPercent;
    private int poisonDmgPercent;
    private int lightingDmgPercent;
    private int lightDmgPercent;
    private int darkDmgPercent;

    // Defense
    private int defense;
    private int defensePercent;

    // Elemental Resistance %
    private int physicalResPercent;
    private int fireResPercent;
    private int iceResPercent;
    private int poisonResPercent;
    private int lightingResPercent;
    private int lightResPercent;
    private int darkResPercent;

    // HP/MP
    private int hp;           // Flat HP bonus from VIT
    private int hpPercent;    // HP% bonus from equipment
    private int mp;           // Flat MP bonus from VIT + INT
    private int mpPercent;    // MP% bonus from equipment
    private int hpRegen;
    private int manaRegen;
    private int hpLifesteal;
    private int manaLifesteal;

    // Combat Stats
    private int criticalRatePercent;
    private int criticalDamage;
    private int evadePercent;
    private int reflectDamagePercent;
    private int penetrationPercent;


    // Skills
    private int attackSkill;
    private int buffSkill;

    public Stats() {
        reset();
    }

    public void reset() {
        bonusSTR = bonusDEX = bonusVIT = bonusINT = 0;
        basicDmg = 0;

        physicalDmg = fireDmg = iceDmg = poisonDmg = 0;
        lightingDmg = lightDmg = darkDmg = 0;

        physicalDmgPercent = fireDmgPercent = iceDmgPercent = poisonDmgPercent = 0;
        lightingDmgPercent = lightDmgPercent = darkDmgPercent = 0;

        defense = defensePercent = 0;

        physicalResPercent = fireResPercent = iceResPercent = poisonResPercent = 0;
        lightingResPercent = lightResPercent = darkResPercent = 0;

        hp = mp = 0;  // FIXED: Added hp and mp reset
        hpPercent = mpPercent = hpRegen = manaRegen = 0;
        hpLifesteal = manaLifesteal = 0;

        criticalRatePercent = criticalDamage = evadePercent = reflectDamagePercent = penetrationPercent = 0;
        attackSkill = buffSkill = 0;
    }

    public void calculate(PlayerEntity player) {
        reset();


        applyBaseAttr(player);

        // Passive Skill Bonus
        applySkillStats(player.getSkillData());

        // Equipment Bonus
        List<EquipmentItem> equips = player.getInventoryManager().getWearing().allEquipped();
        for (EquipmentItem item : equips) {
            if (item == null) continue;
            applyItemStats(item);
        }

        // Buff
        applyBuffStats(player);

        // Pet

        Pet pet;
        if ((pet = player.getPet()) != null) {
            applyPetAttributes(pet);
        }
    }

    public void applyBaseAttr(PlayerEntity player) {
        AttributeConfig config = ConfigManager.getInstance().getAttributeConfig(player.getRole());

        // APPLY STR POINT
        if (!config.getStrength().isEmpty()) {
            for (Attribute attr : config.getStrength()) {
                StatType type = StatType.fromValue(attr.getId());
                if (type == null) continue;

                int value = type.isPercent() ? attr.getIntValue() : (int) Math.round(attr.getValue());
                addStat(type, player.getSTR() * value);

            }
        }

        // APPLY DEX POINT
        if (!config.getDexterity().isEmpty()) {
            for (Attribute attr : config.getDexterity()) {
                StatType type = StatType.fromValue(attr.getId());
                if (type == null) continue;

                int value = type.isPercent() ? attr.getIntValue() : (int) Math.round(attr.getValue());
                addStat(type, player.getDEX() * value);

            }
        }

        // APPLY VIT POINT
        if (!config.getVitality().isEmpty()) {
            for (Attribute attr : config.getVitality()) {
                StatType type = StatType.fromValue(attr.getId());
                if (type == null) continue;

                int value = type.isPercent() ? attr.getIntValue() : (int) Math.round(attr.getValue());
                addStat(type, player.getVIT() * value);

            }
        }

        // APPLY INT POINT
        if (!config.getStrength().isEmpty()) {
            for (Attribute attr : config.getIntelligence()) {
                StatType type = StatType.fromValue(attr.getId());
                if (type == null) continue;

                int value = type.isPercent() ? attr.getIntValue() : (int) Math.round(attr.getValue());
                addStat(type, player.getINT() * value);

            }
        }

    }

    public void applyBaseAttributes(int STR, int DEX, int VIT, int INT, int role) {


        // STR → Critical Rate & All Damage%
        criticalRatePercent += Math.round(STR * 10f); // 0.2% = 20


        // DEX → Evade%, Defense%, Defense
        evadePercent += DEX * 20; // 0.2% = 20
        defensePercent += DEX * 10; // 0.1% = 10
        defense += DEX * 20;


        // VIT → HP, MP, Reflect Damage%
        hp += VIT * 310;
        reflectDamagePercent += VIT * 20; // 0.02%

        mp += INT * 11;
        penetrationPercent += INT * 10; // 0.1%

        // Role-specific bonus (ADDITIONAL to base INT bonus)
        switch (role) {
            case 0 -> {  // WARRIOR (FIRE)
                fireDmg += STR * 4;
                fireDmgPercent += STR * 18;
                physicalDmg += STR * 4;
                physicalDmgPercent += STR * 18;
                basicDmg += STR * 4;
            }
            case 1 -> {  // ASSASSIN (POISON)
                poisonDmg += STR * 4;
                poisonDmgPercent += STR * 18;
                physicalDmg += STR * 4;
                physicalDmgPercent += STR * 18;
                basicDmg += STR * 4;
            }
            case 2 -> {  // MAGE ICE
                iceDmg += INT * 4;
                iceDmgPercent += INT * 18;
                physicalDmg += INT * 4;
                physicalDmgPercent += INT * 18;

                iceDmg += STR * 4;
                iceDmgPercent += STR * 18;
                physicalDmg += STR * 4;
                physicalDmgPercent += STR * 18;

                basicDmg += INT * 4;
            }
            case 3 -> {  // GUNNER (LIGHTING)
                lightingDmg += INT * 4;
                lightingDmgPercent += INT * 18;
                physicalDmg += INT * 4;
                physicalDmgPercent += INT * 18;

                iceDmg += STR * 4;
                iceDmgPercent += STR * 18;
                physicalDmg += STR * 4;
                physicalDmgPercent += STR * 18;

                basicDmg += INT * 4;

            }
        }
    }

    private void applySkillStats(Map<Byte, SkillEntity> skillData) {
        List<SkillEntity> learnedPassiveSkills = skillData.values()
                .stream()
                .filter(skill -> skill.getCurrentLevel() > 0 && skill.getType() == 2)
                .toList();

        for (SkillEntity skillEntity : learnedPassiveSkills) {
            LvSkill lvSkill = skillEntity.getCurrentLevelData();
            for (Option op : lvSkill.options) {
                if (op == null) continue;

                StatType statType = StatType.fromValue(op.getId());
                if (statType == null) continue;

                addStat(statType, op.getValue());
            }
        }
    }

    private void applyItemStats(EquipmentItem item) {
        for (Option op : item.getOption()) {
            StatType type = StatType.fromValue(op.getId());
            if (type == null) continue;

            int value = StatCalculator.getBonusPlus(op, item.getPlus());
            addStat(type, value);
        }
    }

    private void applyBuffStats(PlayerEntity player) {
        if (player == null) return;

        for (StatType statType : StatType.values()) {
            int bonus = player.getBuffManager().getStatBonus(statType);
            if (bonus != 0) {
                addStat(statType, bonus);
            }
        }

        List<StatModifier> buffEffects = player.getEffectManager().getActiveModifiers();
        if (!buffEffects.isEmpty()) {
            for (StatModifier mod : buffEffects) {
                addStat(mod.getType(), mod.getValue());
            }
        }
    }

    private void applyPetAttributes(Pet pet) {
        if (pet == null) return;

        addStat(StatType.STR, (pet.getStrength() / 78));
        addStat(StatType.DEX, (pet.getDexterity() / 78));
        addStat(StatType.VIT, (pet.getVitality() / 78));
        addStat(StatType.INT, (pet.getIntelligence() / 78));

        for (PetOption op : pet.getOptions()) {
            StatType statType = StatType.fromValue(op.getId());
            if (statType == null) continue;

            addStat(statType, op.getValue());
        }

    }

    public void addStat(StatType type, int value) {
        switch (type) {
            case STR -> bonusSTR += value;
            case DEX -> bonusDEX += value;
            case VIT -> bonusVIT += value;
            case INT -> bonusINT += value;

            case BASIC_DAMAGE -> basicDmg += value;

            case PHYSICAL_DAMAGE -> physicalDmg += value;
            case FIRE_DAMAGE -> fireDmg += value;
            case ICE_DAMAGE -> iceDmg += value;
            case POISON_DAMAGE -> poisonDmg += value;
            case LIGHTING_DAMAGE -> lightingDmg += value;
            case LIGHT_DAMAGE -> lightDmg += value;
            case DARK_DAMAGE -> darkDmg += value;

            case PHYSICAL_DAMAGE_PERCENT -> physicalDmgPercent += value;
            case FIRE_DAMAGE_PERCENT -> fireDmgPercent += value;
            case ICE_DAMAGE_PERCENT -> iceDmgPercent += value;
            case POISON_DAMAGE_PERCENT -> poisonDmgPercent += value;
            case LIGHTING_DAMAGE_PERCENT -> lightingDmgPercent += value;
            case LIGHT_DAMAGE_PERCENT -> lightDmgPercent += value;
            case DARK_DAMAGE_PERCENT -> darkDmgPercent += value;

            case DEFENSE -> defense += value;
            case DEFENSE_PERCENT -> defensePercent += value;

            case PHYSICAL_RES -> physicalResPercent += value;
            case FIRE_RES -> fireResPercent += value;
            case ICE_RES -> iceResPercent += value;
            case POISON_RES -> poisonResPercent += value;
            case LIGHTING_RES -> lightingResPercent += value;
            case LIGHT_RES -> lightResPercent += value;
            case DARK_RES -> darkResPercent += value;

            case HP -> hp += value;
            case MP -> mp += value;
            case HP_PERCENT -> hpPercent += value;
            case MP_PERCENT -> mpPercent += value;
            case HP_REGEN -> hpRegen += value;
            case MANA_REGEN -> manaRegen += value;
            case LIFE_STEAL -> hpLifesteal += value;
            case MANA_STEAL -> manaLifesteal += value;

            case CRITICAL_RATE -> criticalRatePercent += value;
            case CRITICAL_DAMAGE -> criticalDamage += value;
            case EVADE -> evadePercent += value;
            case REFLECT_DAMAGE -> reflectDamagePercent += value;
            case PEN -> penetrationPercent += value;

            case ATTACK_SKILL -> attackSkill += value;
            case BUFF_SKILL -> buffSkill += value;
        }
    }

    public int get(StatType type) {
        return switch (type) {
            case STR -> bonusSTR;
            case DEX -> bonusDEX;
            case VIT -> bonusVIT;
            case INT -> bonusINT;

            case BASIC_DAMAGE -> basicDmg;

            case PHYSICAL_DAMAGE,
                 FIRE_DAMAGE,
                 ICE_DAMAGE,
                 POISON_DAMAGE,
                 LIGHTING_DAMAGE,
                 LIGHT_DAMAGE,
                 DARK_DAMAGE,
                 DEFENSE -> getTotal(type);

            case PHYSICAL_DAMAGE_PERCENT -> physicalDmgPercent;
            case FIRE_DAMAGE_PERCENT -> fireDmgPercent;
            case ICE_DAMAGE_PERCENT -> iceDmgPercent;
            case POISON_DAMAGE_PERCENT -> poisonDmgPercent;
            case LIGHTING_DAMAGE_PERCENT -> lightingDmgPercent;
            case LIGHT_DAMAGE_PERCENT -> lightDmgPercent;
            case DARK_DAMAGE_PERCENT -> darkDmgPercent;

            case DEFENSE_PERCENT -> defensePercent;

            case PHYSICAL_RES -> physicalResPercent;
            case FIRE_RES -> fireResPercent;
            case ICE_RES -> iceResPercent;
            case POISON_RES -> poisonResPercent;
            case LIGHTING_RES -> lightingResPercent;
            case LIGHT_RES -> lightResPercent;
            case DARK_RES -> darkResPercent;

            case HP -> hp;
            case MP -> mp;
            case HP_PERCENT -> hpPercent;
            case MP_PERCENT -> mpPercent;
            case HP_REGEN -> hpRegen;
            case MANA_REGEN -> manaRegen;
            case LIFE_STEAL -> hpLifesteal;
            case MANA_STEAL -> manaLifesteal;

            case CRITICAL_RATE -> criticalRatePercent;
            case CRITICAL_DAMAGE -> criticalDamage;
            case EVADE -> evadePercent;
            case REFLECT_DAMAGE -> reflectDamagePercent;
            case PEN -> penetrationPercent;

            case ATTACK_SKILL -> attackSkill;
            case BUFF_SKILL -> buffSkill;

        };
    }

    public int get(int statId) {
        StatType type = StatType.fromValue(statId);
        return type != null ? get(type) : 0;
    }

    public int getTotal(StatType type) {
        return switch (type) {
            case PHYSICAL_DAMAGE -> Math.max(0, physicalDmg + (physicalDmg * physicalDmgPercent / PERCENT_SCALE));
            case ICE_DAMAGE -> Math.max(0, iceDmg + (iceDmg * iceDmgPercent / PERCENT_SCALE));
            case FIRE_DAMAGE -> Math.max(0, fireDmg + (fireDmg * fireDmgPercent / PERCENT_SCALE));
            case LIGHTING_DAMAGE -> Math.max(0, lightingDmg + (lightingDmg * lightingDmgPercent / PERCENT_SCALE));
            case POISON_DAMAGE -> Math.max(0, poisonDmg + (poisonDmg * poisonDmgPercent / PERCENT_SCALE));
            case LIGHT_DAMAGE -> Math.max(0, lightDmg + (lightDmg * lightDmgPercent / PERCENT_SCALE));
            case DARK_DAMAGE -> Math.max(0, darkDmg + (darkDmg * darkDmgPercent / PERCENT_SCALE));
            case DEFENSE -> Math.max(0, defense + (defense * defensePercent / PERCENT_SCALE));
            case PHYSICAL_RES -> physicalResPercent;
            case FIRE_RES -> fireResPercent;
            case ICE_RES -> iceResPercent;
            case POISON_RES -> poisonResPercent;
            case LIGHTING_RES -> lightingResPercent;
            case LIGHT_RES -> lightResPercent;
            case DARK_RES -> darkResPercent;
            default -> 0;
        };

    }

    public int calculateMitigatedDamage(DamageContext context) {
        int baseDamage = context.getDamage();
        DamageType type = context.getDamageType();

        // 1️⃣ Get resistance for damage type
        int resistance = switch (type) {
            case PHYSICAL -> getTotal(StatType.PHYSICAL_RES);
            case FIRE -> getTotal(StatType.FIRE_RES);
            case ICE -> getTotal(StatType.ICE_RES);
            case POISON -> getTotal(StatType.POISON_RES);
            case LIGHTING -> getTotal(StatType.LIGHTING_RES);
            case LIGHT -> getTotal(StatType.LIGHT_RES);
            case DARK -> getTotal(StatType.DARK_RES);
        };

        //log.info("Step 1 - Base Damage: {}, Resistance({}): {}%", baseDamage, type, resistance / 100.0);

        // 2️⃣ Apply penetration
        resistance = Math.max(0, resistance - penetrationPercent);
        // log.info("Step 2 - Resistance after Penetration({}%): {}", penetrationPercent, resistance / 100.0);

        // 3️⃣ Diminishing returns on resistance
        int effectiveRes = (int) ((long) resistance * PERCENT_SCALE / (resistance + PERCENT_SCALE));
        long dmgAfterRes = (long) baseDamage * (PERCENT_SCALE - effectiveRes) / PERCENT_SCALE;
        // log.info("Step 3 - Effective Resistance: {}, Damage after Res: {}", effectiveRes, dmgAfterRes);

        // 4️⃣ Apply flat defense (capped at 20% of base damage)
        long maxFlatBlock = (long) baseDamage * 2000 / PERCENT_SCALE; // 20% cap
        long effectiveDef = Math.min(getTotal(StatType.DEFENSE), maxFlatBlock);
        long finalDamage = getFinalDamage(dmgAfterRes, effectiveDef, baseDamage);
        //  log.info("Step 6 - Min Damage: {}, Final Damage: {}", minDamage, finalDamage);

        return (int) finalDamage;
    }

    private long getFinalDamage(long dmgAfterRes, long effectiveDef, long baseDamage) {
        long dmgAfterDef = Math.max(0, dmgAfterRes - effectiveDef);
        // log.info("Step 4 - FlatDef: {}, MaxFlatBlock: {}, Damage after Flat: {}", effectiveDef, maxFlatBlock, dmgAfterDef);


        int effectiveDefPct = (int) ((long) defensePercent * PERCENT_SCALE / (defensePercent + PERCENT_SCALE));
        long dmgAfterDefPct = dmgAfterDef * (PERCENT_SCALE - effectiveDefPct) / PERCENT_SCALE;
        //  log.info("Step 5 - Defense%: {}, EffectiveDef%: {}, Damage after Def%: {}", defensePercent, effectiveDefPct, dmgAfterDefPct);


        long minDamage = Math.max(1, baseDamage * 800 / PERCENT_SCALE);
        return Math.max(dmgAfterDefPct, minDamage);
    }

    public int calculateReflectedDamage(int incomingDmg) {
        return (int) ((long) incomingDmg * reflectDamagePercent / PERCENT_SCALE);
    }

    public int calculateLifesteal(int damageDealt) {
        long steal = (long) damageDealt * hpLifesteal / PERCENT_SCALE;
        return (int) Math.max(1, steal);
    }

    public int calculateManasteal(int damageDealt) {
        long steal = (long) damageDealt * manaLifesteal / PERCENT_SCALE;
        return (int) Math.max(1, steal);
    }

}