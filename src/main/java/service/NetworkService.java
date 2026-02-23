package service;

import game.buff.BuffEffect;
import game.equipment.PlayerEquipment;
import game.friend.FriendList;
import game.guild.Guild;
import game.guild.GuildManager;
import game.guild.GuildMember;
import game.map.DropItem;
import game.map.Zone;
import game.party.Party;
import game.pet.Pet;
import game.skill.DamageContext;
import game.skill.DamageEffect;
import game.stat.StatCalculator;
import game.stat.StatType;
import manager.*;
import model.DataEffect;
import model.UpgradeLevel;
import game.effects.Effect;
import model.item.*;
import model.map.MapName;
import model.map.Point;
import model.menu.InputDialog;
import model.menu.Menu;
import game.equipment.EquipType;
import model.npc.NpcData;
import model.player.Part;
import model.reward.Reward;
import model.shop.PetTemplate;
import model.config.SVConfig;
import model.skill.LvSkill;
import model.skill.Skill;
import utils.*;
import game.map.GameMap;
import lombok.extern.slf4j.Slf4j;
import model.map.MapData;
import model.monster.Monster;
import model.player.Player;
import handler.Command;
import network.Message;
import network.Session;
import game.stat.Stats;
import game.entity.base.GameObjectType;
import game.entity.base.LivingEntity;
import game.entity.monster.MonsterEntity;
import game.entity.player.PlayerEntity;
import game.skill.SkillEntity;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static game.equipment.EquipType.*;
import static game.equipment.EquipType.LEG;

@Slf4j
public class NetworkService {
    private static final NetworkService INSTANCE = new NetworkService();

    public static NetworkService gI() {
        return INSTANCE;
    }

    public void sendNoticeBox(Session s, String text) {
        try {
            Message m = new Message(Command.NOTICE_BOX);
            m.out().writeUTF(text);
            m.out().writeUTF("");
            m.out().writeByte(15);
            s.send(m);
        } catch (Exception ignore) {
        }

    }

    public void sendNameServer(Session s) {
        Message m = new Message(61);
        try {
            m.out().writeByte(WorldManager.getInstance().mapNames.size());
            for (MapName name : WorldManager.getInstance().mapNames.values()) {
                m.out().writeUTF(name.getName());
            }
            // Quest
            m.out().writeByte(1);
            m.out().writeUTF("test");
            SVConfig config = ConfigManager.getInstance().getSvConfig();
            if (config != null) {

                m.out().writeByte(config.getUpgradeMaterials().size());
                for (short id : config.getUpgradeMaterials()) {
                    m.out().writeShort(id);
                }

                m.out().writeByte(config.getUpgradeLevels().size());
                for (UpgradeLevel level : config.getUpgradeLevels()) {
                    m.out().writeByte(level.getLevel());
                    m.out().writeInt(level.getGold());
                    m.out().writeShort(level.getGem());
                    for (int i = 0; i < 4; i++) {
                        m.out().writeByte(level.getValue()[i]);
                    }
                }
            }

            s.send(m);
        } catch (Exception ignore) {
        }
    }

    public void sendNameServer(PlayerEntity notify, byte[] materials) {
        Message m = new Message(61);
        try {
            m.out().writeByte(WorldManager.getInstance().mapNames.size());
            for (MapName name : WorldManager.getInstance().mapNames.values()) {
                m.out().writeUTF(name.getName());
            }
            // Quest
            m.out().writeByte(1);
            m.out().writeUTF("test");
            SVConfig config = ConfigManager.getInstance().getSvConfig();
            if (config != null) {

                m.out().writeByte(materials.length);
                for (short id : materials) {
                    m.out().writeShort(id);
                }

                m.out().writeByte(config.getUpgradeLevels().size());
                for (UpgradeLevel level : config.getUpgradeLevels()) {
                    m.out().writeByte(level.getLevel());
                    m.out().writeInt(level.getGold());
                    m.out().writeShort(level.getGem());
                    for (int i = 0; i < 4; i++) {
                        m.out().writeByte(level.getValue()[i]);
                    }
                }
            }

            notify.send(m);
        } catch (Exception ignore) {
        }
    }

    public void sendMonsterCatalog(Session s) {
        Message m = new Message(Command.CATALOG_MONSTER);
        List<Monster> monsters = MonsterManager.getInstance().getTemplates();

        try {
            m.out().writeShort(monsters.size());
            for (Monster monster : monsters) {
                m.out().writeShort(monster.getMid());
                m.out().writeUTF(monster.getName());
                m.out().writeByte(monster.getLevel());
                m.out().writeInt(monster.getHp());
                m.out().writeByte(monster.getTypeMove());
            }

            SVConfig sv = ConfigManager.getInstance().getSvConfig();
            if (sv == null) {

                byte[] data = FileUtils.loadFile("data/msg/monster_template");
                if (data != null) {
                    m.out().write(data);
                }

                log.debug("SV CONFIG FAILED TO LOAD FALLBACK TO USE LOCAL DATA");
            } else {

                // PREVIOUS FRAME
                byte[][] prevFrame = sv.getPreviousFrame();
                m.out().writeByte(prevFrame.length);
                for (byte[] bytes : prevFrame) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // CHAR STAND FRAME
                byte[][] dyCharStand = sv.getDyCharStand();
                m.out().writeByte(dyCharStand.length);
                for (byte[] bytes : dyCharStand) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }


                // CHAR MOVE FRAME
                byte[][] dyCharMove = sv.getDyCharMove();
                m.out().writeByte(dyCharMove.length);
                for (byte[] bytes : dyCharMove) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // DX
                byte[][] dx = sv.getDx();
                m.out().writeByte(dx.length);
                for (byte[] bytes : dx) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // DY
                byte[][] dy = sv.getDy();
                m.out().writeByte(dy.length);
                for (byte[] bytes : dy) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // MOVE LEFT AND RIGHT
                byte[][] moveLr = sv.getMoveFramesLr();
                m.out().writeByte(moveLr.length);
                for (byte[] bytes : moveLr) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // MOVE DOWN
                byte[][] moveDown = sv.getMoveFramesDown();
                m.out().writeByte(moveDown.length);
                for (byte[] bytes : moveDown) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // MOVE UP
                byte[][] moveUp = sv.getMoveFramesUp();
                m.out().writeByte(moveUp.length);
                for (byte[] bytes : moveUp) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // SPEED
                byte[] speed = sv.getSpeed();
                m.out().writeByte(speed.length);
                for (byte spd : speed) {
                    m.out().writeByte(spd);
                }

                // ATB CANNOT PAINT
                byte[] atb = sv.getAtbCantPaint();
                m.out().writeByte(atb.length);
                for (byte at : atb) {
                    m.out().writeByte(at);
                }

                // TYPE MOVE
                byte[] typeMove = sv.getPetTypeMove();
                m.out().writeByte(typeMove.length);
                for (byte val : typeMove) {
                    m.out().writeByte(val);
                }

                // EQUIP TEM
                byte[] itemEquip = sv.getItemEquip();
                m.out().writeByte(itemEquip.length);
                for (byte val : itemEquip) {
                    m.out().writeByte(val);
                }

                // EQUIP TEM ROTATE
                byte[] itemEquipRotate = sv.getItemEquipRotate();
                m.out().writeByte(itemEquipRotate.length);
                for (byte val : itemEquipRotate) {
                    m.out().writeByte(val);
                }

                // ID NEW BOSS
                short[] idBoss = sv.getIdBoss();
                m.out().writeByte(idBoss.length);
                for (short val : idBoss) {
                    m.out().writeShort(val);
                }

                // HAIR NO HAT
                byte[] hairNoHat = sv.getHairNoHat();
                m.out().writeByte(hairNoHat.length);
                for (byte val : hairNoHat) {
                    m.out().writeByte(val);
                }

                // DATA EFFECT
                List<DataEffect> dataEffects = sv.getDataEffects();
                m.out().writeByte(dataEffects.size());
                for (DataEffect eff : dataEffects) {
                    m.out().writeByte(eff.getId());
                    byte[] data = eff.getData();
                    m.out().writeShort(data.length);
                    for (byte val : data) {
                        m.out().writeByte(val);
                    }
                }

                // EFFECT SKILL
                byte[][] effectSkill = sv.getEffectSkill();
                m.out().writeShort(effectSkill.length);
                for (byte[] bytes : effectSkill) {
                    m.out().writeByte(bytes.length);
                    for (byte aByte : bytes) {
                        m.out().writeByte(aByte);
                    }
                }

                // FLY MOUNT
                byte[] flyMount = sv.getFlyMount();
                m.out().writeByte(flyMount.length);
                for (byte val : flyMount) {
                    m.out().writeByte(val);
                }

            }
            s.send(m);
            log.debug("MONSTER CATALOG SENT");
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendMessageData(Session s, int cmd, byte[] data) {
        Message m = new Message(cmd);
        try {
            m.out().write(data);
        } catch (Exception ignore) {
        }

        s.send(m);
    }

    public void sendSaveLogin(Session s, String user, String pass) {
        Message md = new Message(31);
        try {
            md.out().writeUTF(user);
            md.out().writeUTF(pass);
        } catch (IOException ignore) {

        }
        s.send(md);
    }

    public void sendChangeMap(PlayerEntity p) {
        Message m = new Message(Command.CHANGE_MAP);
        GameMap map = p.getMap();
        MapData data = map.getMapData();
        try {
            m.out().writeShort(map.getId());
            m.out().writeShort(p.getPosition().getTileX());
            m.out().writeShort(p.getPosition().getTileY());

            m.out().write(map.getMapData().toByteArray());

            m.out().writeByte(p.isTeleport() ? 1 : 0); // isTele
            m.out().writeByte(p.getZone().getId()); // AREA
            m.out().writeByte(data.getType());
            m.out().writeBoolean(data.getIsCity() == 1);
            m.out().writeBoolean(data.getShowHs() == 1);

        } catch (Exception ignore) {
        }

        p.send(m);
        sendMapNpc(p);
    }

    public void sendMaterialTemplate(Session s, MaterialItem item) {
        Message m = new Message(Command.GET_MATERIAL_TEMPLATE);
        try {
            m.out().writeShort(item.getId());
            m.out().writeShort(item.getIcon());
            m.out().writeLong(item.getPrice());
            m.out().writeUTF(item.getName());
            m.out().writeUTF(item.getDescription());
            m.out().writeByte(item.getMaterialType());
            m.out().writeByte(item.getPriceType());
            m.out().writeByte(item.getSell());
            m.out().writeShort(item.getValue());
            m.out().writeByte(item.getSell());
            m.out().writeByte(item.getColorName());
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }

        s.send(m);

    }

    public void sendRebuildItem(PlayerEntity notify, int type, int index, String text) {
        Message m = new Message(Command.REBUILD_ITEM);
        try {
            m.out().writeByte(type);
            m.out().writeShort(index);
            m.out().writeByte(3);
            m.out().writeUTF(text);
            notify.send(m);

        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendItemTemplate(Session s) {
        try {
            Message m = new Message(Command.ITEM_TEMPLATE);

            // POTION TEMPLATE
            List<PotionItem> potions = ItemManager.getInstance()
                    .filterByClass(PotionItem.class);

            m.out().writeShort(potions.size());
            for (PotionItem item : potions) {

                m.out().writeShort(item.getId());
                m.out().writeShort(item.getIcon());
                m.out().writeLong(item.getPrice());
                m.out().writeUTF(item.getName());
                m.out().writeUTF(item.getDescription());
                m.out().writeByte(item.getPotionType());
                m.out().writeByte(item.getPriceType());
                m.out().writeByte(item.getSell());
                m.out().writeShort(item.getValue());
                m.out().writeBoolean(item.getCanTrade() == 1);

            }

            // OPTION TEMPLATE
            List<ItemOption> options = ItemManager.getInstance().getAllItemOption();
            m.out().writeByte(options.size());
            for (ItemOption op : options) {
                m.out().writeUTF(op.getName());
                m.out().writeByte(op.getColor());
                m.out().writeByte(op.isPercent() ? 1 : 0);
            }

            // MATERIAL TEMPLATE
            List<MaterialItem> materials = ItemManager.getInstance()
                    .filterByClass(MaterialItem.class);

            m.out().writeShort(materials.size());
            for (MaterialItem item : materials) {

                m.out().writeShort(item.getId());
                m.out().writeShort(item.getIcon());
                m.out().writeLong(item.getPrice());
                m.out().writeUTF(item.getName());
                m.out().writeUTF(item.getDescription());
                m.out().writeByte(item.getMaterialType());
                m.out().writeByte(item.getPriceType());
                m.out().writeByte(item.getSell());
                m.out().writeShort(item.getValue());
                m.out().writeBoolean(item.getCanTrade() == 1);
                m.out().writeByte(item.getColorName());

            }

            // PRICE SETTINGS
            SVConfig sv = ConfigManager.getInstance().getSvConfig();
            m.out().writeShort(sv.getPriceSellPotion());
            m.out().writeShort(sv.getPriceSellItem());
            m.out().writeShort(sv.getHesoLevel());
            m.out().writeShort(sv.getHesoColor());
            m.out().writeShort(sv.getPriceSellQuest());
            m.out().writeShort(sv.getMaxPriceItem());
            m.out().writeShort(sv.getPriceClanIcon());
            m.out().writeByte(sv.getPriceChatWorld());


            // PET TEMPLATE
            m.out().writeByte(sv.getPetTemplate().size());
            for (PetTemplate pet : sv.getPetTemplate()) {

                m.out().writeShort(pet.getId());
                m.out().writeByte(pet.getType());

            }

            //CRAFT MATERIAL
            if (sv.getCraftMaterial() != null && sv.getCraftMaterial().length > 0) {
                m.out().writeByte(sv.getCraftMaterial().length);
                for (int id : sv.getCraftMaterial()) {
                    m.out().writeShort(id);
                }
            }

            s.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendMainCharInfo(PlayerEntity p) {

        Stats st = p.getStats();
        Message m = new Message(Command.MAIN_CHAR_INFO);
        try {
            m.out().writeShort(p.getId());
            m.out().writeUTF(p.getName());
            m.out().writeInt(p.getHp());
            m.out().writeInt(p.getMaxHp());
            m.out().writeInt(p.getMp());
            m.out().writeInt(p.getMaxMp());
            m.out().writeByte(p.getBody()[0]);
            m.out().writeByte(p.getRole());
            m.out().writeByte(p.getBody()[1]);
            m.out().writeByte(p.getBody()[2]);

            // ATTRIBUTE INFORMATIONS
            short[] attr = new short[]{0, 1, 2, 3, 4, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20, 28, 33, 34, 35, 36, 40, 29, 30, 31, 32, 181};
            m.out().writeByte(attr.length);
            for (short value : attr) {
                m.out().writeByte(value);
                m.out().writeInt(st.get(value));
            }

            m.out().writeShort(p.getLevel());
            m.out().writeShort(p.getLevelPercent()); // EXP PERCENTAGE
            m.out().writeShort(p.getPotentialPoint()); // AVAILABLE POTENTIAL POINTS
            m.out().writeShort(p.getSkillPoint()); // AVAILABLE SKILL POINTS

            m.out().writeShort(p.getSTR()); // STR
            m.out().writeShort(p.getDEX()); // DEX
            m.out().writeShort(p.getVIT()); // VIT
            m.out().writeShort(p.getINT()); // INT

            m.out().writeShort(st.getBonusSTR()); // BONUS STR
            m.out().writeShort(st.getBonusVIT()); // BONUS DEX
            m.out().writeShort(st.getBonusVIT()); // BONUS VIT
            m.out().writeShort(st.getBonusINT()); // BONUS INT

            ///// LEARNED SKILL LEVEL
            for (SkillEntity skill : p.getSkillData().values()) {
                m.out().writeByte(skill.getCurrentLevel());
            }
            //// BONUS LEARNED SKILL LEVEL
            for (int i = 0; i < 21; i++) {
                m.out().writeByte(0);
            }
            m.out().writeByte(p.getTypePK()); // TypePK
            m.out().writeShort(0); // PointPKK
            m.out().writeByte(126); // MaxBag

            Guild guild = GuildManager.getInstance().getPlayerGuild(p.getId());
            if (guild == null) {
                m.out().writeShort(-1); // CLAN
            } else {
                m.out().writeShort(guild.getIcon());
                m.out().writeInt(guild.getId());
                m.out().writeUTF(guild.getShortName());
                GuildMember member = guild.getMember(p.getId());
                m.out().writeByte(member.getPosition());
            }

            m.out().writeUTF("A2:");
            m.out().writeLong(0);

            StringBuilder sb = new StringBuilder();
            m.out().writeByte(p.getFashion().length);
            sb.append("FASHION [");
            for (int b : p.getFashion()) {
                m.out().writeShort(b);
                sb.append(b).append(",");
            }
            sb.append("]");

            m.out().writeByte(0);              // naptien
            m.out().writeShort(p.getMaskId());            // getMaskId()
            m.out().writeByte(1);              // isMaskInFront()
            m.out().writeShort(p.getCloakId());            // getCloakId()
            m.out().writeShort(p.getWeaponId());            // getWeaponId()
            m.out().writeShort(p.getMount() == null ? -1 : p.getMount().getId());           // getMountId()
            m.out().writeShort(p.getHairId());            // getHairId()
            m.out().writeShort(p.getWingId());            // getWingId()
            m.out().writeShort(p.getTitleId());            // getTitleName
            m.out().writeShort(-1);            // p.getBodyId()
            m.out().writeShort(-1);            // getLegId()
            m.out().writeShort(-1);            // getTransformId()
            p.send(m);

            log.debug(sb.toString());
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }


    }

    public void sendIcon(Session s, short id, byte[] data) {
        try {
            Message m = new Message(-51);
            m.out().writeShort(id);
            m.out().write(data);
            s.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendStatusArea(PlayerEntity notify) {

        Collection<Zone> zones = notify.getMap().getZones();
        Message m = new Message(Command.UPDATE_STATUS_AREA);
        log.debug("ZONE SIZE {}", zones.size());
        try {
            m.out().writeByte(zones.size());
            for (Zone zone : zones) {

                if (zone.isFull()) {
                    m.out().writeByte(2);
                } else if (zone.getPlayerCount() >= (zone.getMaxPlayers() / 2)) {
                    m.out().writeByte(1);
                } else {
                    m.out().writeByte(0);
                }

                m.out().writeByte(0);
                m.out().writeUTF(String.format("Zona %d %d/%d", zone.getId(), zone.getPlayerCount(), zone.getMaxPlayers()));

                log.debug("ZONE ID {} , PLAYER SIZE {}, MAX PLAYER {} ", zone.getId(), zone.getPlayerCount(), zone.getMaxPlayers());
            }
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exceptiopn", e);
        }

    }

    public void sendPartData(Session s, PartData part) {
        Message m = new Message(-52);
        try {
            m.out().writeByte(part.type);
            m.out().writeShort(part.id);
            m.out().writeInt(part.image.length);
            m.out().write(part.image);
            m.out().write(part.imageData);
            s.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendQuest(PlayerEntity p) {

        Message m = new Message(Command.QUEST);
        try {
            m.out().writeByte(10);
            m.out().writeByte(10);
            m.out().writeByte(10);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }

        p.getSession().send(m);

    }

    public void sendFillRectTime(PlayerEntity p, int type) {

        try {
            Message m = new Message(Command.FILL_REC_UPDATE_TIME);
            m.out().writeByte(type);
            switch (type) {
                case 3 -> m.out().writeInt(0);

                case 5 -> m.out().writeByte(0);

            }
            p.getSession().send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendPoints(PlayerEntity p) {
        Message m = new Message(59);
        try {
            m.out().writeInt(p.getActivePoint());
            m.out().writeInt(p.getArenaPoint());
            p.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendLoginRms(PlayerEntity p) {
        byte[][] rms = p.getRms();

        try {
            Message m = new Message(55);
            m.out().writeByte(1);
            m.out().writeShort(2);
            m.out().writeByte(-1);
            m.out().writeByte(0);
            p.getSession().send(m);

            m = new Message(55);
            m.out().writeByte(2);
            if (p.getMap().getId() == 0 && p.getLevel() < 2) { // is new begin
                m.out().writeShort(0);
            } else {
                m.out().writeShort(1);
                m.out().writeByte(0);
            }
            p.getSession().send(m);

            if (rms[0].length > 0) {
                m = new Message(55);
                m.out().writeByte(0);
                m.out().writeShort(rms[0].length);
                m.out().write(rms[0]);
                p.getSession().send(m);
            }

            if (rms[1].length > 0) {
                m = new Message(55);
                m.out().writeByte(3);
                m.out().writeShort(rms[1].length);
                m.out().write(rms[1]);
                p.getSession().send(m);
            }

        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }


    }

//    public void sendPlayerWear(PlayerEntity p) {
//        Message m = new Message(Command.CHAR_WEARING);
//
//
//        List<EquipmentItem> equipments = p.getEquipment().getAllEquipments();
//
//
//        try {
//            m.out().writeShort(p.getId());
//            m.out().writeByte(equipments.size());
//
//            for (int i = 0; i < equipments.size(); i++) {
//                EquipmentItem eq = equipments.get(i);
//                if (eq == null) {
//                    m.out().writeByte(-1);
//                    continue;
//                }
//
//                m.out().writeByte(i);
//
//                m.out().writeUTF(eq.getName());
//                m.out().writeByte(eq.getRole());
//                m.out().writeByte(eq.getType());
//                m.out().writeShort(eq.getIcon());
//                m.out().writeByte(eq.getType());
//                m.out().writeByte(eq.getPlus()); // plus item (tier)
//                m.out().writeShort(eq.getLevel());
//                m.out().writeByte(eq.getColor());
//                m.out().writeByte(eq.getOption().size());
//                for (Option op : eq.getOption()) {
//                    m.out().writeByte(op.getId());
//                    m.out().writeInt(op.getValue());
//                }
//                m.out().writeByte(1); // islock
//
//            }
//
//            m.out().writeByte(-1); // pet
//
//            short[] fashion = new short[]{-1, -1, -1, -1, -1, -1, -1};
//            m.out().writeByte(fashion.length);
//            for (short b : fashion) {
//                m.out().writeShort(b);
//            }
//        } catch (Exception ignore) {
//        }
//
//        p.getSession().send(m);
//
//    }

    public void sendSkillTemplate(PlayerEntity p) {

        Map<Byte, Skill> skills = SkillManager.getInstance().getRoleSkill(p.getRole());
        if (skills.isEmpty()) {
            return;
        }

        Message m = new Message(Command.LIST_SKILL);
        try {
            m.out().writeByte(skills.size());
            for (Skill skill : skills.values()) {
                m.out().writeByte(skill.sid);
                m.out().writeByte(skill.iconId);
                m.out().writeUTF(skill.name);
                m.out().writeByte(skill.type);
                m.out().writeShort(skill.attackRange);
                m.out().writeUTF(skill.description);
                m.out().writeByte(skill.buffType);
                m.out().writeByte(skill.subEffectType);
                m.out().writeByte(skill.levels.size());
                for (LvSkill lv : skill.levels) {
                    m.out().writeShort(lv.mpCost);
                    m.out().writeShort(lv.requiredLevel);
                    m.out().writeInt(lv.cooldown);
                    m.out().writeInt(lv.buffDuration);
                    m.out().writeByte(lv.subEffectPercent);
                    m.out().writeShort(lv.subEffectDuration);
                    m.out().writeShort(lv.bonusHp);
                    m.out().writeShort(lv.bonusMp);

                    m.out().writeByte(lv.options.length);
                    for (Option op : lv.options) {
                        m.out().writeByte(op.getId());
                        m.out().writeInt(op.getValue());
                    }

                    m.out().writeByte(lv.targetCount);
                    m.out().writeShort(lv.castRange);
                }

                m.out().writeShort(skill.performDuration);
                m.out().writeByte(skill.paintType);
                p.send(m);
            }
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

//    public void sendPlayerInventory(PlayerEntity p, ItemCategory category) {
//
//        try {
//            switch (category) {
//                case EQUIPMENT -> {
//                    Message m = new Message(16);
//                    m.out().writeByte(0);
//                    m.out().writeByte(3);
//                    m.out().writeLong(p.getGold());
//                    m.out().writeInt((int) p.getGems());
//
//                    m.out().writeByte(3);
//                    List<InventorySlot> slots = p.getInventory().getItemsByCategory(category);
//                    m.out().writeByte(slots.size());
//
//                    AtomicInteger index = new AtomicInteger(0);
//                    for (InventorySlot slot : slots) {
//
//                        EquipmentItem item = (EquipmentItem) slot.getItem();
//
//                        m.out().writeUTF(item.getName());
//                        m.out().writeByte(item.getRole()); // item clazz
//                        m.out().writeShort(index.getAndIncrement()); // id : index
//                        m.out().writeByte(item.getType()); // type only
//                        m.out().writeShort(item.getIcon()); // idicon
//                        m.out().writeByte(item.getPlus()); // tier
//                        m.out().writeShort(item.getLevel()); // level
//                        m.out().writeByte(item.getColor()); // color name
//                        m.out().writeByte(1); // can sell
//                        m.out().writeByte(item.isLock() ? 0 : 1); // can trade
//                        m.out().writeByte(item.getOption().size()); // size
//                        for (Option op : item.getOption()) {
//                            m.out().writeByte(op.getId());
//                            m.out().writeInt(op.getValue());
//                        }
//                        //
//                        if (item.getTimeUse() != 0) {
//                            long timeUse = (item.getTimeUse() - System.currentTimeMillis()) / 60_000;
//                            m.out().writeInt((int) ((timeUse > 0) ? timeUse : 1)); // time use
//                        } else {
//                            m.out().writeInt(0); // time use
//                        }
//                        m.out().writeByte(item.isLock() ? (byte) 1 : (byte) 0); // islock
//                        if (item.getExpireDate() <= 0) {
//                            m.out().writeByte(0); // b10
//                        } else {
//                            m.out().writeByte(1);

    /// /                            m.writer().writeInt(43200);
    /// /                            m.writer().writeUTF(""+pet.expiry_date);
//                            m.out().writeInt(0);
//                            m.out().writeUTF("" + item.getExpireDate());
//                        }
//                        m.out().writeByte(0); // canShell_notCanTrade
//
//                    }
//
//                    p.getSession().send(m);
//
//                }
//
//                case MATERIAL, POTION -> {
//                    Message m = new Message(16);
//                    m.out().writeByte(0);
//                    m.out().writeByte(category.getValue());
//                    m.out().writeLong(p.getGold());
//                    m.out().writeInt((int) p.getGems());
//                    m.out().writeByte(category.getValue());
//
//                    List<InventorySlot> slots = p.getInventory().getItemsByCategory(category);
//
//                    m.out().writeByte(slots.size());
//                    for (InventorySlot slot : slots) {
//                        m.out().writeShort(slot.getItem().getId());
//                        m.out().writeShort(slot.getAmount());
//                        m.out().writeByte(1);
//                        m.out().writeByte(0);
//
//                    }
//
//                    p.getSession().send(m);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    public void sendListCharacter(Session s) {
        List<Player> characters = PlayerService.gI().getAllByAccountId(s.getAccount().getId());

        Message m = new Message(Command.LIST_CHAR);
        try {

            m.out().writeByte(characters.size());
            for (Player player : characters) {

                m.out().writeInt(player.getId());
                m.out().writeUTF(player.getName());

                byte[] body = player.getBody();
                m.out().writeByte(body[0]);
                m.out().writeByte(body[2]);
                m.out().writeByte(body[1]);

                List<Part> parts = PlayerHelper.getPartPlayer(player.getId());

                m.out().writeByte(parts.size());
                for (Part part : parts) {
                    m.out().writeByte(part.getType());
                    m.out().writeByte(part.getPart());
                }

                m.out().writeShort(player.getLevel());
                m.out().writeByte(player.getRole());
                m.out().writeByte(0);
                m.out().writeByte(0);

                Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
                if (guild == null) {
                    m.out().writeShort(-1); // CLAN
                } else {
                    m.out().writeShort(guild.getIcon());
                    m.out().writeUTF(guild.getShortName());
                    m.out().writeByte(guild.getMember(player.getId()).getPosition());
                }
            }

            s.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendLevelUp(PlayerEntity notify, PlayerEntity p) {
        Message m = new Message(Command.LEVEL_UP);
        try {
            m.out().writeShort(p.getId());
            m.out().writeByte(p.getLevel());
            notify.send(m);
        } catch (IOException ignore) {
        }
    }

    public void sendMenu(PlayerEntity p, Menu menu) {

        Message m = new Message(Command.DYNAMIC_MENU);
        try {
            m.out().writeShort(menu.getNpc());
            m.out().writeByte(menu.getId()); // Index
            m.out().writeByte(menu.getMenus().size());
            for (Menu item : menu.getMenus()) {
                m.out().writeUTF(item.getName());
            }
            m.out().writeUTF(menu.getTitle());
            p.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }


    }

    public void sendUpdateExp(PlayerEntity notify, PlayerEntity p, int exp) {
        Message m = new Message(Command.SET_EXP);
        try {
            m.out().writeShort(p.getId());
            m.out().writeShort(p.getLevelPercent());
            m.out().writeInt(exp);
            notify.send(m);
        } catch (IOException ignore) {
        }
    }

    public void sendPartChar(Session s) {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try {

                    List<PartData> parts = PartDataLoader.getAllByZoom(s.getZoomLv());
                    int partIndex = PartDataLoader.getPartIndex(s.getZoomLv());

                    // Filter out type 113 and null entries
                    List<PartData> filtered = parts.stream()
                            .filter(Objects::nonNull)           // Filter null parts
                            .filter(partData -> partData.type != 113)
                            .toList();

                    int size = filtered.size();

                    // Send count message
                    Message m = new Message(Command.UPDATE_DATA);
                    m.out().writeShort(partIndex);
                    m.out().writeShort(size);
                    s.send(m);

                    // Send each part
                    for (PartData part : filtered) {
                        m = new Message(Command.LOAD_IMAGE_DATA_PART_CHAR);
                        m.out().writeByte(part.type);
                        m.out().writeShort(part.id);
                        m.out().writeInt(part.image.length);
                        m.out().write(part.image);
                        m.out().write(part.imageData);
                        s.send(m);
                    }

                } catch (Exception e) {
                    log.error("[sendPartChar] Failed to send part char data: ", e);
                }
            });
        }
    }

    public void sendMove(PlayerEntity notify, LivingEntity objectMove) {
        Message m = new Message(Command.OBJECT_MOVE);
        try {
            m.out().writeByte(objectMove.getType().code);
            if (objectMove.getType() == GameObjectType.MONSTER) {
                m.out().writeShort(((MonsterEntity) objectMove).getTemplateId());
            } else {
                m.out().writeShort(0);
            }
            m.out().writeShort(objectMove.getId());
            m.out().writeShort(objectMove.getPosition().getX());
            m.out().writeShort(objectMove.getPosition().getY());
            m.out().writeByte(0);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendPartyRequest(PlayerEntity notify, String requestFrom) {
        try {
            Message m = new Message(Command.PARTY);
            m.out().writeByte(1);
            m.out().writeUTF(requestFrom);
            notify.send(m);
        } catch (Exception e) {
            log.error("Err", e);
        }
    }

    public void sendParty(PlayerEntity notify, Party party) {
        try {
            Message m = new Message(48);
            m.out().writeByte(2);
            m.out().writeByte(party.getMembers().size());
            for (PlayerEntity p : party.getMembers().values()) {
                m.out().writeUTF(p.getName());
                m.out().writeShort(p.getLevel());
                m.out().writeByte(p.getMap().getId());
                m.out().writeByte(p.getZone().getId());
            }
            notify.send(m);
        } catch (Exception e) {
            log.error("Err ", e);
        }
    }

    public void leaveParty(PlayerEntity notify) {
        try {
            Message m = new Message(48);
            m.out().writeByte(5);
            notify.send(m);
        } catch (Exception e) {
            log.error("Err ", e);
        }
    }

    public void sendMoveOut(PlayerEntity notify, LivingEntity objectMove) {
        Message m = new Message(Command.OBJECT_MOVE);
        try {
            m.out().writeByte(objectMove.getType().code);
            if (objectMove.getType() == GameObjectType.MONSTER) {
                m.out().writeShort(((MonsterEntity) objectMove).getTemplateId());
            } else {
                m.out().writeShort(0);
            }
            m.out().writeShort(objectMove.getId());
            m.out().writeShort(objectMove.getPosition().getX());
            m.out().writeShort(objectMove.getPosition().getY());


            m.out().writeByte(127);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendPlayerExit(PlayerEntity notify, int id) {
        Message m = new Message(Command.PLAYER_EXIT);
        try {
            m.out().writeShort(id);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendRemoveActor(PlayerEntity notify, LivingEntity target) {
        Message m = new Message(Command.REMOVE_ACTOR);
        try {

            m.out().writeByte(target.getType().code);
            m.out().writeShort(target.getId());
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }


    public void sendMonsterInfo(PlayerEntity notify, MonsterEntity monster) {
        Message m = new Message(Command.MONSTER_INFO);
        try {
            m.out().writeShort(monster.getId());
            m.out().writeByte(monster.getLevel());
            m.out().writeShort(monster.getPosition().getX());
            m.out().writeShort(monster.getPosition().getY());
            m.out().writeInt(monster.getHp());
            m.out().writeInt(monster.getMaxHp());
            m.out().writeByte(20); // Skill
            m.out().writeInt(monster.getRefreshTime());

            Guild guild;
            if ((guild = monster.getGuild()) != null) {
                m.out().writeShort(guild.getIcon());
                m.out().writeInt(guild.getId());
                m.out().writeUTF(guild.getShortName());
                m.out().writeByte(122);
            } else {
                m.out().writeShort(-1); // Clan
            }
            m.out().writeByte(0); // ServerControL
            m.out().writeByte(1); // Movement Speed
            m.out().writeByte(0); //Direction

            m.out().writeUTF(""); // Summoner Name
            m.out().writeLong(-1111); // Time Revie Spawn Time
            m.out().writeByte(monster.getMonsterType().getValue()); // ColorName
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendChangeFlag(PlayerEntity notify, PlayerEntity target) {

        Message m = new Message(Command.PK);
        try {
            m.out().writeShort(target.getId());
            m.out().writeByte(target.getTypePK());
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    // Monster attack players
    public void sendMonsterFire(PlayerEntity notify, LivingEntity attacker, LivingEntity target, int damage) {
        try {

            Message m = new Message(Command.MONSTER_FIRE);
            m.out().writeByte(1);
            m.out().writeShort(attacker.getId());
            m.out().writeInt(attacker.getHp());
            m.out().writeByte(0);
            m.out().writeByte(1);
            m.out().writeShort(target.getId());
            m.out().writeInt(damage);
            m.out().writeInt(target.getHp());
            m.out().writeByte(2);
            m.out().writeByte(0);
            notify.send(m);

        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendMapNpc(PlayerEntity p) {
        Message m = new Message(Command.NPC_BIG);
        List<Point> points = p.getMap().getMapData().getNpc();
        try {

            m.out().writeByte(points.size());
            for (Point point : points) {
                NpcData npcData = NpcManager.getInstance().getNpc(point.getId());
                if (npcData == null)
                    continue;

                // Write NPC data
                m.out().writeUTF(npcData.getName());
                m.out().writeUTF(npcData.getDialogName());
                m.out().writeByte(npcData.getId());
                m.out().writeByte(npcData.getImageId());
                m.out().writeShort(point.getX());
                m.out().writeShort(point.getY());
                m.out().writeByte(npcData.getWBlock());
                m.out().writeByte(npcData.getHBlock());
                m.out().writeByte(npcData.getTotalFrame());
                m.out().writeByte(npcData.getBigAvatar());
                m.out().writeUTF(npcData.getDialogText());
                m.out().writeByte(npcData.isPerson() ? 1 : 0);
                m.out().writeByte(npcData.isShowHp() ? 1 : 0);

            }
            p.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }

    }

    public void sendOtherPlayerInfo(PlayerEntity notify, PlayerEntity target) {

        Message m = new Message(Command.OTHER_PLAYER_INFO);
        try {
            m.out().writeShort(target.getId());
            m.out().writeUTF(target.getName());
            m.out().writeByte(target.getRole());
            m.out().writeByte(target.getBody()[0]);
            m.out().writeByte(target.getBody()[1]);
            m.out().writeByte(target.getBody()[2]);
            m.out().writeShort(target.getLevel());
            m.out().writeInt(target.getHp());
            m.out().writeInt(target.getMaxHp());
            m.out().writeByte(target.getTypePK());
            m.out().writeShort(0); // POINT PK
            PlayerEquipment wearing = target.getInventoryManager().getWearing();
            m.out().writeByte(wearing.getItems().length);
            for (int i = 0; i < wearing.getItems().length; i++) {
                EquipmentItem pet = wearing.item(i);
                if (pet == null) {
                    m.out().writeByte(-1);
                    continue;
                }

                m.out().writeByte(i);
                m.out().writeUTF(pet.getName());
                m.out().writeByte(pet.getRole());
                m.out().writeByte(pet.getType());
                m.out().writeShort(pet.getIcon());
                m.out().writeByte(pet.getPart()); // show part char
                m.out().writeByte(pet.getPlus()); // plus item = tier
                m.out().writeShort(pet.getLevel());
                m.out().writeByte(pet.getColor());
                m.out().writeByte(pet.getOption().size());
                for (Option op : pet.getOption()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(StatCalculator.getBonusPlus(op, pet.getPlus()));
                }
                m.out().writeByte(0); // can sell

            }

            Guild guild = target.getGuild();
            if (guild == null) {
                m.out().writeShort(-1);
            } else {
                m.out().writeShort(guild.getIcon());
                m.out().writeUTF(guild.getShortName());
                GuildMember member = guild.getMember(target.isClone() ? target.getIdCopy() : target.getId());
                m.out().writeByte(member.getPosition());
                m.out().writeUTF(guild.getName());
            }

            Pet pet;
            if ((pet = target.getPet()) == null) {
                m.out().writeByte(-1); // pet
            } else {
                m.out().writeByte(5); // EQUIPMENT SLOT INDEX
                m.out().writeUTF(pet.getName());
                m.out().writeByte(4); // Clazz Type
                m.out().writeShort(pet.getLevel());
                m.out().writeShort(pet.getLevelPercent());
                m.out().writeByte(pet.getIcon());
                m.out().writeByte(pet.getImage());
                m.out().writeByte(pet.getFrameCount());
                m.out().writeByte(pet.getColor());

                // PET INFO
                m.out().writeInt(pet.getAgeHours());
                m.out().writeShort(pet.getGrow());
                m.out().writeShort(pet.getMaxGrow());
                m.out().writeShort(pet.getStrength());
                m.out().writeShort(pet.getDexterity());
                m.out().writeShort(pet.getVitality());
                m.out().writeShort(pet.getIntelligence());
                m.out().writeShort(pet.getMaxPoints());

                m.out().writeByte(pet.getOptions().size());

                for (PetOption op : pet.getOptions()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(op.getValue());
                    m.out().writeInt(op.getMaxValue());
                }


            }

            m.out().writeByte(0);

            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendUseMount(PlayerEntity notify, PlayerEntity target) {
        try {
            Message m = new Message(Command.USE_MOUNT);
            m.out().writeByte(0);
            m.out().writeByte(target.getMount() != null ? target.getMount().getType() : -1);
            m.out().writeShort(target.getId());
            notify.send(m);

        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendCharInfo(PlayerEntity notify, PlayerEntity target) {
        Message m = new Message(Command.CHAR_INFO);


        List<EquipmentItem> items = target.getInventoryManager().getWearing().allEquipped().stream().filter(
                item -> {
                    EquipType type = fromValue(item.getType());
                    return type == WEAPON || type == ARMOR || type == HELMET || type == LEG || type == WING;

                }
        ).toList();

        try {
            m.out().writeShort(target.getId());
            m.out().writeUTF(target.getName());
            m.out().writeShort(target.getPosition().getX());
            m.out().writeShort(target.getPosition().getY());
            m.out().writeByte(target.getRole());
            m.out().writeByte(-1); // IS BOT ?
            m.out().writeByte(target.getBody()[0]);
            m.out().writeByte(target.getBody()[1]);
            m.out().writeByte(target.getBody()[2]);
            m.out().writeShort(target.getLevel());
            m.out().writeInt(target.getHp());
            m.out().writeInt(target.getMaxHp());
            m.out().writeByte(target.getTypePK());
            m.out().writeShort(0); // Point PK

            m.out().writeByte(items.size());
            for (EquipmentItem item : items) {
                if (item != null) {
                    m.out().writeByte(item.getType());
                    m.out().writeByte(item.getPart());

                    // GEM SIZE TOTAL 12 EQUIPMENT, EACH EQUIPMENT HAS 3 SLOT GEM
                    m.out().writeByte(3);
                    // ARRAY INDEX
                    m.out().writeShort(-1);
                    m.out().writeShort(-1);
                    m.out().writeShort(-1);

                    m.out().writeShort(-1); // EFFECT
                }
            }

            // GUILD
            Guild guild = target.getGuild();
            if (guild == null) {
                m.out().writeShort(-1);
            } else {
                m.out().writeShort(guild.getIcon());
                m.out().writeInt(guild.getId());
                m.out().writeUTF(guild.getShortName());
                GuildMember member = guild.getMember(target.isClone() ? target.getIdCopy() : target.getId());
                m.out().writeByte(member.getPosition());
            }

            Pet pet;
            if ((pet = target.getPet()) == null) {
                m.out().writeByte(-1);
            } else {
                m.out().writeByte(pet.getTypeMove());
                m.out().writeByte(pet.getImage());
                m.out().writeByte(pet.getFrameCount());
            }

            // FASHION
            m.out().writeByte(target.getFashion().length);
            for (int b : target.getFashion()) {
                m.out().writeShort(b);
            }

            m.out().writeShort(-1);          //TransformationImageId
            m.out().writeByte(target.getMount() == null ? -1 : target.getMount().getType());            // Type Mount
            m.out().writeBoolean(false);     // isFootSnow
            m.out().writeByte(1);            // TypeFocus
            m.out().writeByte(0);            // TypeFire

            m.out().writeShort(target.getMaskId());            // MaskID
            m.out().writeByte(1);              // isMaskInFront()
            m.out().writeShort(target.getCloakId());            // getCloakId()
            m.out().writeShort(target.getWeaponId());            // getWeaponId()
            m.out().writeShort(target.getMount() == null ? -1 : target.getMount().getId());            // getMountId()
            m.out().writeShort(target.getHairId());            // getHairId()
            m.out().writeShort(target.getWingId());            // getWingId()
            m.out().writeShort(target.getTitleId());            // getIdName()
            m.out().writeShort(-1);            // p.getBodyId()
            m.out().writeShort(-1);            // getLegId()
            m.out().writeShort(-1);            // getTransformId()

            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendWearing(PlayerEntity notify, PlayerEntity target) {
        Message m = new Message(Command.CHAR_WEARING);

        PlayerEquipment wearing = target.getInventoryManager().getWearing();
        try {
            m.out().writeShort(target.getId());
            m.out().writeByte(wearing.getItems().length);

            for (int i = 0; i < wearing.getItems().length; i++) {
                EquipmentItem eq = wearing.item(i);
                if (eq == null) {
                    m.out().writeByte(-1);
                    continue;
                }

                m.out().writeByte(i);

                m.out().writeUTF(eq.getName());
                m.out().writeByte(eq.getRole());
                m.out().writeByte(eq.getType());
                m.out().writeShort(eq.getIcon());
                m.out().writeByte(eq.getPart());
                m.out().writeByte(eq.getPlus()); // plus item (tier)
                m.out().writeShort(eq.getLevel());
                m.out().writeByte(eq.getColor());
                m.out().writeByte(eq.getOption().size());
                for (Option op : eq.getOption()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(StatCalculator.getBonusPlus(op, eq.getPlus()));
                }
                m.out().writeByte(1); // islock

            }

            Pet pet;
            if ((pet = target.getPet()) == null) {
                m.out().writeByte(-1); // pet
            } else {
                m.out().writeByte(5); // EQUIPMENT SLOT INDEX
                m.out().writeUTF(pet.getName());
                m.out().writeByte(4); // Clazz Type
                m.out().writeShort(pet.getLevel());
                m.out().writeShort(pet.getLevelPercent());
                m.out().writeByte(pet.getIcon());
                m.out().writeByte(pet.getImage());
                m.out().writeByte(pet.getFrameCount());
                m.out().writeByte(pet.getColor());

                // PET INFO
                m.out().writeInt(pet.getAgeHours());
                m.out().writeShort(pet.getGrow());
                m.out().writeShort(pet.getMaxGrow());
                m.out().writeShort(pet.getStrength());
                m.out().writeShort(pet.getDexterity());
                m.out().writeShort(pet.getVitality());
                m.out().writeShort(pet.getIntelligence());
                m.out().writeShort(pet.getMaxPoints());

                m.out().writeByte(pet.getOptions().size());

                for (PetOption op : pet.getOptions()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(op.getValue());
                    m.out().writeInt(op.getMaxValue());
                }
                if (pet.getExpireDate() <= 0) {
                    m.out().writeByte(0);
                } else {
                    m.out().writeByte(1);
                    m.out().writeInt(43200);
                    m.out().writeUTF("" + pet.getExpireDate());
                }
            }

            m.out().writeByte(target.getFashion().length);
            for (int b : target.getFashion()) {
                m.out().writeShort(b);
            }
        } catch (Exception ignore) {
        }

        notify.send(m);
    }

    public void sendInputDialog(PlayerEntity player, InputDialog box) {
        Message m = new Message(-31);
        try {
            m.out().writeShort(box.getNpcId());
            m.out().writeByte(0);
            m.out().writeUTF(box.getTitle());
            m.out().writeByte(box.getFields().size());
            for (String field : box.getFields()) {
                m.out().writeUTF(field);
                m.out().writeByte(0);
            }
            for (String ignored : box.getFields()) { //Ignore
                m.out().writeUTF("");
                m.out().writeByte(0);
            }
            player.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendPetContainer(PlayerEntity notify) {
        try {
            Message m = new Message(44);
            m.out().writeByte(0); // No Use
            m.out().writeByte(0); // OperationType
            m.out().writeByte(9); // ItemCategory


            List<Pet> pets = notify.getPlayerPet().getAllPet();
            m.out().writeByte(0); // No use
            m.out().writeByte(pets.size());


            for (Pet pet : pets) {
                m.out().writeUTF(pet.getName());
                m.out().writeByte(4); // Clazz Type
                m.out().writeShort(pet.getId());
                m.out().writeShort(pet.getLevel());
                m.out().writeShort(pet.getLevelPercent());
                m.out().writeByte(pet.getIcon());
                m.out().writeByte(pet.getImage());
                m.out().writeByte(pet.getFrameCount());
                m.out().writeByte(pet.getColor());

                // PET INFO
                m.out().writeInt(pet.getAgeHours());
                m.out().writeShort(pet.getGrow());
                m.out().writeShort(pet.getMaxGrow());
                m.out().writeShort(pet.getStrength());
                m.out().writeShort(pet.getDexterity());
                m.out().writeShort(pet.getVitality());
                m.out().writeShort(pet.getIntelligence());
                m.out().writeShort(pet.getMaxPoints());

                m.out().writeByte(pet.getOptions().size());

                for (PetOption op : pet.getOptions()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(op.getValue());
                    m.out().writeInt(op.getMaxValue());
                }

                log.debug("Sent pet {} to container", pet.getName());

            }

            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendAddItemPetContainer(PlayerEntity notify, int category, Pet pet) {
        try {
            Message m = new Message(44);
            m.out().writeByte(28); // No Use
            m.out().writeByte(1); // OperationType
            m.out().writeByte(category); // ItemCategory
            m.out().writeByte(category);
            if (category == 9) {
                m.out().writeUTF(pet.getName());
                m.out().writeByte(4); // Clazz Type
                m.out().writeShort(pet.getId());
                m.out().writeShort(pet.getLevel());
                m.out().writeShort(pet.getLevelPercent());
                m.out().writeByte(pet.getIcon());
                m.out().writeByte(pet.getImage());
                m.out().writeByte(pet.getFrameCount());
                m.out().writeByte(pet.getColor());

                // PET INFO
                m.out().writeInt(pet.getAgeHours());
                m.out().writeShort(pet.getGrow());
                m.out().writeShort(pet.getMaxGrow());
                m.out().writeShort(pet.getStrength());
                m.out().writeShort(pet.getDexterity());
                m.out().writeShort(pet.getVitality());
                m.out().writeShort(pet.getIntelligence());
                m.out().writeShort(pet.getMaxPoints());

                m.out().writeByte(pet.getOptions().size());

                for (PetOption op : pet.getOptions()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(op.getValue());
                    m.out().writeInt(op.getMaxValue());
                }
            } else {
                EquipmentItem item = ItemManager.getInstance().getEquipment(pet.getId());
                m.out().writeUTF(item.getName());
                m.out().writeByte(item.getRole());
                m.out().writeShort(item.getId());
                m.out().writeByte(item.getType());
                m.out().writeShort(item.getIcon());
                m.out().writeByte(item.getPlus());
                m.out().writeShort(item.getLevel());
                m.out().writeByte(item.getColor());
                m.out().writeByte(item.isLock() ? 0 : 1); // CanSell
                m.out().writeByte(item.isLock() ? 0 : 1); // CanTrade
                m.out().writeByte(0); //Option Size
//                for (Option op : item.getOption()) {
//                    m.out().writeByte(op.getId());
//                    m.out().writeInt(op.getValue());
//                }
                m.out().writeInt(pet.getRemainingMinutes());
                m.out().writeByte(item.isLock() ? 1 : 0); // IsLock
            }

            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void removePetContainer(PlayerEntity player, int petId) {
        try {
            Message m = new Message(44);
            m.out().writeByte(28);
            m.out().writeByte(2);
            m.out().writeByte(9); // NO USE

            m.out().writeByte(9);
            m.out().writeShort(petId);
            player.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }

    }

    public void sendRewardDialog(PlayerEntity notify, String title, List<Reward> rewards) {
        try {
            Message m = new Message(78);
            m.out().writeUTF(title);
            m.out().writeByte(rewards.size());
            for (Reward reward : rewards) {
                BaseItem baseItem = reward.getItem();
                if (baseItem == null) continue;

                switch (baseItem.getCategory()) {
                    case EQUIPMENT -> {
                        EquipmentItem eq = (EquipmentItem) baseItem;
                        m.out().writeUTF(eq.getName()); // name
                        m.out().writeShort(eq.getIcon()); // icon
                        m.out().writeInt(reward.getQuantity()); // quantity
                        m.out().writeByte(eq.getCategory().getValue()); // type in bag
                        m.out().writeByte(eq.getPlus()); // tier
                        m.out().writeByte(eq.getColor()); // color

                    }
                    case POTION -> {
                        if (baseItem.getId() == -1) {
                            m.out().writeUTF("Gold"); // name
                            m.out().writeShort(0); // icon
                        } else if (baseItem.getId() == -2) {
                            m.out().writeUTF("Permata"); // name
                            m.out().writeShort(246); // icon
                        } else {
                            m.out().writeUTF(baseItem.getName()); // name
                            m.out().writeShort(baseItem.getIcon());
                        }
                        m.out().writeInt(reward.getQuantity()); // quantity
                        m.out().writeByte(baseItem.getCategory().getValue()); // type in bag
                        m.out().writeByte(0); // tier
                        m.out().writeByte(0); // color

                    }
                    case MATERIAL -> {
                        m.out().writeUTF(baseItem.getName()); // name
                        m.out().writeShort(baseItem.getIcon()); // icon
                        m.out().writeInt(reward.getQuantity()); // quantity
                        m.out().writeByte(baseItem.getCategory().getValue()); // type in bag
                        m.out().writeByte(0); // tier
                        m.out().writeByte(0); // color

                    }
                }
            }
            m.out().writeUTF("");
            m.out().writeByte(1);
            m.out().writeByte(1);
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendToast(PlayerEntity notify, String text) {
        Message m = new Message(Command.INFO_EASY_SERVER);
        try {
            m.out().writeUTF(text);
            m.out().writeByte(0);
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendConfirmDialog(PlayerEntity notify) {
        Message m = new Message(Command.DIALOG_SERVER);
        try {
            m.out().writeShort(notify.getId());
            m.out().writeByte(notify.getConfirmDialog().getId());
            m.out().writeUTF(notify.getConfirmDialog().getText());
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled exception", e);
        }

    }

    public void sendDropItem(PlayerEntity notify, DropItem item) {
        Message m = new Message(Command.ITEM_DROP);
        try {
            m.out().writeByte(item.getItem().getCategory().getValue());
            m.out().writeShort(item.getMobId());
            m.out().writeShort(item.getItem().getIcon());
            m.out().writeShort(item.getDropId());
            m.out().writeUTF(item.getItem().getName());
            m.out().writeByte(item.getColor());
            m.out().writeShort(-1); // id player
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendPickUp(PlayerEntity notify, PlayerEntity picked, DropItem item) {
        Message m = new Message(Command.GET_ITEM_MAP);
        try {
            m.out().writeByte(item.getItem().getCategory().getValue());
            m.out().writeShort(item.getDropId());
            m.out().writeShort(picked.getId());
            notify.send(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendPotionEffect(PlayerEntity notify, LivingEntity target, int type, int value) {
        Message m = new Message(Command.USE_POTION);
        try {
            m.out().writeByte(target.getType().code);
            m.out().writeShort(target.getId());
            m.out().writeShort(-1); // id potion in bag
            if (target.getType() == GameObjectType.PLAYER) {
                m.out().writeByte(type);
                m.out().writeInt(type == 1 ? target.getMaxMp() : target.getMaxHp()); // max hp
                m.out().writeInt(type == 1 ? target.getMp() : target.getHp()); // hp
                m.out().writeInt(value);
            } else {
                m.out().writeInt(target.getMaxHp()); // max hp
                m.out().writeInt(target.getHp()); // hp
            }

            notify.send(m);

        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendEffect(PlayerEntity notify, int type, Effect effect, LivingEntity target) {
        Message m = new Message(Command.LOAD_IMAGE_DATA_AUTO_EFF);
        try {
            // Type
            m.out().writeByte(type);

            // ImageData
            m.out().writeShort(0);
            m.out().writeByte(0);
            m.out().writeByte(0);
            // EffectID
            m.out().writeByte(effect.getEffectId());

            if (type == 2) {
                m.out().writeShort(target.getId());
                m.out().writeByte(target.getType().code);
                m.out().writeByte(0);
                m.out().writeInt(effect.getDurationMs());
            }

            notify.send(m);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public void removeEffect(PlayerEntity notify, LivingEntity target, int id) {
        Message m = new Message(Command.LOAD_IMAGE_DATA_AUTO_EFF);
        try {
            m.out().writeByte(100);
            m.out().writeShort(target.getId());
            m.out().writeByte(id);
            m.out().writeByte(target.getType().code);
            m.out().writeByte(0);
            notify.send(m);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public void sendBuff(PlayerEntity notify, LivingEntity target, BuffEffect buff) {
        try {

            Message m = new Message(Command.BUFF);
            m.out().writeByte(1);
            m.out().writeByte(buff.getType());
            m.out().writeShort(target.getId());
            m.out().writeByte(buff.getId()); // Skill ID
            m.out().writeInt(buff.getDuration()); // Time
            m.out().writeShort(target.getId());
            m.out().writeByte(target.getType().code);

            m.out().writeByte(buff.getIconId());
            m.out().writeByte(buff.getStatModifiers().size());
            for (Map.Entry<StatType, Integer> op : buff.getStatModifiers().entrySet()) {
                m.out().writeByte(op.getKey().getValue());
                m.out().writeInt(op.getValue());
            }
            notify.send(m);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public void sendEffServer(PlayerEntity notify, LivingEntity source, LivingEntity target, int effectId, int duration, int vibrateDuration) {
        try {
            Message m = new Message(Command.EFF_SERVER);
            m.out().writeByte(effectId); // CLIENT SIDE 0 - 13
            m.out().writeByte(source.getType().code);
            m.out().writeShort(source.getId());

            m.out().writeShort(duration);
            m.out().writeByte(vibrateDuration);
            m.out().writeShort(target.getId());
            notify.send(m);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public void sendEffectPlusTime(PlayerEntity notify, LivingEntity target, SkillEntity skillEntity) {
        try {
            Skill skill = skillEntity.getSkillData();
            LvSkill lv = skillEntity.getCurrentLevelData();

            Message m = new Message(Command.EFF_PLUS_TIME);
            m.out().writeByte(target.getType().code);
            m.out().writeShort(target.getId());

            m.out().writeByte(skill.subEffectType);
            m.out().writeByte(lv.buffDuration);

            notify.send(m);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public void sendTopLevel(PlayerEntity p) {
        List<Player> players = PlayerService.gI().findTopByLevel(50);

        try {

            Message m = new Message(Command.SET_PAGE);
            m.out().writeByte(6);
            m.out().writeUTF("Top Level");
            m.out().writeByte(99);
            m.out().writeInt(0);
            m.out().writeByte(players.size());
            for (Player player : players) {
                PlayerEntity entity = WorldManager.getInstance().findPlayer(player.getId());
                String info = String.format("Lv: %d", player.getLevel());
                if (entity != null && entity.isOnline()) {
                    info = String.format("Lv: %d - Map: %s Area: %s", entity.getLevel(), entity.getMap().getName(), entity.getZone().getId());
                }
                m.out().writeUTF(player.getName());
                m.out().writeByte(player.getBody()[0]);
                m.out().writeByte(player.getBody()[1]);
                m.out().writeByte(player.getBody()[2]);
                m.out().writeShort(player.getLevel());

                List<Part> parts = PlayerHelper.getPartPlayer(player.getId());
                m.out().writeByte(parts.size());

                for (Part part : parts) {
                    m.out().writeByte(part.getPart());
                    m.out().writeByte(part.getType());
                }

                m.out().writeByte(WorldManager.getInstance().isOnline(player.getId()) ? 1 : 0); // type online
                m.out().writeUTF(info);
                Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
                if (guild == null) {
                    m.out().writeShort(-1);
                } else {
                    m.out().writeShort(guild.getIcon());
                    m.out().writeUTF(guild.getShortName());
                    m.out().writeByte(guild.getMember(player.getId()).getPosition());
                }
            }

            p.send(m);
        } catch (Exception e) {
            log.error("Unhandle Exception", e);
        }
    }

    public void sendTopGuild(PlayerEntity p) {

        try {

            Message m = new Message(Command.SET_PAGE);
            m.out().writeByte(3);
            m.out().writeUTF("Top Guild");
            m.out().writeByte(0);
            m.out().writeInt(0);

            List<Guild> guilds = GuildManager.getInstance()
                    .getGuilds()
                    .values()
                    .stream()
                    .sorted(Comparator.comparingInt(Guild::getLevel))
                    .toList();

            m.out().writeByte(guilds.size());
            for (Guild guild : guilds) {

                m.out().writeUTF(guild.getName());
                m.out().writeInt(guild.getId());
                m.out().writeShort(guild.getIcon());
                m.out().writeUTF(guild.getShortName());
                m.out().writeUTF(guild.getSlogan());

            }

            p.send(m);
        } catch (Exception e) {
            log.error("Unhandle Exception", e);
        }
    }


    // Player attack monsters
    public void sendFireMonster(PlayerEntity notify, DamageContext ctx) {
        try {

            Message m = new Message(Command.FIRE_MONSTER);
            m.out().writeShort(ctx.getAttacker().getId());
            m.out().writeByte(ctx.getSkill().getSkillId());
            m.out().writeByte(1);
            m.out().writeShort(ctx.getDefender().getId());
            m.out().writeInt(ctx.getFinalDamage()); // dame
            m.out().writeInt(ctx.getDefender().getHp()); // target HP after;

            m.out().writeByte(ctx.getEffect().size());
            for (DamageEffect effect : ctx.getEffect()) {
                m.out().writeByte(effect.getId()); // 1: armor penetration, 2: life steal, 3: mana steal, 4: critical hit, 5: counterattack
                m.out().writeInt(effect.getDamage());
            }

            m.out().writeInt(ctx.getAttacker().getHp());
            m.out().writeInt(ctx.getAttacker().getMp());
            m.out().writeByte(11);
            m.out().writeInt(0);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendPlayerDie(PlayerEntity notify, LivingEntity killer, LivingEntity victim) {
        Message m = new Message(Command.DIE_PLAYER);
        try {
            m.out().writeShort(victim.getId());
            m.out().writeShort(killer.getId());
            m.out().writeShort(0); // Killer Point PK
            m.out().writeByte(killer.getType().code);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }

    }

    public void sendFireObject(PlayerEntity notify, DamageContext ctx) {
        try {

            Message m = new Message(Command.FIRE_PK);
            m.out().writeShort(ctx.getAttacker().getId());
            m.out().writeByte(ctx.getSkill().getSkillId());
            m.out().writeByte(1);
            m.out().writeShort(ctx.getDefender().getId());
            m.out().writeInt(ctx.getFinalDamage()); // dame
            m.out().writeInt(ctx.getDefender().getHp()); // target HP after;


            m.out().writeByte(ctx.getEffect().size());
            for (DamageEffect effect : ctx.getEffect()) {
                m.out().writeByte(effect.getId()); // 1: armor penetration, 2: life steal, 3: mana steal, 4: critical hit, 5: counterattack
                m.out().writeInt(effect.getDamage());
            }

            m.out().writeInt(ctx.getAttacker().getHp());
            m.out().writeInt(ctx.getAttacker().getMp());
            m.out().writeByte(11);
            m.out().writeInt(0);
            notify.send(m);

        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendChatPopup(PlayerEntity notify, LivingEntity owner, String chat) {
        try {
            Message m = new Message(Command.CHAT_POPUP);
            m.out().writeShort(owner.getId());
            m.out().writeByte(owner.getType().code);
            m.out().writeUTF(chat);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendFriendRequest(PlayerEntity notify, String name) {
        try {
            Message m = new Message(Command.FRIEND);
            m.out().writeByte(0);
            m.out().writeUTF(name);
            notify.send(m);
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }
    }

    public void sendFriendList(PlayerEntity notify, FriendList list) {

        List<Player> playerList = PlayerService.gI().findAllById(list.getFriends());


        try {
            Message m = new Message(Command.FRIEND);
            m.out().writeByte(4);
            m.out().writeUTF("Daftar Teman");
            m.out().writeByte(0);

            m.out().writeByte(playerList.size());
            for (Player player : playerList) {
                m.out().writeUTF(player.getName());
                m.out().writeByte(player.getBody()[0]);
                m.out().writeByte(player.getBody()[1]);
                m.out().writeByte(player.getBody()[2]);
                m.out().writeShort(player.getLevel());

                List<Part> parts = PlayerHelper.getPartPlayer(player.getId());
                m.out().writeByte(parts.size());
                for (Part part : parts) {
                    m.out().writeByte(part.getType());
                    m.out().writeByte(part.getPart());
                }

                m.out().writeByte(WorldManager.getInstance().isOnline(player.getId()) ? 1 : 0);
                m.out().writeShort(-1);
            }

            notify.send(m);

        } catch (Exception e) {
            log.error("FriendList error {}", e.getMessage(), e);
        }
    }

    public void sendChatWorld(PlayerEntity notify, String chat) {
        try {
            Message m = new Message(Command.INFO_EASY_SERVER);
            m.out().writeUTF(chat);
            m.out().writeByte(1);
            notify.send(m);
        } catch (Exception e) {
            log.error("err ", e);
        }
    }


    public void sendChatTab(PlayerEntity notify, String from, String chat) {
        try {
            Message m = new Message(Command.CHAT_TAB);
            m.out().writeUTF(from);
            m.out().writeUTF(chat);
            notify.send(m);
        } catch (Exception e) {
            log.error("err ", e);
        }
    }

    public void sendUseItemArena(PlayerEntity notify, PlayerEntity target, int type) {
        try {
            Message m = new Message(Command.USE_ITEM_ARENA);
            m.out().writeByte(type);
            switch (type) {
                // BUFF DAMAGE MONSTER
                case 0 -> {
                    m.out().writeByte(1); // 1 true 0 false
                }
                // TRANSFORM
                case 1 -> {
                    m.out().writeShort(target.getTransformId()); // TRANSFORM ID
                    m.out().writeShort(target.getId()); // OBJECT ID
                }
                case 2 -> {
                    m.out().writeByte(1); // PAINT PLAYER 1 TRUE 0 FALSE
                    m.out().writeShort(target.getId()); // OBJECT ID
                }
            }
            notify.send(m);
        } catch (Exception e) {
            log.error("err ", e);
        }

    }

    public void sendUpdateMarkKiller(PlayerEntity notify, PlayerEntity target, int type) {
        try {
            Message m = new Message(Command.MARKKILLER);
            m.out().writeByte(type);
            switch (type) {
                // BUFF DAMAGE MONSTER
                case 0 -> {
                    m.out().writeShort(1);
                    m.out().writeShort(target.getTotalKill());
                }
                // TRANSFORM
                case 1 -> {
                    m.out().writeShort(target.getTransformId()); // TRANSFORM ID
                    m.out().writeShort(target.getId()); // OBJECT ID
                }
                case 2 -> {
                    m.out().writeByte(1); // PAINT PLAYER 1 TRUE 0 FALSE
                    m.out().writeShort(target.getId()); // OBJECT ID
                }
            }
            notify.send(m);
        } catch (Exception e) {
            log.error("err ", e);
        }

    }
}
