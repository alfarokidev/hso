package game.skill;

import game.buff.BuffEffect;
import game.entity.DamageType;
import game.stat.StatType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import manager.SkillManager;
import model.item.Option;
import model.skill.LvSkill;
import model.skill.Skill;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class SkillEntity {
    public static final Set<Byte> PHYSICAL_SKILLS = new HashSet<>(Set.of((byte) 0, (byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 19));
    private final byte skillId;
    private byte currentLevel;
    private long lastUsedTime;
    private final int role;
    private Skill skillData;


    public SkillEntity(byte skillId, byte currentLevel, byte role) {
        this.skillId = skillId;
        this.currentLevel = currentLevel;
        this.lastUsedTime = 0;
        this.role = role;
    }

    public LvSkill getCurrentLevelData() {
        Skill skill = getSkillData();
        return skill != null ? skill.getLevel((byte) (currentLevel - 1)) : null;
    }

    public Skill getSkillData() {
        if (skillData == null) {
            skillData = SkillManager.getInstance().getSkill(role, skillId);
        }
        return skillData;
    }

    /**
     * Upgrade skill to next level
     */
    public boolean upgrade(short playerLevel, int value) {
        Skill skill = getSkillData();
        if (skill == null) return false;

        int maxLevel = skill.levels.size();
        int targetLevel = Math.min(currentLevel + value, maxLevel);

        byte newLevel = currentLevel;

        // Validate level-by-level
        for (byte lv = (byte) (currentLevel + 1); lv <= targetLevel; lv++) {
            if (!skill.canLearn(lv, playerLevel)) {
                break; // stop at first invalid level
            }
            newLevel = lv;
        }

        if (newLevel == currentLevel) {
            return false; // nothing upgraded
        }

        currentLevel = newLevel;
        return true;
    }

    public boolean canUpgrade(short playerLevel, int value) {
        Skill skill = getSkillData();
        if (skill == null) return false;

        int maxLevel = skill.levels.size();
        int targetLevel = Math.min(currentLevel + value, maxLevel);


        // Validate level-by-level
        for (byte lv = (byte) (currentLevel + 1); lv <= targetLevel; lv++) {
            if (!skill.canLearn(lv, playerLevel)) {
                return false; // stop at first invalid level
            }

        }

        return true; // nothing upgraded
    }

    public boolean isOnCooldown() {
        LvSkill levelData = getCurrentLevelData();
        if (levelData == null) {
            log.warn("Cannot check cooldown: skill level data not found for skillId={}, level={}", skillId, currentLevel);
            return true; // Prevent usage if data is missing
        }

        // If never used, skill is ready
        if (lastUsedTime == 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUsedTime;

        // Reduce cooldown by 1 second (1000ms) to sync with client
        long adjustedCooldown = Math.max(0, levelData.cooldown - 1000);

        // Check if elapsed time is less than adjusted cooldown duration
        return elapsedTime < adjustedCooldown;
    }

    /**
     * Get remaining cooldown in milliseconds
     * Adjusted to match client-side cooldown calculation
     *
     * @return remaining cooldown time in ms, or 0 if ready to use
     */

    public long getRemainingCooldown() {
        if (!isOnCooldown()) {
            return 0;
        }

        LvSkill levelData = getCurrentLevelData();
        if (levelData == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUsedTime;

        // Reduce cooldown by 1 second (1000ms) to sync with client
        long adjustedCooldown = Math.max(0, levelData.cooldown - 1000);
        long remainingTime = adjustedCooldown - elapsedTime;

        // Return remaining time (guaranteed > 0 because isOnCooldown is true)
        return Math.max(0, remainingTime);
    }


    /**
     * Mark skill as used, starts cooldown
     */
    public void onUse() {
        this.lastUsedTime = System.currentTimeMillis();
    }

    /**
     * Reset cooldown (for special cases)
     */
    public void resetCooldown() {
        this.lastUsedTime = 0;
    }

    public boolean isMaxLevel() {
        Skill s = getSkillData();
        if (s == null) return false;

        return currentLevel >= s.levels.size();
    }

    public byte getType() {
        Skill skill = getSkillData();
        if (skill == null) return -1;

        return skill.type;
    }

    public short getTargetCount() {
        LvSkill lvSkill = getCurrentLevelData();
        if (lvSkill == null) return 0;

        return lvSkill.targetCount;
    }

    public boolean isPhysicalSkill(byte skillId) {
        return PHYSICAL_SKILLS.contains(skillId);
    }

    public boolean isBuffSkill() {
        Skill skill = getSkillData();
        if (skill == null) return false;

        // type 1 = attack with buff, type 3 = pure buff
        return skill.type == 1 || skill.type == 2;
    }

    public BuffEffect createBuff() {
        LvSkill lvData = getCurrentLevelData();
        Skill skill = getSkillData();

        if (lvData == null || skill == null) {
            return null;
        }

        // Only create buff if skill has buff duration
        if (lvData.buffDuration <= 0) {
            return null;
        }

        BuffEffect buff = new BuffEffect(
                skillId,
                skill.iconId,
                skill.buffType,
                lvData.buffDuration
        );

        // Add stat modifiers from skill options
        if (lvData.options != null) {
            for (Option option : lvData.options) {
                if (option == null) continue;

                StatType statType = StatType.fromValue(option.getId());
                if (statType != null) {
                    buff.addStatModifier(statType, option.getValue());
                }
            }
        }

        return buff;
    }

    public DamageType getDamageType() {
        if (isPhysicalSkill(skillId)) {
            return DamageType.PHYSICAL;
        } else {
            return switch (role) {
                case 0 -> DamageType.FIRE;
                case 1 -> DamageType.POISON;
                case 2 -> DamageType.ICE;
                case 3 -> DamageType.LIGHTING;
                default -> throw new IllegalStateException("Unexpected value: " + role);
            };
        }
    }

}