package manager;

import database.DatabaseLoader;
import game.effects.StatEffect;
import game.effects.StatModifier;
import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import game.equipment.EquipmentSlot;
import game.equipment.PlayerEquipment;
import game.event.EventManager;
import game.event.btf.BTF;
import game.event.btf.BTFState;
import game.event.btf.Team;
import game.event.task.DailyAbsent;
import game.guild.Guild;
import game.guild.GuildManager;
import game.guild.GuildMember;
import game.guild.GuildResult;
import game.stat.StatType;
import lombok.extern.slf4j.Slf4j;
import model.account.Account;
import game.effects.Effect;
import model.item.BaseItem;
import model.item.EquipmentItem;
import model.item.PartSettings;
import model.item.PartType;
import model.menu.ConfirmDialog;
import model.menu.InputDialog;
import model.menu.Menu;
import model.menu.MenuHelper;
import model.npc.Go;
import service.AccountService;
import service.NetworkService;
import service.ShopService;
import utils.CryptoUtils;
import utils.StringUtils;
import utils.ValidationUtils;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static game.equipment.EquipType.*;
import static language.Key.*;
import static model.npc.Go.*;
import static model.npc.NpcName.*;

@Slf4j
public class MenuManager {

    public static void handleMenuSelection(PlayerEntity player, short npcId, byte menuId, byte index) throws IOException {
        Menu current = player.getCurrentMenu();

        if (!isValidMenu(current, npcId, menuId)) {
            log.warn("Invalid menu state for player {}: npc={}, menuId={}", player.getId(), npcId, menuId);
            return;
        }

        List<Menu> options = current.getMenus();
        if (!isValidIndex(index, options)) {
            log.warn("Invalid menu index {} for player {}", index, player.getId());
            return;
        }

        Menu selected = options.get(index);
        executeMenuSelection(player, selected);
    }

    private static boolean isValidMenu(Menu menu, short npcId, byte menuId) {
        return menu != null && menu.getNpc() == npcId && menu.getId() == menuId;
    }

    private static boolean isValidIndex(byte index, List<Menu> options) {
        return options != null && index >= 0 && index < options.size();
    }

    private static void executeMenuSelection(PlayerEntity player, Menu selected) throws IOException {
        // Check if it's a navigation action (like Back button)
        boolean isNavigation = selected.getTitle().equals("Back");

        // If menu has submenus, navigate to them
        if (hasSubmenus(selected)) {
            player.setCurrentMenu(selected); // This adds current to history
            player.sendMenuDialog(selected);

            // Execute action after navigation if present

            selected.perform(player);

            return;
        }

        // Execute action if present (leaf menu)

        selected.perform(player);


        // Don't auto-close if it's a navigation action
        if (!isNavigation && !hasSubmenus(selected)) {
            player.closeMenuDialog();
        }
    }

    private static boolean hasSubmenus(Menu menu) {
        return menu.getMenus() != null && !menu.getMenus().isEmpty();
    }

    // Helper to open root menu
    public static void openMenu(PlayerEntity player, Menu rootMenu) {
        player.setMenuDialogRoot(rootMenu);
        player.sendMenuDialog(rootMenu);
    }

    // Helper to navigate back to parent menu (if tracking menu history)
    public static void navigateBack(PlayerEntity player) {
        player.navigateToParent();
    }


    public static void refreshMenu(PlayerEntity player, List<Menu> newMenus) {
        if (newMenus == null || newMenus.isEmpty()) return;

        // create a fake root menu to hold them
        Menu root = Menu.builder()
                .npc(newMenus.getFirst().getNpc())
                .id(0)
                .title("")
                .name("")
                .menus(newMenus)
                .build();
        openMenu(player, root);
    }

    // ==================== TELEPORT METHODS ====================
    public static void openTeleportLevel1(PlayerEntity player, int npcId) {
        Menu teleportMenu = Menu.builder()
                .npc(npcId)
                .title("Teleporter")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(npcId)
                        .submenu("Ke Kota", createCitiesLevel1(npcId))
                        .submenu("Map Leveling", createDungeonsLevel1(npcId))
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    private static List<Menu> createCitiesLevel1(int npcId) {
        return MenuHelper.SubMenuBuilder.create(npcId)
                .item("Desa Srigala", p -> {
                    teleport(p, DESA_SRIGALA, 1);
                })
                .item("Kota Emas", p -> {
                    teleport(p, KOTA_EMAS, 40);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> createDungeonsLevel1(int npcId) {
        return MenuHelper.SubMenuBuilder.create(npcId)
                .item("Gua Api", p -> {
                    teleport(p, GUA_API, 4);
                })
                .item("Hutan Ilusi", p -> {
                    teleport(p, HUTAN_ILUSI, 15);
                })
                .item("Lembah Misterius", p -> {
                    teleport(p, LEMBAH_MISTERIUS, 15);
                })
                .item("Danau Kenangan", p -> {
                    teleport(p, DANAU_KENANGAN, 15);
                })
                .item("Pesisir", p -> {
                    teleport(p, PESISIR, 20);
                })
                .item("Jurang Batu", p -> {
                    teleport(p, JURANG_BATU, 20);
                })
                .item("Karang Tersembunyi", p -> {
                    teleport(p, KARANG_TERSEMBUNYI, 30);
                })
                .item("Rawa", p -> {
                    teleport(p, RAWA, 30);
                })
                .item("Kuil Kuno", p -> {
                    teleport(p, KUIL_KUNO, 30);
                })
                .item("Gua Kelalawar", p -> {
                    teleport(p, GUA_KELALAWAR, 30);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    public static void openTeleportLevel40(PlayerEntity player, int npcId) {
        Menu teleportMenu = Menu.builder()
                .npc(npcId)
                .title("Teleporter")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(npcId)
                        .submenu("Ke Kota", createCitiesLevel40(npcId))
                        .submenu("Map Leveling", createDungeonsLevel40(npcId))
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    private static List<Menu> createCitiesLevel40(int npcId) {
        return MenuHelper.SubMenuBuilder.create(npcId)

                .item("Kota Emas", p -> {
                    teleport(p, KOTA_EMAS, 40);
                })
                .item("Kota Pelabuhan", p -> {
                    teleport(p, KOTA_PELABUHAN, 1);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    private static List<Menu> createDungeonsLevel40(int npcId) {
        return MenuHelper.SubMenuBuilder.create(npcId)
                .item("Gurun", p -> {
                    teleport(p, GURUN, 4);
                })
                .item("Jurang Tenggelam", p -> {
                    teleport(p, JURANG_TENGGELAM, 15);
                })
                .item("Kuburan Pasir", p -> {
                    teleport(p, KUBURAN_PASIR, 15);
                })
                .item("Mata Air Hantu", p -> {
                    teleport(p, MATA_AIR_HANTU, 15);
                })
                .item("Makam Lt 1", p -> {
                    teleport(p, MAKAM_LT1, 20);
                })
                .item("Makam Lt 2", p -> {
                    teleport(p, MAKAM_LT2, 20);
                })
                .item("Makam Lt 3", p -> {
                    teleport(p, MAKAM_LT3, 20);
                })
                .item("Daratan Tinggi", p -> {
                    teleport(p, DARATAN_TINGGI, 30);
                })
                .item("Tebing Curam", p -> {
                    teleport(p, TEBING_CURAM, 30);
                })
                .item("Dunia Atas", p -> {
                    teleport(p, DUNIA_ATAS, 30);
                })
                .item("Bawah Tanah", p -> {
                    teleport(p, BAWAH_TANAH, 30);
                })
                .item("Gerbang Dunia Bawah", p -> {
                    teleport(p, GERBANG_DUNIA_BAWAH, 20);
                })
                .item("Back", "Kembali", MenuManager::navigateBack)
                .build();
    }

    public static void teleport(PlayerEntity player, Go go, int levelReq) {
        if (player.getLevel() < levelReq) {
            player.sendMessageDialog("Mmbutuhkan level " + levelReq);
            return;
        }
        player.setTeleport(true);
        WorldManager.getInstance().changeMap(player, go.getPosition());
        player.closeMenuDialog();
    }

    // ==================== OTHER MENU ====================

    public static void openOtherMenu(PlayerEntity player) {
        MenuHelper helper = MenuHelper.forNpc(-100);
        if (player.getSession().getAccount().getRole() > 0) {
            helper.submenu("Admin", createAdminMenu());
        }

        helper.menu("Mode AFK", p -> {
                    ConfirmDialog afk = ConfirmDialog
                            .builder()
                            .id(-1)
                            .text("Kamu akan terputus dalam game, lanjutkan?")
                            .onRespond((px, yesOrNo, args) -> {
                                if (yesOrNo) {
                                    px.initBot();
                                    px.getSession().close();
                                    px.unbindSession();
                                }
                            })
                            .build();

                    p.openConfirmDialog(afk);
                })
                .menu("Generate Item", p -> {
                    List<EquipmentItem> items = ItemManager.getInstance()
                            .filterByClass(EquipmentItem.class).stream()
                            .filter(item -> item.getLevel() >= p.getLevel() && item.getLevel() < p.getLevel() + 20 && item.getColor() == 4)
                            .filter(item -> item.getRole() == p.getRole())
                            .limit(p.getInventoryManager().getBagSpace() - 5)
                            .toList();

                    for (EquipmentItem item : items) {
                        if (!p.getInventoryManager().hasBagSpace()) {
                            break; // Stop if inventory is full
                        }
                        item.setPlus((byte) 15);
                        p.getInventoryManager().addToBag(item);
                    }
                    p.getInventoryManager().updateInventory();
                })
                .menu("Ke Kota", p -> {

                    if (p.getLevel() >= 1 && p.getLevel() <= 40) {
                        teleport(p, DESA_SRIGALA, 1);
                    } else if (p.getLevel() >= 40 && p.getLevel() <= 100) {
                        teleport(p, KOTA_EMAS, 40);
                    } else {
                        teleport(p, KOTA_PELABUHAN, 100);
                    }
                })

                .submenu("Lainya", createOtherSub(-100, player));


        helper.menu("Close", "Tutup", PlayerEntity::closeMenuDialog);

        Menu root = Menu
                .builder()
                .title("Helper")
                .name("Helper")
                .menus(helper.build()).build();

        MenuManager.openMenu(player, root);
    }

    public static List<Menu> createAdminMenu() {
        return MenuHelper.forNpc(-100)
                .menu("Add Effect", player -> {
                    InputDialog dialog = InputDialog

                            .builder()
                            .title("Effect")
                            .npcId(-100)
                            .fields(List.of("ID", "Duration"))
                            .action((p, args) -> {
                                try {

                                    int id = Integer.parseInt(args[0]);
                                    int duration = Integer.parseInt(args[1]);
                                    StatEffect effect = StatEffect.buff(
                                            id, -1, duration * 1000, new StatModifier(StatType.EVADE, 10000));

                                    p.applyEffect(effect);
                                } catch (NumberFormatException e) {
                                    log.error("Invalid Format ", e);
                                }


                            }).build();


                    player.openInput(dialog);
                })
                .menu("Remove Effect", player -> {
                    InputDialog dialog = InputDialog

                            .builder()
                            .title("Effect")
                            .npcId(-100)
                            .fields(List.of("ID"))
                            .action((p, args) -> {
                                try {
                                    int id = Integer.parseInt(args[0]);
                                    p.getZone().broadcast(n -> NetworkService.gI().removeEffect(n, p, id));
                                } catch (NumberFormatException e) {
                                    log.error("Invalid Format ", e);
                                }

                            }).build();


                    player.openInput(dialog);
                })
                .menu("Atur Level", p -> {
                    if (p.getSession().getAccount().getRole() < 1) {
                        p.sendMessageDialog("Belum ada fitur");
                        return;
                    }

                    InputDialog levelUp = InputDialog.builder()
                            .title("Atur Level")
                            .fields(List.of("level"))
                            .action((px, input) -> {
                                int value = Integer.parseInt(input[0]);
                                px.setLevelTo(value);
                                log.info("Value {}", value);
                            })
                            .build();

                    p.openInput(levelUp);
                })
                .menu("Find Item", p -> {
                    InputDialog inputDialog = InputDialog.builder()
                            .title("Find Item")
                            .fields(List.of("Category", "Item ID", "Quantity"))
                            .action((px, input) -> {
                                if (!StringUtils.isNumeric(input[0])) return;
                                int category = Integer.parseInt(input[0]);

                                if (!StringUtils.isNumeric(input[1])) return;
                                int itemId = Integer.parseInt(input[1]);

                                if (!StringUtils.isNumeric(input[2])) return;
                                int quantity = Integer.parseInt(input[2]);

                                BaseItem item = switch (category) {
                                    case 3 -> ItemManager.getInstance().getEquipment(itemId);
                                    case 4 -> ItemManager.getInstance().getPotion(itemId);
                                    case 7 -> ItemManager.getInstance().getMaterial(itemId);
                                    default -> null;
                                };

                                if (item == null) {
                                    NetworkService.gI().sendToast(px, "Item tidak ditemukan");
                                    return;
                                }
                                if (category > 3) {
                                    px.getInventoryManager().addToBag(item, quantity);
                                } else {
                                    px.getInventoryManager().addToBag(item);
                                }

                                px.getInventoryManager().updateInventory();

                                NetworkService.gI().sendToast(px, String.format("%s telah ditambahkan", item.getName()));
                            })

                            .build();

                    p.openInput(inputDialog);
                })
                .menu("Reload Database", player -> DatabaseLoader.getInstance().reloadAll(() -> {
                    WorldManager.getInstance().worldBroadcast(pl -> WorldManager.getInstance().changeMap(pl, pl.getPosition()));
                }))

                .menu("Back", "Tutup", PlayerEntity::navigateToParent)
                .build();
    }

    public static List<Menu> createOtherSub(int npcId, PlayerEntity player) {
        return MenuHelper.forNpc(npcId)

                .submenu("Lepas Fashion", createUnequipFashionMenu(player))
                .submenu("Hide Visual", createHideVisual(player))
                .submenu("Show Visual", createShowVisual(player))
                .menu("Fix Position", p -> {
                    p.setTeleport(true);
                    WorldManager.getInstance().changeMap(p, p.getMap().getNearestWarpPoint(p.getPosition()));
                })
                .menu("BTF MAP", p -> {
                    p.teleport(GUA_API);
                })
                .menu("Clear Inventory", p -> p.getInventoryManager().clearInventory())
                .menu("Back", "Kembali", PlayerEntity::navigateToParent)
                .build();
    }

    private static List<Menu> createShowVisual(PlayerEntity player) {

        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-100);

        PlayerEquipment equipment = player.getInventoryManager().getWearing();
        PartSettings settings = player.getPartSettings();
        if (equipment.hasEquipped(EquipType.FASHION_CLOAK) && !settings.isShowCloak()) {
            helper.item("CLOAK",
                    p -> {
                        player.getPartSettings().setShowCloak(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_WING) && !settings.isShowWing()) {
            helper.item("WING (FASHION)",
                    p -> {
                        player.getPartSettings().setShowWing(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_HAIR) && !settings.isShowHair()) {
            helper.item("HAIR (FASHION)",
                    p -> {
                        player.getPartSettings().setShowHair(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_WEAPON) && !settings.isShowWeapon()) {
            helper.item("WEAPON (FASHION)",
                    p -> {
                        player.getPartSettings().setShowWeapon(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_TITLE) && !settings.isShowTitle()) {
            helper.item("TITLE",
                    p -> {
                        player.getPartSettings().setShowTitle(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_MASK) && !settings.isShowMask()) {
            helper.item("MASK",
                    p -> {
                        player.getPartSettings().setShowMask(true);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        helper.item("Back", "Kembali", MenuManager::navigateBack);

        return helper.build();

    }

    private static List<Menu> createHideVisual(PlayerEntity player) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-100);

        int[] parts = player.getFashion();
        for (int index = 0; index < parts.length; index++) {
            if (parts[index] == -1 || parts[index] == -2) continue;

            PartType type = PartType.fromId(index);
            if (type == null) continue;


            helper.item(
                    type.name(),
                    Map.of("index", index),
                    (p, args) -> {

                        int idx = (Integer) args.get("index");

                        int[] partSettings = p.getPartSettings().getFashion();

                        PartType typePart = PartType.fromId(idx);
                        if (typePart == PartType.HAT && p.getHairId() == -1) {
                            partSettings[PartType.HAIR.getId()] = -1;
                        }

                        partSettings[idx] = partSettings[idx] == -3 ? -1 : -3;
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );


        }
        PartSettings settings = player.getPartSettings();
        PlayerEquipment equipment = player.getInventoryManager().getWearing();
        if (equipment.hasEquipped(FASHION_CLOAK) && settings.isShowCloak()) {
            helper.item("CLOAK",
                    p -> {
                        player.getPartSettings().setShowCloak(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);
                    }
            );
        }
        if (equipment.hasEquipped(FASHION_WING) && settings.isShowWing()) {
            helper.item("WING (FASHION)",
                    p -> {
                        player.getPartSettings().setShowWing(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_HAIR) && settings.isShowHair()) {
            helper.item("HAIR (FASHION)",
                    p -> {
                        player.getPartSettings().setShowHair(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_WEAPON) && settings.isShowWeapon()) {
            helper.item("WEAPON (FASHION)",
                    p -> {
                        player.getPartSettings().setShowWeapon(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_TITLE) && settings.isShowTitle()) {
            helper.item("TITLE",
                    p -> {
                        player.getPartSettings().setShowTitle(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        if (equipment.hasEquipped(FASHION_MASK) && settings.isShowMask()) {
            helper.item("MASK",
                    p -> {
                        player.getPartSettings().setShowMask(false);
                        p.getInventoryManager().broadcastWearing();
                        NetworkService.gI().sendMainCharInfo(p);

                    }
            );
        }
        helper.item("Reset",
                p -> {
                    p.getPartSettings().reset();
                    p.getInventoryManager().broadcastWearing();
                    NetworkService.gI().sendMainCharInfo(p);

                }
        );
        helper.item("Back", "Kembali", MenuManager::navigateBack);

        return helper.build();
    }

    public static List<Menu> createUnequipFashionMenu(PlayerEntity player) {
        MenuHelper.SubMenuBuilder helper = MenuHelper
                .SubMenuBuilder
                .create(-100);

        List<EquipmentSlot> slots = player.getInventoryManager()
                .getWearing().allEquippedExcept(
                        EnumSet.of(
                                EquipType.WEAPON,
                                EquipType.ARMOR,
                                EquipType.LEG,
                                EquipType.HELMET,
                                EquipType.BOOTS,
                                EquipType.RING_1,
                                EquipType.RING_2,
                                EquipType.NECKLACE,
                                EquipType.GLOVE));

        for (EquipmentSlot slot : slots) {
            EquipmentItem item = slot.getItem();
            if (item == null) {
                continue;
            }

            helper.item(
                    item.getName(),
                    Map.of("item", item, "index", slot.getSlotIndex()),
                    (p, args) -> {

                        EquipmentItem eq = (EquipmentItem) args.get("item");
                        int index = (int) args.get("index");
                        if (p.getInventoryManager().unequipItem(index)) {
                            p.recalculateStats();
                            p.getInventoryManager().broadcastWearing();
                            p.getInventoryManager().updateInventory();
                            NetworkService.gI().sendMainCharInfo(p);
                            NetworkService.gI().sendToast(p, String.format("%s berhasil dilepas", eq.getName()));
                        }
                    }
            );
        }
        helper.item("Back", "Kembali", MenuManager::navigateBack);

        return helper.build();
    }

    // ==================== NPC WIZARD ====================

    public static void openWizardMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(WIZARD.getId())
                .title("Kakek Penyihir")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(WIZARD.getId())

                        .menu("Shop Material", p -> {
                            ShopService.getInstance().sendShop(p, WIZARD, 1);
                        })
                        .menu("Upgrade Equipment", p -> {
                            ShopService.getInstance().opeCraftingDialog(p, 0);
                        })
                        .menu("Create Medal", p -> {
                            ShopService.getInstance().opeCraftingDialog(p, 1);
                        }).menu("Upgrade Medal", p -> {
                            ShopService.getInstance().opeCraftingDialog(p, 2);
                        })
                        .menu("Close", "Close", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }


    public static void openDoubarMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(DOUBAR.getId())
                .title("Doubar")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(DOUBAR.getId())
                        .menu("Armor Warrior", p -> {
                            ShopService.getInstance().sendShop(p, DOUBAR, 0);
                        })
                        .menu("Armor Assasin", p -> {
                            ShopService.getInstance().sendShop(p, DOUBAR, 1);
                        })
                        .menu("Armor Penyihir", p -> {
                            ShopService.getInstance().sendShop(p, DOUBAR, 2);
                        })
                        .menu("Armor Penembak", p -> {
                            ShopService.getInstance().sendShop(p, DOUBAR, 3);
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openHammerMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(HAMMER.getId())
                .title("Hammer")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(HAMMER.getId())
                        .menu("Senjata Warrior", p -> {
                            ShopService.getInstance().sendShop(p, HAMMER, 0);
                        })
                        .menu("Senjata Assasin", p -> {
                            ShopService.getInstance().sendShop(p, HAMMER, 1);
                        })
                        .menu("Senjata Penyihir", p -> {
                            ShopService.getInstance().sendShop(p, HAMMER, 2);
                        })
                        .menu("Senjata Penembak", p -> {
                            ShopService.getInstance().sendShop(p, HAMMER, 3);
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openAnnaMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(ANNA.getId())
                .title("Miss Anna")
                .name("")
                .menus(MenuHelper.forNpc(ANNA.getId())
                        .menu("Gift Code", p -> {
                            InputDialog input = InputDialog.builder()
                                    .title("Gift Code")
                                    .fields(List.of("Enter Code"))
                                    .npcId(ANNA.getId())
                                    .action((pl, strings) -> {
                                        String code = strings[0];
                                        pl.sendMessageDialog(LanguageManager.getInstance().get(CODE_NOT_FOUND, player.getLanguage()));
                                    })
                                    .build();

                            p.openInput(input);
                        })
                        .menu("Close", LanguageManager.getInstance().get(CLOSE, player.getLanguage()), PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openAmanMenu(PlayerEntity player) {
        LanguageManager lang = LanguageManager.getInstance();

        MenuHelper helper = MenuHelper.forNpc(AMAN.getId());

        helper.menu(lang.get(OPEN_STORAGE, player.getLanguage()), p -> {
            p.getInventoryManager().openPlayerStorage();
            ShopService.getInstance().sendShop(p, AMAN);
        });

        if (player.getSession().getAccount().getUser().contains("user_")) {
            helper.menu(lang.get(BIND_ACCOUNT, player.getLanguage()), p -> {

                InputDialog input = InputDialog.builder()
                        .title(lang.get(OPEN_STORAGE, player.getLanguage()))
                        .fields(List.of(lang.get(EMAIL, player.getLanguage()), lang.get(PASSWORD, player.getLanguage())))
                        .npcId(AMAN.getId())
                        .action((px, strings) -> {
                            String email = strings[0];
                            String pass = strings[1];

                            if (!ValidationUtils.isValidEmail(email)) {
                                px.sendMessageDialog(lang.get(INVALID_EMAIL, player.getLanguage()));
                                return;
                            }
                            if (!ValidationUtils.isValidLength(pass, 3, 20)) {
                                px.sendMessageDialog(lang.get(INVALID_LENGTH, player.getLanguage(), "Password", 3));
                                return;
                            }

                            if (AccountService.gI().exists(email)) {
                                px.sendMessageDialog(lang.get(EMAIL_USED, player.getLanguage()));
                                return;
                            }

                            Account account = px.getSession().getAccount();
                            account.setUser(email);
                            account.setPass(CryptoUtils.md5(pass));
                            AccountService.gI().save(account);

                            NetworkService.gI().sendSaveLogin(px.getSession(), email, pass);
                            px.sendMessageDialog(lang.get(BIND_SUCCESS, player.getLanguage()));
                        })
                        .build();

                p.openInput(input);
            });


        } else {
            helper.menu(lang.get(CHANGE_PASS, player.getLanguage()), p -> {

                InputDialog input = InputDialog.builder()
                        .title(lang.get(CHANGE_PASS, player.getLanguage()))
                        .fields(List.of(lang.get(OLD_PASSWORD, player.getLanguage()), lang.get(NEW_PASSWORD, player.getLanguage())))
                        .npcId(AMAN.getId())
                        .action((px, strings) -> {
                            String oldPass = strings[0];
                            String newPass = strings[1];

                            if (!CryptoUtils.verifyMD5(oldPass, p.getSession().getAccount().getPass())) {
                                px.sendMessageDialog(lang.get(PASSWORD_NOT_MATCH, player.getLanguage()));
                                return;
                            }

                            if (!ValidationUtils.isValidLength(newPass, 3, 20)) {
                                px.sendMessageDialog(lang.get(INVALID_LENGTH, player.getLanguage(), "Password", 3));
                                return;
                            }

                            Account account = px.getSession().getAccount();
                            account.setPass(CryptoUtils.md5(newPass));
                            AccountService.gI().save(account);

                            NetworkService.gI().sendSaveLogin(px.getSession(), p.getSession().getAccount().getUser(), newPass);
                            px.sendMessageDialog(lang.get(CHANGE_PASS_SUCCESS, player.getLanguage()));
                        })
                        .build();

                p.openInput(input);
            });
        }
        helper.menu("Close", lang.get(CLOSE, player.getLanguage()), PlayerEntity::closeMenuDialog);

        Menu teleportMenu = Menu.builder()
                .npc(AMAN.getId())
                .title("Aman")
                .name("Where would you like to go?")
                .menus(helper.build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openZoroMenu(PlayerEntity player) {
        MenuHelper helper = MenuHelper.forNpc(ZORO.getId());
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild == null) {
            helper.menu("Daftar Guild", p -> {
                InputDialog input = InputDialog.builder()
                        .title("Pendaftaran Guild")
                        .fields(List.of("Nama Guild", "Singkatan"))
                        .npcId(ZORO.getId())
                        .action((pl, strings) -> {
                            String name = strings[0];
                            String alias = strings[1];
                            if (!ValidationUtils.isValidLength(name, 4, 15)) {
                                pl.sendMessageDialog("Nama guild harus 4 - 15 karakter");
                                return;
                            }

                            if (!ValidationUtils.isValidLength(alias, 3, 3)) {
                                pl.sendMessageDialog("Nama alias harus terdiri dari 3 karakter");
                                return;
                            }

                            ConfirmDialog confirmDialog = ConfirmDialog
                                    .builder()
                                    .id(ZORO.getId())
                                    .text("Biaya pembuatan guild 500 permata, lanjutkan?")
                                    .args(Map.of("name", name, "alias", alias))
                                    .onRespond((pd, yOrN, args) -> {
                                        if (yOrN) {
                                            String nm = (String) args.get("name");
                                            String als = (String) args.get("alias");

                                            if (!pd.spendGem(500)) {
                                                pd.sendMessageDialog("Permata tidak cukup");
                                                return;
                                            }
                                            GuildResult result = GuildManager.getInstance().createGuild(pd, nm, als);
                                            pd.sendMessageDialog(result.getMessage());

                                            pd.getZone().broadcast(player1 -> NetworkService.gI().sendCharInfo(player1, pl));
                                            NetworkService.gI().sendMainCharInfo(pd);
                                        }
                                    }).build();


                            pl.openConfirmDialog(confirmDialog);

                        })
                        .build();

                p.openInput(input);
            });
        } else {
            if (guild.isLeader(player)) {
                helper.menu("Guild Icon", px -> {
                    ShopService.getInstance().sendShop(px, ZORO);
                });
            }

            helper.menu("Guild Storage", px -> {
                px.getInventoryManager().openGuildStorage(guild.getInventory());
                ShopService.getInstance().sendShop(px, AMAN);

            });
            helper.menu("Donasi Gold", px -> {
                InputDialog input = InputDialog.builder()
                        .title("Donasi")
                        .fields(List.of("Jumlah Gold"))
                        .npcId(ZORO.getId())
                        .action((pl, strings) -> {
                            try {
                                long gold = Long.parseLong(strings[0]);

                                if (!pl.spendGold(gold)) {
                                    pl.sendMessageDialog("Gold tidak cukup");
                                    return;
                                }
                                GuildMember member = guild.getMember(pl.getId());
                                if (member == null) return;

                                int cp = guild.donateGold(member, gold);

                                pl.getInventoryManager().updateInventory();
                                pl.sendMessageDialog(String.format("%d gold telah di donasikan, kamu menerima %d point kontribusi", gold, cp));
                            } catch (Exception e) {
                                pl.sendMessageDialog("Angka tidak valid");
                            }

                        })
                        .build();

                px.openInput(input);
            });
            helper.menu("Donasi Permata", px -> {
                InputDialog input = InputDialog.builder()
                        .title("Donasi")
                        .fields(List.of("Jumlah Permata"))
                        .npcId(ZORO.getId())
                        .action((pl, strings) -> {
                            try {
                                int amt = Integer.parseInt(strings[0]);

                                if (!pl.spendGem(amt)) {
                                    pl.sendMessageDialog("Permata tidak cukup");
                                    return;
                                }
                                GuildMember member = guild.getMember(pl.getId());
                                if (member == null) return;

                                int cp = guild.donateGem(member, amt);

                                pl.getInventoryManager().updateInventory();
                                pl.sendMessageDialog(String.format("%d permata telah di donasikan, kamu menerima %d point kontribusi", amt, cp));
                            } catch (Exception e) {
                                pl.sendMessageDialog("Angka tidak valid");
                            }

                        })
                        .build();

                px.openInput(input);
            });

            helper.menu("Informasi", px -> {
                GuildMember member = guild.getMember(px.getId());
                if (member == null)
                    return;

                px.sendMessageDialog(String.format(
                        """
                                
                                CP: %s
                                Keterangan:
                                - CP dapat diperoleh dari donasi guild:
                                  • 10.000 Gold = 1 CP
                                  • 1 Permata   = 1 CP
                                - Equipment dihitung berdasarkan:
                                  • Warna
                                  • Level
                                  • Plus
                                - CP digunakan untuk mengambil item guild.
                                """,

                        member.getContributionPoints()
                ));


            });
        }
        helper.menu("Close", "Tutup", PlayerEntity::closeMenuDialog);

        Menu root = Menu.builder()
                .npc(ZORO.getId())
                .title("Zoro")
                .name("Where would you like to go?")
                .menus(helper.build()).build();

        MenuManager.openMenu(player, root);
    }

    public static void openAlisamaMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(ALISAMA.getId())
                .title("Alisama")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(ALISAMA.getId())
                        .menu("Armor Warrior", p -> {
                            ShopService.getInstance().sendShop(p, ALISAMA, 0);
                        })
                        .menu("Armor Assasin", p -> {
                            ShopService.getInstance().sendShop(p, ALISAMA, 1);
                        })
                        .menu("Armor Penyihir", p -> {
                            ShopService.getInstance().sendShop(p, ALISAMA, 2);
                        })
                        .menu("Armor Penembak", p -> {
                            ShopService.getInstance().sendShop(p, ALISAMA, 3);
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openBlackEyeMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(BLACK_EYE.getId())
                .title("Black Eye")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(BLACK_EYE.getId())
                        .menu("Senjata Warrior", p -> {
                            ShopService.getInstance().sendShop(p, BLACK_EYE, 0);
                        })
                        .menu("Senjata Assasin", p -> {
                            ShopService.getInstance().sendShop(p, BLACK_EYE, 1);
                        })
                        .menu("Senjata Penyihir", p -> {
                            ShopService.getInstance().sendShop(p, BLACK_EYE, 2);
                        })
                        .menu("Senjata Penembak", p -> {
                            ShopService.getInstance().sendShop(p, BLACK_EYE, 3);
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openBallardMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(BALLARD.getId())
                .title("Medan Perang")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(BALLARD.getId())
                        .menu("Pendaftaran", p -> {
                            BTF btf = EventManager.getInstance().getEvent(BTF.class);
                            if (btf == null) {
                                p.sendMessageDialog("Event belum dimulai");
                                return;
                            }

                            btf.registerPlayer(p);

                        })
                        .menu("Masuk", p -> {
                            BTF btf = EventManager.getInstance().getEvent(BTF.class);
                            if (btf == null) {
                                p.sendMessageDialog("Event belum dimulai");
                                return;
                            }

                            if (btf.getState() != BTFState.FIGHTING) {
                                p.sendMessageDialog("Battle belum dimulai atau telah berakhir");
                                return;
                            }

                            Team team = btf.getPlayerTeam(p.getId());
                            if (team == null) {
                                p.sendMessageDialog("Kamu tidak terdaftar dalam team");
                                return;
                            }

                            if (team.isDestroyed()) {
                                p.sendMessageDialog("Team sudah ter eliminasi");
                                return;
                            }

                            if (btf.getState() == BTFState.FIGHTING) {
                                p.teleport(team.getLocation());
                            }

                        })
                        .menu("Tukar Point", p -> {
                            ShopService.getInstance().sendShop(p, BALLARD, 2);
                        })
                        .menu("Panduan", p -> {
                            ShopService.getInstance().sendShop(p, BALLARD, 2);
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openCacaoMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(CAOCAO.getId())
                .title("Liu Bei")
                .name("")
                .menus(MenuHelper.forNpc(CAOCAO.getId())
                        .menu("Fashion Shop", p -> {
                            ShopService.getInstance().sendShop(p, CAOCAO, 0);
                        })
                        .menu("Hair Shop", p -> {
                            ShopService.getInstance().sendShop(p, CAOCAO, 1);
                        })
                        .menu("Close", LanguageManager.getInstance().get(CLOSE, player.getLanguage()), PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openPetManagerMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(PET_MANAGER.getId())
                .title("PetManager")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(PET_MANAGER.getId())
                        .menu("Pet Container", p -> ShopService.getInstance().sendShop(p, PET_MANAGER, 0))
                        .menu("Makanan Pet", p -> ShopService.getInstance().sendShop(p, PET_MANAGER, 1))
                        .menu("Toko Telur", p -> ShopService.getInstance().sendShop(p, PET_MANAGER, 2))
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openZuluMenu(PlayerEntity player) {
        Menu teleportMenu = Menu.builder()
                .npc(ZULU.getId())
                .title("Zulu")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(ZULU.getId())
                        .menu("Absen Harian", p -> {
                            DailyAbsent absent = EventManager.getInstance().getEvent(DailyAbsent.class);
                            if (absent == null) return;

                            if (!absent.claim(p)) {
                                p.sendMessageDialog("Kamu sudah menerima hadiah datang lagi besok");
                            }
                        })
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        MenuManager.openMenu(player, teleportMenu);
    }

    public static void openRankMenu(PlayerEntity player) {
        Menu root = Menu.builder()
                .npc(RANKING.getId())
                .title("Zulu")
                .name("Where would you like to go?")
                .menus(MenuHelper.forNpc(RANKING.getId())
                        .menu("Top Level", p -> NetworkService.gI().sendTopLevel(p))
                        .menu("Top Guild", p -> NetworkService.gI().sendTopGuild(p))
                        .menu("Close", "Tutup", PlayerEntity::closeMenuDialog)
                        .build())
                .build();

        openMenu(player, root);
    }
}