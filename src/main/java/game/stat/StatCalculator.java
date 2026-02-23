package game.stat;

import game.entity.DamageType;
import game.skill.SkillEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import manager.ItemManager;
import model.item.ItemOption;
import model.item.Option;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
@Slf4j
public class StatCalculator {

    private static final int PERCENT_SCALE = 10000;

    public static int calculateSkillDamage(int baseDamage, SkillEntity skill) {
        int flatDmg = 0;
        int percentBonus = 0;

        for (Option op : skill.getCurrentLevelData().options) {
            if (op == null) continue;

            StatType type = StatType.fromValue(op.getId());
            if (type == null) continue;

            switch (type) {
                case PHYSICAL_DAMAGE,
                     FIRE_DAMAGE,
                     ICE_DAMAGE,
                     POISON_DAMAGE,
                     LIGHTING_DAMAGE,
                     LIGHT_DAMAGE,
                     DARK_DAMAGE -> flatDmg += op.getValue();
                case PHYSICAL_DAMAGE_PERCENT,
                     ICE_DAMAGE_PERCENT,
                     LIGHTING_DAMAGE_PERCENT,
                     FIRE_DAMAGE_PERCENT,
                     POISON_DAMAGE_PERCENT,
                     LIGHT_DAMAGE_PERCENT,
                     DARK_DAMAGE_PERCENT -> percentBonus += op.getValue();
            }
        }


        float percentRate = percentBonus / (float) PERCENT_SCALE;
        int skillDamage = Math.round(baseDamage * (1 + percentRate)) + flatDmg;
        return Math.max(0, skillDamage);

    }

    public static int calculateBaseDamage(Stats stats, DamageType damageType) {
        int flatDmg = stats.getBasicDmg();
        int percentBonus = 0;

        // Add elemental damage and percentage bonus
        switch (damageType) {
            case PHYSICAL -> {
                flatDmg += stats.getPhysicalDmg();
                percentBonus = stats.getPhysicalDmgPercent();
            }
            case FIRE -> {
                flatDmg += stats.getFireDmg();
                percentBonus = stats.getFireDmgPercent();
            }
            case ICE -> {
                flatDmg += stats.getIceDmg();
                percentBonus = stats.getIceDmgPercent();
            }
            case POISON -> {
                flatDmg += stats.getPoisonDmg();
                percentBonus = stats.getPoisonDmgPercent();
            }
            case LIGHTING -> {
                flatDmg += stats.getLightingDmg();
                percentBonus = stats.getLightingDmgPercent();
            }
            case LIGHT -> {
                flatDmg += stats.getLightDmg();
                percentBonus = stats.getLightDmgPercent();
            }
            case DARK -> {
                flatDmg += stats.getDarkDmg();
                percentBonus = stats.getDarkDmgPercent();
            }
        }

        // Apply percentage bonus
        long totalDmg = flatDmg + ((long) flatDmg * percentBonus / PERCENT_SCALE);

        return Math.toIntExact(Math.max(0, totalDmg));
    }

    public static int getBonusPlus(Option op, int plus) {
        if (plus <= 0) return op.getValue();


        ItemOption option = ItemManager.getInstance().getOption(op.getId());
        if (option == null) return op.getValue();


        int value = option.isPercent() ? option.getUpgradeBonusInt() : (int) Math.round(option.getUpgradeBonus());
        if (value <= 0) return op.getValue();


        StatType type = StatType.fromValue(op.getId());
        if (type == null) return op.getValue();

        return op.getValue() + (plus * value);

    }

    public static boolean isCriticalHit(Stats stats) {
        int chance = Math.max(0, Math.min(PERCENT_SCALE, stats.getCriticalRatePercent()));
        return ThreadLocalRandom.current().nextInt(PERCENT_SCALE) < chance;
    }

    public static boolean isPenetrationHit(Stats stats) {
        int chance = Math.max(0, Math.min(PERCENT_SCALE, stats.getPenetrationPercent()));
        return ThreadLocalRandom.current().nextInt(PERCENT_SCALE) < chance;
    }

    public static boolean isEvaded(Stats stats) {
        int chance = Math.max(0, Math.min(PERCENT_SCALE, stats.getEvadePercent()));
        return ThreadLocalRandom.current().nextInt(PERCENT_SCALE) < chance;
    }


}
