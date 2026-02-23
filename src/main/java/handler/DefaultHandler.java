package handler;

import game.guild.Guild;
import game.guild.GuildManager;
import game.equipment.PlayerEquipment;
import game.inventory.PlayerInventory;
import game.map.GameMap;
import language.LanguageType;
import lombok.extern.slf4j.Slf4j;
import manager.*;
import model.config.SVConfig;
import model.account.Account;
import model.item.MaterialItem;
import model.menu.Menu;
import model.menu.MenuHelper;
import model.player.*;
import service.*;
import network.Message;
import network.Session;
import utils.*;
import game.entity.player.PlayerEntity;

import java.io.IOException;

import static language.Key.*;
import static model.npc.Go.DESA_SRIGALA;
import static model.npc.NpcName.CAOCAO;


@Slf4j
public class DefaultHandler implements MessageHandler {

    @Override
    public void onMessage(Session s, Message m) throws IOException {

        switch (m.command) {
            case Command.LOGIN -> handleLogin(s, m);
            case Command.CREATE_CHAR -> handleCreateCharacter(s, m);
            case Command.SELECT_CHAR -> handleSelectCharacter(s, m);
            case Command.LOAD_IMAGE -> {
                short iconId = m.in().readShort();
                byte[] data = IconHelper.getIcon(s.getZoomLv(), iconId);
                if (data != null) {
                    NetworkService.gI().sendIcon(s, iconId, data);
                }
            }
            case Command.LOAD_IMAGE_DATA_PART_CHAR -> {
                byte type = m.in().readByte();
                short id = m.in().readShort();
                PartData partData = PartDataLoader.getByZoom(s.getZoomLv(), type, id);
                if (partData != null) {
                    NetworkService.gI().sendPartData(s, partData);
                }
            }

            case Command.GET_MATERIAL_TEMPLATE -> {
                MaterialItem item = ItemManager.getInstance().getMaterial(m.in().readShort());
                if (item != null) {
                    NetworkService.gI().sendMaterialTemplate(s, item);
                }
            }

            case Command.FIRE_MONSTER,
                 Command.FIRE_PK,
                 Command.BUFF,
                 Command.MONSTER_INFO,
                 Command.OBJECT_MOVE,
                 Command.GO_HOME,
                 Command.CHAR_INFO,
                 Command.GET_ITEM_MAP,
                 Command.OTHER_PLAYER_INFO,
                 Command.PK,
                 Command.NPC_INFO -> MapHandler.onMessage(s, m);


            case Command.CHAT_POPUP,
                 Command.CHAT_TAB,
                 Command.CHAT_WORLD, Command.FRIEND, Command.PARTY -> SocialHandler.onMessage(s, m);


            case Command.DYNAMIC_MENU,
                 Command.DIALOG_MORE_OPTION_SERVER,
                 Command.DIALOG_SERVER -> UIHandler.onMessage(s, m);

            case Command.BUY_ITEM,
                 Command.USE_POTION,
                 Command.USE_ITEM,
                 Command.DELETE_ITEM,
                 Command.REBUILD_ITEM,
                 Command.HOP_RAC,
                 Command.USE_MOUNT -> ItemHandler.onMessage(s, m);

            case Command.UPDATE_CHAR_CHEST -> {
                PlayerEntity p = s.getPlayer();
                if (p == null) return;

                p.getInventoryManager().handleBoxTransfer(m);
            }
            case Command.UPDATE_PET_CONTAINER, Command.PET_EAT -> PetHandler.onMessage(s, m);
            case Command.CLAN -> GuildHandler.handleGuild(s, m);

            case Command.ADD_BASE_SKILL_POINT -> handlePlusPoint(s, m);
            case Command.MINI_GAME -> {

                PlayerEntity px = s.getPlayer();
                if (px == null) return;

                MenuManager.openOtherMenu(px);

            }
            case Command.SAVE_RMS_SERVER -> handleSaveRMs(s, m);
            case Command.PLAYER_SUCKHOE -> NetworkService.gI().sendPoints(s.getPlayer());
            case Command.NAME_SERVER -> {
                NetworkService.gI().sendMonsterCatalog(s);
                NetworkService.gI().sendItemTemplate(s);
                NetworkService.gI().sendNameServer(s);
                //NetworkService.gI().sendMessageData(s, 61, FileUtils.loadFile("data/msg/name_server"));
            }
            default -> {
            }
        }
    }


    private void handlePlusPoint(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        int type = m.in().readByte();
        byte index = m.in().readByte();
        int value = 1;

        if (m.in().available() > 0) {
            value = m.in().readShort();
        }

        switch (type) {
            case 0 -> {
                p.addPotentialPoint(index, value);
                NetworkService.gI().sendMainCharInfo(p);
            }
            case 1, 2 -> {
                if (p.upgradeSkill(index, value)) {
                    NetworkService.gI().sendMainCharInfo(p);
                }
            }
        }
    }


    private void handleSaveRMs(Session s, Message m) throws IOException {
        byte type = m.in().readByte();
        byte id = m.in().readByte();
        short size = m.in().readShort();

        log.debug("SAVE RMS Type={} ID:{} SIZE:{}", type, id, size);
        if (size > 0) {
            byte[] data = new byte[size];
            m.in().readFully(data);

            PlayerEntity p = s.getPlayer();
            if (p == null) {
                return;
            }

            switch (id) {
                case 0 -> p.getRms()[0] = data;
                case 3 -> {
                    if (size == 11) {
                        p.getRms()[1] = data;
                        for (byte val : p.getRms()[1]) {
                            log.debug("Value: {} ", val);
                        }
                    }
                }
            }
        }
    }

    private void handleLogin(Session s, Message m) throws IOException {

        LanguageManager lang = LanguageManager.getInstance();

        String user = m.in().readUTF();
        String pass = m.in().readUTF();
        m.in().readUTF(); // version
        m.in().readUTF(); // clinePro
        m.in().readUTF(); // pro
        m.in().readUTF(); // agent
        s.setZoomLv(m.in().readByte());
        m.in().readByte(); // device
        m.in().readInt(); // id
        m.in().readByte(); // area
        m.in().readByte(); // !Main.isPC ? 0 : 1
        m.in().readByte(); // IndexRes
        m.in().readByte(); // indexInfoLogin
        m.in().readByte(); // fake byte
        short indexCharPar = m.in().readShort();

        if (user.equals("1") && pass.equals("1")) {
            String newUser = String.format("user_%d", System.nanoTime());
            String newPass = "123456";
            boolean register = AccountService.gI().createAccount(newUser, newPass);
            if (register) {
                Account account = AccountService.gI().login(newUser, newPass);

                if (account == null) {
                    NetworkService.gI().sendNoticeBox(s, lang.get(WRONG_CREDENTIAL, LanguageType.ENGLISH));
                    return;
                }


                account.setIpAddress(s.getIpAddress());
                s.setAccount(account);

                AccountService.gI().save(account);
                NetworkService.gI().sendSaveLogin(s, newUser, newPass);

                if (indexCharPar != PartDataLoader.getPartIndex(s.getZoomLv())) {
                    NetworkService.gI().sendPartChar(s);
                    NetworkService.gI().sendListCharacter(s);

                } else {
                    NetworkService.gI().sendListCharacter(s);
                }


            }

            return;
        }

        Account account = AccountService.gI().login(user, pass);

        if (account == null) {
            NetworkService.gI().sendNoticeBox(s, lang.get(WRONG_CREDENTIAL, LanguageType.ENGLISH));
            return;
        }

        account.setIpAddress(s.getIpAddress());
        s.setAccount(account);

        AccountService.gI().save(account);
        NetworkService.gI().sendSaveLogin(s, user, pass);

        if (indexCharPar != PartDataLoader.getPartIndex(s.getZoomLv())) {
            NetworkService.gI().sendPartChar(s);
            NetworkService.gI().sendListCharacter(s);

        } else {
            NetworkService.gI().sendListCharacter(s);
        }
    }

    private void handleCreateCharacter(Session s, Message m) throws IOException {

        byte clazz = m.in().readByte();
        String name = m.in().readUTF().toLowerCase();
        byte hair = m.in().readByte();
        byte eye = m.in().readByte();
        byte head = m.in().readByte();

        if (PlayerService.gI().existsByName(name)) {
            NetworkService.gI().sendNoticeBox(s, LanguageManager.getInstance().get(NAME_USED, s.getLanguage()));
            return;
        }

        Player player = new Player();
        player.setBody(new byte[]{head, eye, hair});
        player.setName(name);
        player.setRole(clazz);
        player.setLevel(1);
        player.setGems(5000);
        player.setGold(20_000);
        player.setStrength(5);
        player.setDexterity(5);
        player.setVitality(5);
        player.setIntelligence(5);
        player.setPotentialPoint(1);
        player.setSkillPoint(1);
        player.setUid(s.getAccount().getId());


        long uid;
        if ((uid = PlayerService.gI().createPlayer(player)) != -1) {
            PlayerInventory inventory = new PlayerInventory(126);
            inventory.add(ItemManager.getInstance().getPotion(2), 100);
            inventory.add(ItemManager.getInstance().getPotion(4), 100);

            inventory.setPlayerId((int) uid);
            InventoryService.gI().createInventory(inventory);
            inventory.clear();

            InventoryService.gI().createStorage(inventory);
            PlayerEquipment equipment = InventoryService.createStarterEquipment(clazz);

            equipment.setPlayerId((int) uid);
            InventoryService.gI().createEquipment(equipment);

            NetworkService.gI().sendListCharacter(s);
        } else {
            NetworkService.gI().sendNoticeBox(s, LanguageManager.getInstance().get(NAME_USED, s.getLanguage()));
        }

    }

    private void handleSelectCharacter(Session s, Message m) throws IOException {
        m.in().readByte();
        int characterId = m.in().readInt();
        Player player = PlayerService.gI().getPlayer(characterId);

        if (player == null) {
            NetworkService.gI().sendNoticeBox(s, String.format("Character %d not found", characterId));
            return;
        }

        // Check first if player is on mode AFK
        PlayerEntity entity = WorldManager.getInstance().findPlayer(player.getId());
        if (entity != null) {
            if (!entity.isModeBot()) {
                entity.sendMessageDialog(LanguageManager.getInstance().get(ALREADY_LOGIN, s.getLanguage()));
                entity.getSession().close();
                entity.unbindSession();
            }
            WorldManager.getInstance().leaveMap(entity);
            s.bindPlayer(entity);
            entity.setModeBot(false);
            entity.setBot(null);
            sendInitialPackets(entity);

            return;
        }

        entity = PlayerMapper.toEntity(player);
        s.bindPlayer(entity);
        entity.initial();
//        if (s.getAccount().getLanguage() == null) {
//            Menu selectLangMenu = Menu.builder()
//                    .npc(CAOCAO.getId())
//                    .title("Select Language")
//                    .name("")
//                    .menus(MenuHelper.forNpc(CAOCAO.getId())
//                            .menu("English", p -> {
//                                s.setLanguage(LanguageType.ENGLISH);
//                                s.getAccount().setLanguage(LanguageType.ENGLISH);
//                                AccountService.gI().save(s.getAccount());
//                                sendInitialPackets(p);
//                            })
//                            .menu("Indonesia", p -> {
//                                s.setLanguage(LanguageType.INDONESIAN);
//                                s.getAccount().setLanguage(LanguageType.INDONESIAN);
//                                AccountService.gI().save(s.getAccount());
//                                sendInitialPackets(p);
//                            })
//                            .menu("Close", "Close", PlayerEntity::closeMenuDialog)
//                            .build())
//                    .build();
//
//            MenuManager.openMenu(entity, selectLangMenu);
//
//        }
        sendInitialPackets(entity);
    }

    private void sendInitialPackets(PlayerEntity player) {
        NetworkService net = NetworkService.gI();

        net.sendQuest(player);
        net.sendFillRectTime(player, 3);
        net.sendMainCharInfo(player);


        net.sendMessageData(player.getSession(), Command.LOGIN, FileUtils.loadFile("data/msg/table_map"));
        GameMap map = WorldManager.getInstance().getGameMap(player.getPosition().getMap());
        if (map.isBattleMap()) {
            player.setPosition(DESA_SRIGALA.getPosition());
        }
        WorldManager.getInstance().enterMap(player);
        player.getInventoryManager().updateInventory();
        player.getInventoryManager().updateStorage();

        net.sendSkillTemplate(player);
        net.sendLoginRms(player);
        net.sendFillRectTime(player, 5);
        SVConfig config = ConfigManager.getInstance().getSvConfig();
        net.sendChatTab(player, "Inbox", config.getNotifServer());
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild != null) {
            net.sendChatTab(player, "Guild", guild.getNotification());
        }

    }

}
