package model.player;

import game.entity.player.PlayerEntity;
import game.skill.SkillEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PlayerMapper {
    // TO GAME RUNTIME
    public static PlayerEntity toEntity(Player player) {
        PlayerEntity entity = new PlayerEntity();

        entity.setId(player.getId());
        entity.setUid(player.getUid());
        entity.setName(player.getName());
        entity.setRole(player.getRole());
        entity.setBody(player.getBody());
        entity.setLevel(player.getLevel());
        entity.setExperience(player.getExperience());
        entity.setGold(player.getGold());
        entity.setGems(player.getGems());
        entity.setSTR(player.getStrength());
        entity.setDEX(player.getDexterity());
        entity.setVIT(player.getVitality());
        entity.setINT(player.getIntelligence());
        entity.setActivePoint(player.getActivePoint());
        entity.setArenaPoint(player.getArenaPoint());
        entity.setPotentialPoint(player.getPotentialPoint());
        entity.setSkillPoint(player.getSkillPoint());
        entity.setRms(player.getRms());
        entity.setSkills(player.getSkills());
        entity.setOnline(player.isOnline());
        entity.setLocation(player.getLocation(), null, null);
        if (player.getPartSettings() != null) {
            entity.setPartSettings(player.getPartSettings());
        }
        return entity;
    }

    // TO DATABASE MODEL
    public static Player toModel(PlayerEntity entity) {
        Player p = new Player();
        p.setId(entity.getId());
        p.setUid(entity.getUid());
        p.setName(entity.getName());
        p.setRole(entity.getRole());
        p.setBody(entity.getBody());
        p.setLevel(entity.getLevel());
        p.setExperience(entity.getExperience());
        p.setGold(entity.getGold());
        p.setGems(entity.getGems());
        p.setStrength(entity.getSTR());
        p.setDexterity(entity.getDEX());
        p.setVitality(entity.getVIT());
        p.setIntelligence(entity.getINT());
        p.setActivePoint(entity.getActivePoint());
        p.setArenaPoint(entity.getArenaPoint());
        p.setPotentialPoint(entity.getPotentialPoint());
        p.setSkillPoint(entity.getSkillPoint());
        p.setRms(entity.getRms());
        p.setOnline(entity.isOnline());

        // Store Skill Level
        byte[] skills = new byte[21];
        for (byte i = 0; i < entity.getSkillData().size(); i++) {
            SkillEntity sk = entity.getSkillData().get(i);
            skills[i] = sk.getCurrentLevel();
        }
        p.setSkills(skills);

        // Store Last Location
        p.setLocation(entity.getPosition());
        p.setPartSettings(entity.getPartSettings());
        return p;
    }

    public static PlayerEntity clonePlayer(PlayerEntity player) {
        PlayerEntity entity = new PlayerEntity();

        entity.setId(player.getId());
        entity.setUid(player.getUid());
        entity.setName(player.getName());
        entity.setRole(player.getRole());
        byte[] body = player.getBody().clone();
        body[2] = 33;
        entity.setBody(body);

        entity.setLevel(player.getLevel());
        entity.setExperience(player.getExperience());
        entity.setGold(player.getGold());
        entity.setGems(player.getGems());
        entity.setSTR(player.getSTR());
        entity.setDEX(player.getDEX());
        entity.setVIT(player.getVIT());
        entity.setINT(player.getINT());
        entity.setActivePoint(player.getActivePoint());
        entity.setArenaPoint(player.getArenaPoint());
        entity.setPotentialPoint(player.getPotentialPoint());
        entity.setSkillPoint(player.getSkillPoint());
        entity.setRms(player.getRms());
        entity.setSkills(player.getSkills());
        entity.setOnline(player.isOnline());
        entity.setLocation(player.getPosition(), null, null);
        return entity;
    }
}
