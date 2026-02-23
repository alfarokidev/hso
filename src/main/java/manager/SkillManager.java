package manager;

import lombok.extern.slf4j.Slf4j;
import model.skill.LvSkill;
import model.skill.Skill;
import game.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SkillManager {
    private SkillManager() {
    }

    private final Map<Integer, Map<Byte, Skill>> skills = new HashMap<>();

    private static class Holder {
        private static final SkillManager INSTANCE = new SkillManager();
    }

    public static SkillManager getInstance() {
        return Holder.INSTANCE;
    }
    public void addSkill(int role, Map<Byte, Skill> roleSkill) {
        skills.putIfAbsent(role, roleSkill);
    }

    public Map<Byte, Skill> getRoleSkill(int role) {
        return skills.get(role);
    }


    public Skill getSkill(int role, byte skillId) {
        Map<Byte, Skill> skillData = getRoleSkill(role);
        return skillData.get(skillId);
    }

    public void clear() {
        skills.clear();
    }

}


