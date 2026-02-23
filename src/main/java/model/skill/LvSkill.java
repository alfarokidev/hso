package model.skill;

import model.item.Option;

public class LvSkill {
    public short mpCost;
    public short requiredLevel;
    public int cooldown;
    public int buffDuration;
    public byte subEffectPercent;
    public short subEffectDuration;
    public short bonusHp;
    public short bonusMp;
    public Option[] options;
    public byte targetCount;
    public short castRange;
}
