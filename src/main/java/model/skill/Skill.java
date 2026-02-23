package model.skill;

import java.util.List;

public class Skill {
    public byte sid;
    public int role;
    public byte iconId;
    public String name;
    public byte type;
    public short attackRange;
    public String description;
    public byte buffType;
    public byte subEffectType;
    public List<LvSkill> levels;

    public short performDuration;
    public byte paintType;


    public LvSkill getLevel(byte level) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }

        int index = level - 1;

        // clamp level to max available
        if (index < 0) index = 0;
        if (index >= levels.size()) index = levels.size() - 1;

        return levels.get(index);
    }

    /**
     * Check if player can learn this skill level
     */
    public boolean canLearn(byte level, short playerLevel) {
        int targetLv = level-1;
        if (targetLv < 0 || targetLv >= levels.size()) return false;

        LvSkill skillLevel = levels.get(targetLv);
        return skillLevel != null && playerLevel >= skillLevel.requiredLevel;
    }
}
