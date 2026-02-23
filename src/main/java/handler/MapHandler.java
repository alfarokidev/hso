package handler;

import game.entity.Position;
import game.entity.base.LivingEntity;
import game.entity.monster.MonsterEntity;
import game.entity.player.PlayerEntity;
import game.guild.Guild;
import game.guild.GuildManager;
import game.map.DropItem;
import game.skill.SkillEntity;
import manager.MenuManager;
import manager.WorldManager;
import model.map.Vgo;
import model.npc.Go;
import model.skill.LvSkill;
import network.Message;
import network.Session;
import service.NetworkService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static handler.Command.*;
import static model.npc.Go.*;

public class MapHandler {

    public static void onMessage(Session s, Message m) throws IOException {
        switch (m.command) {
            case GET_ITEM_MAP -> onGetItemMap(s, m);
            case FIRE_PK -> onFirePK(s, m);
            case FIRE_MONSTER -> onFireMonster(s, m);
            case BUFF -> onUseBuff(s, m);
            case MONSTER_INFO -> onMonsterInfo(s, m);
            case CHAR_INFO -> onCharInfo(s, m);
            case NPC_INFO -> NpcHandler.handle(s, m);
            case OBJECT_MOVE -> onMove(s, m);
            case GO_HOME -> onGoHome(s, m);
            case OTHER_PLAYER_INFO -> onOtherPlayerInfo(s, m);
            case PK -> onChangeFlag(s, m);
        }
    }

    private static void onChangeFlag(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        if (p.getMap() != null && p.getMap().isBattleMap()) {
            NetworkService.gI().sendToast(p, "You can’t do that");
            return;
        }

        p.setTypePK(m.in().readByte());
        p.getZone().broadcast(notify -> NetworkService.gI().sendChangeFlag(notify, p));
    }

    private static void onOtherPlayerInfo(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        String name = m.in().readUTF();
        short type = m.in().readByte();
        if (type == 0) {
            PlayerEntity target = p.getZone().findPlayerByName(name);
            if (target == null) {
                NetworkService.gI().sendNoticeBox(s, "Pemain ini sedang offline");
                return;
            }

            NetworkService.gI().sendOtherPlayerInfo(p, target);
        }
    }

    private static void onCharInfo(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        short playerId = m.in().readShort();

        PlayerEntity target = p.getZone().findPlayerById(playerId);
        if (target != null) {
            NetworkService.gI().sendCharInfo(p, target);
        }
    }

    private static void onGoHome(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        if (p.getMap().isBattleMap()) {
            NetworkService.gI().sendToast(p, "Now allowed");
            return;
        }

        byte type = m.in().readByte();

        if (type == 1) {
            if (!p.spendGem(-50)) {
                NetworkService.gI().sendNoticeBox(s, "Tidak cukup gems");
                return;
            }
            p.respawn();
            p.getInventoryManager().broadcastWearing();
            NetworkService.gI().sendMainCharInfo(p);
            // WorldManager.getInstance().changeMap(p, p.getPosition());

        } else if (type == 0) {
            p.respawn();
            WorldManager.getInstance().changeMap(p, DESA_SRIGALA.getPosition());
            Go go;
            if (p.getLevel() < 40)
                go = DESA_SRIGALA;
            else if (p.getLevel() < 100)
                go = KOTA_EMAS;
            else
                go = KOTA_PELABUHAN;
            MenuManager.teleport(p, go, 1);
            NetworkService.gI().sendMainCharInfo(p);
        }
    }

    private static void onMove(Session s, Message m) throws IOException {
        short x = m.in().readShort();
        short y = m.in().readShort();

        PlayerEntity p = s.getPlayer();
        if (p == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Check warp BEFORE updating position
        if (p.getMap().isWarp(x, y)) {
            if ((currentTime - p.getLastChangeMapTime()) < 2000) {
                return;
            }

            Vgo vgo;
            if ((vgo = p.getMap().getWarpAt(x, y)) != null) {
                p.setTeleport(false);
                WorldManager.getInstance().changeMap(p, new Position(vgo.getToMap(), (short) vgo.getToX(), (short) vgo.getToY()));
                p.setLastChangeMapTime(currentTime);
                return;
            }
        }

        p.setPosition(x, y);
        p.getZone().broadcastExcept(p, player -> NetworkService.gI().sendMove(player, p));

    }

    private static void onMonsterInfo(Session s, Message m) throws IOException {
        MonsterEntity entity = s.getPlayer().getZone().getMonster(m.in().readShort());
        if (entity != null) {
            NetworkService.gI().sendMonsterInfo(s.getPlayer(), entity);
        }
    }

    private static void onFireMonster(Session s, Message m) throws IOException {

        PlayerEntity p = s.getPlayer();
        if (p == null) {
            return;
        }

        byte skillId = m.in().readByte();
        byte type = m.in().readByte();
        int targetId = m.in().readShort();

        SkillEntity skill = p.getSkillData().get(skillId);
        if (skill == null) {
            return;
        }

        LvSkill lvSkill = skill.getCurrentLevelData();
        if (lvSkill == null) {
            NetworkService.gI().sendNoticeBox(s, "Skill tidak ditemukan");
            return;
        }


        // SKILL ATTACK
        if (skill.getType() == 0) {
            // find monster
            MonsterEntity monster = p.getZone().getMonster(targetId);
            if (monster == null || monster.isDead()) {
                return;
            }

            Guild guild = p.getGuild();
            if (monster.getTemplateId() == 64) {
                if (guild == null) return;
                if (monster.isMyGuild(guild.getId())) return;
            }

            int targetCount = lvSkill.targetCount; // Maximal Mob
            int areaRange = lvSkill.castRange; // IN RADIUS

            if (targetCount == 1) {

                p.useSkill(skillId, monster);

            } else {

                List<LivingEntity> targets = new ArrayList<>(p.getZone().getMonsters().stream()
                        .filter(mob -> mob != null && !mob.isDead())
                        .filter(mob -> monster.distanceTo(mob) <= areaRange)
                        .map(mob -> (LivingEntity) mob)
                        .toList());

                if (p.getTypePK() != -1) {
                    List<PlayerEntity> snapshot = new ArrayList<>(p.getZone().getPlayers());
                    List<LivingEntity> players = snapshot.stream()
                            .filter(Objects::nonNull)
                            .filter(player -> !player.isDead())
                            .filter(player -> monster.distanceTo(player) <= areaRange)
                            .filter(player -> player.getTypePK() != p.getTypePK())
                            .filter(player -> player.getId() != p.getId())
                            .limit(targetCount)
                            .map(player -> (LivingEntity) player)
                            .toList();

                    targets.addAll(players);
                }


                p.useSkillAoE(skillId, monster, targets);
            }


        }

    }

    private static void onFirePK(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte skillId = m.in().readByte();
        byte type = m.in().readByte();
        int targetId = m.in().readShort();

        SkillEntity skill = p.getSkillData().get(skillId);
        if (skill == null) {
            return;
        }

        LvSkill lvSkill = skill.getCurrentLevelData();
        if (lvSkill == null) {
            NetworkService.gI().sendNoticeBox(s, "Skill tidak ditemukan");
            return;
        }

        // SKILL ATTACK
        if (skill.getType() == 0) {

            // find player
            LivingEntity target = p.getZone().getPlayer(targetId);
            if (target == null || target.isDead()) {
                return;
            }

            if (target.isClone() && p.getGuild() != null && p.getGuild().getId() == target.getGuild().getId()) {
                NetworkService.gI().sendToast(p, "Can't attack");
                return;
            }

            int targetCount = lvSkill.targetCount; // Maximal Mob
            int areaRange = lvSkill.castRange; // IN RADIUS

            if (targetCount == 1) {

                p.useSkill(skillId, target);

            } else {

                List<LivingEntity> targets = new ArrayList<>(p.getZone().getPlayers().stream()
                        .filter(player -> player != null && !player.isDead() && player.getId() != p.getId())
                        .filter(mob -> target.distanceTo(mob) <= areaRange)
                        .map(mob -> (LivingEntity) mob)
                        .toList());


                p.useSkillAoE(skillId, target, targets);
            }

        }

    }

    private static void onGetItemMap(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null)
            return;

        short dropId = m.in().readShort();
        byte category = m.in().readByte();

        DropItem dropItem = p.getZone().getDropItem(dropId);
        if (dropItem != null) {
            if (dropItem.isExpired()) {
                p.getZone().removeDropItem(dropId);
                return;
            }

            if (!dropItem.canPickup(p.getId())) {
                int remainingSeconds = dropItem.getRemainingLockSeconds();
                NetworkService.gI().sendToast(p, String.format("Terbuka dalam %d detik lagi!", remainingSeconds));
                return;
            }
            p.getZone().removeDropItem(dropId);

            p.getInventoryManager().addToBag(dropItem.getItem());
            p.getInventoryManager().updateInventory();
            p.getZone().broadcast(player -> NetworkService.gI().sendPickUp(player, p, dropItem));
        }

    }


    private static void onUseBuff(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte skillId = m.in().readByte();
        p.useSkill(skillId, p);
    }

}
