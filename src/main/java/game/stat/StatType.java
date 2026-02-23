package game.stat;

import lombok.Getter;

@Getter
public enum StatType {

    BASIC_DAMAGE(40, false),
    PHYSICAL_DAMAGE(0, false),
    ICE_DAMAGE(1, false),
    FIRE_DAMAGE(2, false),
    LIGHTING_DAMAGE(3, false),
    POISON_DAMAGE(4, false),

    LIGHT_DAMAGE(5, false),
    DARK_DAMAGE(6, false),

    PHYSICAL_DAMAGE_PERCENT(7, true),
    ICE_DAMAGE_PERCENT(8, true),
    FIRE_DAMAGE_PERCENT(9, true),
    LIGHTING_DAMAGE_PERCENT(10, true),
    POISON_DAMAGE_PERCENT(11, true),
    LIGHT_DAMAGE_PERCENT(12, true),
    DARK_DAMAGE_PERCENT(13, true),

    DEFENSE(14, false),
    DEFENSE_PERCENT(15, true),

    PHYSICAL_RES(16, true),
    FIRE_RES(17, true),
    ICE_RES(18, true),
    POISON_RES(19, true),
    LIGHTING_RES(20, true),
    LIGHT_RES(21, true),
    DARK_RES(22, true),

    STR(23, false),
    DEX(24, false),
    VIT(25, false),
    INT(26, false),

    HP(231, false),
    MP(232, false),
    HP_PERCENT(27, true),
    MP_PERCENT(28, true),
    HP_REGEN(29, true),
    MANA_REGEN(30, true),
    LIFE_STEAL(31, true),
    MANA_STEAL(32, true),

    CRITICAL_RATE(33, true),
    CRITICAL_DAMAGE(107, true),
    EVADE(34, true),
    REFLECT_DAMAGE(35, true),
    PEN(36, true),
    ATTACK_SKILL(37, false),
    BUFF_SKILL(38, false);

    private final int value;
    private final boolean percent;

    StatType(int value, boolean percent) {
        this.value = value;
        this.percent = percent;
    }

    public static StatType fromValue(int typeId) {
        for (StatType type : values()) {
            if (type.value == typeId) return type;
        }
        return null;
    }

}
