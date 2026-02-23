package handler;

import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import game.inventory.InventorySlot;
import lombok.extern.slf4j.Slf4j;
import manager.ConfigManager;
import manager.ItemManager;
import model.item.EquipmentItem;
import model.item.ItemCategory;
import network.Message;
import network.Session;
import service.ItemService;
import service.NetworkService;
import service.ShopService;
import service.UpgradeService;
import utils.NumberUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static game.equipment.EquipType.PET;
import static handler.Command.*;

@Slf4j
public class ItemHandler {

    public static void onMessage(Session s, Message m) throws IOException {
        switch (m.command) {
            case USE_ITEM -> onUseItem(s, m);
            case USE_POTION -> onUsePotion(s, m);
            case USE_MOUNT -> onUseMount(s, m);
            case DELETE_ITEM -> onDeleteItem(s, m);
            case BUY_ITEM -> onBuyItem(s, m);
            case BUY_SELL -> onSellItem(s, m);
            case REBUILD_ITEM -> onRebuildItem(s, m);
            case HOP_RAC -> onMedalCrafting(s, m);
        }
    }

    private static void onMedalCrafting(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;


        byte type = m.in().readByte();
        short id = -1;
        byte tem = -1;
        if (m.in().available() > 0) {
            id = m.in().readShort();
            tem = m.in().readByte();
        }

        log.debug("TYPE {}, ID {},CATEGORY {}", type, id, tem);
        switch (type) {
            // MEDAL FUSSION
            case 0 -> {
                InventorySlot slot = p.getInventoryManager().getSlot(id);
                if (slot == null || slot.getItem() == null) {
                    log.info("bagSlot: {} is empty", id);
                    return;
                }
                EquipmentItem item = (EquipmentItem) slot.getItem();

                boolean isMedal = item.getId() >= 4587 && item.getId() <= 4590;
                if (!isMedal) {
                    p.sendMessageDialog("This item doesn't support!");
                    return;
                }

                if (item.getPlus() >= 15) {
                    p.sendMessageDialog("This item has reached max level!");
                    return;
                }

                ItemService.getInstance().sendUpgradeMaterialMedal(p);
            }
            // MEDAL CREATE
            case 3 -> {
                List<Short> materials = ConfigManager.getInstance()
                        .getSvConfig()
                        .getMaterialMedal()
                        .get(p.getRole());

                if (materials == null || materials.isEmpty()) {
                    p.sendMessageDialog("No material configuration found");
                    return;
                }

                boolean hasAllMaterials = true;
                // Checking requirement material from Bag
                for (short materialId : materials) {
                    var optionalSlot = p.getInventoryManager()
                            .getInventory()
                            .findSlot(materialId, ItemCategory.MATERIAL);

                    if (optionalSlot.isEmpty() || optionalSlot.get().getAmount() < 1) {
                        hasAllMaterials = false;
                        break;
                    }
                }

                if (!hasAllMaterials) {
                    p.sendMessageDialog("Not enough material");
                    if (s.getAccount().getRole() > 0) {
                        for (short materialId : materials) {
                            var materialItem = ItemManager.getInstance().getMaterial(materialId);
                            p.getInventoryManager().addToBag(materialItem, 100);
                        }
                        p.getInventoryManager().updateInventory();
                    }
                    return;
                }

                // deduce requirement material from Bag
                for (short materialId : materials) {
                    var optionalSlot = p.getInventoryManager()
                            .getInventory()
                            .findSlot(materialId, ItemCategory.MATERIAL);

                    if (optionalSlot.isEmpty()) continue;

                    InventorySlot slot = optionalSlot.get();
                    slot.decrease();
                }


                EquipmentItem equip = switch (p.getRole()) {
                    case 0 -> ItemManager.getInstance().getEquipment(4587);
                    case 1 -> ItemManager.getInstance().getEquipment(4589);
                    case 2 -> ItemManager.getInstance().getEquipment(4588);
                    default -> ItemManager.getInstance().getEquipment(4590);
                };

                int percent = NumberUtils.percent();

                if (percent <= 10) {
                    equip.setColor(4);
                } else if (percent <= 20) {
                    equip.setColor(3);
                } else if (percent <= 30) {
                    equip.setColor(2);
                } else if (percent <= 70) {
                    equip.setColor(1);        // 40%
                } else {
                    equip.setColor(0);        // 30%
                }

                equip.setLock(false);
                ItemService.getInstance().sendCreateMedal(p, equip);
                p.getInventoryManager().addToBag(equip, 1);
                p.getInventoryManager().updateInventory();

            }
            // UPGRADE MEDAL
            case 4 -> {
                InventorySlot slot = p.getInventoryManager().getSlot(id);
                if (slot == null || slot.getItem() == null) {
//                    log.info("bagSlot: {} is empty", id);
                    p.sendMessageDialog("Please reselect the item");
                    return;
                }
                EquipmentItem item = (EquipmentItem) slot.getItem();

                boolean isMedal = item.getId() >= 4587 && item.getId() <= 4590;
                if (!isMedal) {
                    p.sendMessageDialog("This item doesn't support!");
                    return;
                }

                List<Short> materials = ConfigManager.getInstance()
                        .getSvConfig()
                        .getMaterialMedal()
                        .get(p.getRole());

                if (materials == null || materials.isEmpty()) {
                    p.sendMessageDialog("No material configuration found");
                    return;
                }

                boolean hasAllMaterials = true;
                // Checking requirement material from Bag
                for (short materialId : materials) {
                    var optionalSlot = p.getInventoryManager()
                            .getInventory()
                            .findSlot(materialId, ItemCategory.MATERIAL);

                    if (optionalSlot.isEmpty() || optionalSlot.get().getAmount() < 2) {
                        hasAllMaterials = false;
                        break;
                    }
                }

                if (!hasAllMaterials) {
                    p.sendMessageDialog("Not enough material");
                    return;
                }

                // deduce requirement material from Bag
                for (short materialId : materials) {
                    var optionalSlot = p.getInventoryManager()
                            .getInventory()
                            .findSlot(materialId, ItemCategory.MATERIAL);

                    if (optionalSlot.isEmpty()) continue;

                    InventorySlot bagSlot = optionalSlot.get();
                    bagSlot.decrease(2);
                }

                UpgradeService service = UpgradeService.getInstance();
                service.sendUpgradeMedalResult(p, service.tryUpgrade(item, 0, item.getPlus() == 6 || item.getPlus() == 10), item);

                if (item.getPlus() < 15) {
                    ItemService.getInstance().sendUpgradeMaterialMedal(p);
                }

                p.getInventoryManager().updateInventory();
            }
        }
    }


    private static void onUseMount(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        if (p.getInventoryManager().unequipItem(EquipType.FASHION_MOUNT)) {
            p.recalculateStats();
            p.getInventoryManager().updateInventory();
            p.getInventoryManager().broadcastWearing();
            NetworkService.gI().sendMainCharInfo(p);
        }
    }

    private static void onRebuildItem(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null)
            return;

        byte type = m.in().readByte();
        int bagSlot = m.in().readShort();
        byte category = m.in().readByte(); // type item insert


        log.info("type {} id {} category {}", type, bagSlot, category);


        UpgradeService service = UpgradeService.getInstance();
        switch (type) {
            // SELECT ITEM
            case 0 -> {

                InventorySlot slot = p.getInventoryManager().getSlot(bagSlot);
                if (slot == null || slot.getItem() == null) {
                    log.info("bagSlot: {} is empty", bagSlot);
                    return;
                }
                EquipmentItem item = (EquipmentItem) slot.getItem();

                if (!service.canUpgrade(item)) {
                    p.sendMessageDialog("This item doesn't support!");
                    return;
                }
                p.setCurrentItemUpgrade(bagSlot);
                log.info("Select {}", item.getName());
                int chance = service.getBaseRate(item);
                service.sendSelectItem(p, bagSlot, String.format("%d%%", chance));

            }

            // Upgrade
            case 2 -> {
                long now = System.currentTimeMillis();
                if (now - p.getLastUpgradeTime() < UpgradeService.UPGRADE_COOLDOWN) {
                    p.sendMessageDialog("Please slow down");
                    return; // or send error packet
                }

                InventorySlot slot = p.getInventoryManager().getSlot(p.getCurrentItemUpgrade());
                if (slot == null || slot.getItem() == null) {
                    log.info("bagSlot: {} is empty", bagSlot);
                    return;
                }
                EquipmentItem item = (EquipmentItem) slot.getItem();

                if (p.getSession().getAccount().getRole() > 0) {
                    item.setPlus((byte) (item.getPlus() + (UpgradeService.MAX_LEVEL - item.getPlus())));
                    service.sendUpgradeResult(p, true, String.format("You have successfully upgrade %s into +%d", item.getName(), item.getPlus()));
                } else {
                    if (service.tryUpgrade(item, 0, item.getPlus() == 6 || item.getPlus() == 10)) {
                        service.sendUpgradeResult(p, true, String.format("Congratulations! You have successfully upgraded %s into +%d", item.getName(), item.getPlus()));
                    } else {
                        service.sendUpgradeResult(p, true, String.format("“Failed to upgrade %s to %d.", item.getName(), item.getPlus() + 1));
                    }
                }

                p.setLastUpgradeTime(now);
                p.getInventoryManager().updateInventory();
                service.sendClear(p);
                if (service.canUpgrade(item)) {
                    service.sendSelectItem(p, p.getCurrentItemUpgrade(), String.format("%d%%", service.getBaseRate(item)));
                }
            }
        }


    }

    private static void onSellItem(Session s, Message m) {
    }

    private static void onBuyItem(Session s, Message m) throws IOException {
        PlayerEntity e = s.getPlayer();
        if (e == null) return;

        ShopService.getInstance().buyItem(e, m);
    }

    private static void onDeleteItem(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null)
            return;


        byte category = m.in().readByte();
        short id = m.in().readShort();
        byte type = m.in().readByte();

        switch (category) {
            case 3 -> {
                p.getInventoryManager().remove(id);
                if (type == 1) {
                    p.addGold(300);
                }
            }

            case 4 -> {
                Optional<InventorySlot> potion = p.getInventoryManager().getInventory().findSlot(id, ItemCategory.POTION);
                if (potion.isPresent()) {
                    InventorySlot slot = potion.get();
                    if (type == 1) {
                        p.addGold(5L * slot.getAmount());
                    }
                    slot.clear();
                }


            }
            case 7 -> {
                Optional<InventorySlot> material = p.getInventoryManager().getInventory().findSlot(id, ItemCategory.MATERIAL);
                if (material.isPresent()) {
                    InventorySlot slot = material.get();
                    if (type == 1) {
                        p.addGold(5L * slot.getAmount());
                    }
                    slot.clear();
                }

            }
        }
        p.getInventoryManager().updateInventory();
    }

    private static void onUseItem(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) {
            return;
        }

        byte bagSlot = m.in().readByte();
        byte equipSlot = m.in().readByte();

        InventorySlot slot = p.getInventoryManager().getSlot(bagSlot);
        if (slot == null || slot.getItem() == null) {
            NetworkService.gI().sendToast(p, "Item not found");
            return;
        }


        EquipmentItem item = (EquipmentItem) slot.getItem();
        if (item.getEquipmentType() == null) {
            NetworkService.gI().sendToast(p, "Invalid item");
            return;
        }

        if (item.getEquipmentType() == PET) {
            NetworkService.gI().sendToast(p, "Bring to Pet Manager");
            return;
        }

        if (p.getLevel() < item.getLevel()) {
            NetworkService.gI().sendNoticeBox(s, "Level is not enough");
            return;
        }

        if (item.getRole() == p.getRole() || item.getRole() == 4) {
            if (equipSlot == 3 || equipSlot == 9) {
                if (p.getInventoryManager().equipItem(bagSlot, equipSlot)) {
                    p.recalculateStats();
                    p.getInventoryManager().broadcastWearing();
                    p.getInventoryManager().updateInventory();
                    NetworkService.gI().sendMainCharInfo(p);
                }
            } else {
                if (p.getInventoryManager().equipItem(bagSlot)) {
                    p.recalculateStats();
                    p.getInventoryManager().updateInventory();
                    p.getInventoryManager().broadcastWearing();
                    if (EquipType.fromValue(bagSlot) == EquipType.FASHION_MOUNT) {
                        p.getZone().broadcast(player -> NetworkService.gI().sendUseMount(player, p));
                    }
                    NetworkService.gI().sendMainCharInfo(p);

                }
            }
        } else {
            NetworkService.gI().sendNoticeBox(s, "Invalid class");
        }
    }

    private static void onUsePotion(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        int itemId = m.in().readShort();

        p.getInventoryManager().usePotion(itemId);
    }
}
