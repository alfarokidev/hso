package game.inventory;

import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import game.equipment.PlayerEquipment;
import game.guild.Guild;
import game.guild.GuildManager;
import game.guild.GuildMember;
import game.stat.StatCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.item.*;
import network.Message;
import service.InventoryService;
import service.NetworkService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class InventoryManager {
    private final PlayerEntity player;

    @Getter
    private PlayerEquipment wearing;

    @Getter
    private PlayerInventory inventory;

    @Getter
    private PlayerInventory storage;

    @Getter
    private BaseInventory activeStorage;


    public InventoryManager(PlayerEntity player) {
        this.player = player;
        load();
    }

    private void load() {
        // Load equipment
        PlayerEquipment equipment = InventoryService.gI().findEquipmentById(player.getId());
        if (equipment == null) {
            wearing = new PlayerEquipment();
            wearing.setPlayerId(player.getId());
            log.info("Load equipment failed, creating new");
        } else {
            wearing = equipment;
        }

        // Load inventory
        PlayerInventory bag = InventoryService.gI().findInventoryById(player.getId());
        if (bag == null) {
            inventory = new PlayerInventory(126);
            inventory.setPlayerId(player.getId());
            log.info("Load inventory failed");
        } else {
            inventory = bag;
        }

        // Load storage
        PlayerInventory box = InventoryService.gI().findStorageById(player.getId());
        if (box == null) {
            storage = new PlayerInventory(126);
            storage.setPlayerId(player.getId());
            log.info("Load storage failed");
        } else {
            storage = box;
        }

        activeStorage = storage;
    }


    public void openPlayerStorage() {
        activeStorage = storage;
    }

    public void openGuildStorage(GuildInventory guildInv) {
        this.activeStorage = guildInv;
    }

    // ==================== EQUIPMENT OPERATIONS ====================

    public boolean equipItem(int bagSlotIndex) {
        InventorySlot slot = inventory.slot(bagSlotIndex);
        if (slot == null || slot.isEmpty()) return false;

        BaseItem base = slot.getItem();
        if (!(base instanceof EquipmentItem item)) return false;

        EquipmentItem oldEquip = wearing.equipAndReturn(item);
        inventory.remove(bagSlotIndex);

        if (oldEquip != null) {
            inventory.add(oldEquip, 1);
        }

        return true;
    }

    public boolean equipItem(int bagSlotIndex, int slotIndex) {
        InventorySlot slot = inventory.slot(bagSlotIndex);
        if (slot == null || slot.isEmpty()) return false;

        BaseItem base = slot.getItem();
        if (!(base instanceof EquipmentItem item)) return false;

        EquipmentItem oldEquip = wearing.equipAndReturn(item, slotIndex);
        inventory.remove(bagSlotIndex);

        if (oldEquip != null) {
            inventory.add(oldEquip, 1);
        }

        return true;
    }

    public boolean unequipItem(int equipSlotIndex) {
        if (!hasBagSpace()) return false;

        EquipmentItem item = wearing.unequip(equipSlotIndex);
        if (item == null) return false;

        if (!inventory.add(item, 1)) {
            wearing.equip(equipSlotIndex, item);
            return false;
        }

        return true;
    }

    public boolean unequipItem(EquipType equipType) {
        if (!hasBagSpace()) return false;

        EquipmentItem item = wearing.unequip(equipType);
        if (item == null) return false;

        if (!inventory.add(item, 1)) {
            wearing.equip(item);
            return false;
        }

        return true;
    }


    public void usePotion(int itemId) throws IOException {
        // Find the item with POTION category
        PotionItem potion = (PotionItem) inventory.findSlot(itemId, ItemCategory.POTION)
                .map(InventorySlot::getItem)
                .orElse(null);

        if (potion == null) return;

        potion.onUse(player);

    }

    // ==================== INVENTORY OPERATIONS ====================

    public void addToBag(BaseItem item, int quantity) {
        if (item.getCategory() == ItemCategory.POTION) {
            if (item.getId() == -1) {
                player.addGold(quantity);
            } else if (item.getId() == -2) {
                player.addGem(quantity);
            } else {
                inventory.add(item, quantity);
            }
        } else {
            inventory.add(item, quantity);
        }
    }

    public void addToBag(BaseItem item) {
        inventory.add(item, 1);
    }

    public void addToStorage(BaseItem item, int quantity) {
        activeStorage.add(item, quantity);
    }

    public void remove(int slotIndex) {
        inventory.remove(slotIndex);
    }

    public void removeById(int itemId, int quantity) {
        inventory.removeById(itemId, quantity);
    }

    public void removeStorageSlot(int slotIndex) {
        activeStorage.remove(slotIndex);
    }

    public void removeStorageById(int itemId, int quantity) {
        activeStorage.removeById(itemId, quantity);
    }

    public int countInBag(int itemId) {
        return inventory.count(itemId);
    }

    public int countInStorage(int itemId) {
        return activeStorage.count(itemId);
    }

    public int totalCount(int itemId) {
        return countInBag(itemId) + countInStorage(itemId);
    }

    public boolean hasBagSpace() {
        return inventory.available() > 0;
    }

    public boolean hasStorageSpace() {
        return activeStorage.available() > 0;
    }

    public int getBagSpace() {
        return inventory.available();
    }

    public int getStorageSpace() {
        return activeStorage.available();
    }

    public InventorySlot getSlot(int index) {
        return inventory.slot(index);
    }

    // ==================== TRANSFER BAG <-> BOX ====================


    public boolean moveToBag(int id, int quantity, ItemCategory category) {
        if (!hasBagSpace()) return false;

        if (category == ItemCategory.EQUIPMENT) {
            InventorySlot slot = activeStorage.slot(id);
            if (slot == null || slot.isEmpty()) return false;

            BaseItem item = slot.getItem();
            int removed = activeStorage.remove(id);

            if (removed > 0) {
                if (!inventory.add(item, 1)) {
                    activeStorage.slot(id).set(item, 1);
                    return false;
                }
                return true;
            }

            return false;
        } else {
            Optional<InventorySlot> opt = activeStorage.findSlot(id, category);
            if (opt.isEmpty()) {
                return false;
            }
            InventorySlot slot = opt.get();
            if (quantity <= 0 || slot.getAmount() < quantity) return false;

            BaseItem item = slot.getItem();
            if (item == null) return false;

            int removed = activeStorage.removeById(id, quantity);
            if (removed > 0) {
                if (!inventory.add(item, quantity)) {
                    activeStorage.slot(id).set(item, quantity);
                    return false;
                }
                return true;
            }
        }

        return false;
    }

    public boolean moveToBox(int id, int quantity, ItemCategory category) {
        if (!hasStorageSpace()) return false;

        if (category == ItemCategory.EQUIPMENT) {
            InventorySlot slot = inventory.slot(id);
            if (slot == null || slot.isEmpty()) return false;

            BaseItem item = slot.getItem();
            int removed = inventory.remove(id);

            if (removed > 0) {
                if (!activeStorage.add(item, 1)) {
                    inventory.slot(id).set(item, 1);
                    return false;
                }
                return true;
            }
        } else {

            Optional<InventorySlot> opt = inventory.findSlot(id, category);
            if (opt.isEmpty()) {
                return false;
            }
            InventorySlot slot = opt.get();
            if (quantity <= 0 || slot.getAmount() < quantity) return false;

            BaseItem item = slot.getItem();
            if (item == null) return false;

            int removed = inventory.removeById(id, quantity);
            if (removed > 0) {
                if (!activeStorage.add(item, quantity)) {
                    inventory.slot(id).set(item, quantity);
                    return false;
                }
                return true;
            }

        }

        return false;
    }

    // ==================== NETWORK MESSAGES ====================

    public void broadcastWearing() {
        player.getZone().broadcast(notify -> NetworkService.gI().sendWearing(notify, player));
        player.getZone().broadcast(notify -> NetworkService.gI().sendCharInfo(notify, player));
    }

    public void updateInventory() {
        sendInventoryUpdate(4);
        sendInventoryUpdate(7);
        sendInventoryUpdate(3);
    }

    public void updateStorage() {
        sendStorageUpdate(4);
        sendStorageUpdate(7);
        sendStorageUpdate(3);
    }

    private void sendInventoryUpdate(int type) {
        switch (type) {
            case 3 -> sendEquipmentInventory();
            case 4 -> sendPotionInventory();
            case 7 -> sendMaterialInventory();
        }
    }

    private void sendEquipmentInventory() {
        try {
            Message m = new Message(16);
            m.out().writeByte(0);
            m.out().writeByte(3);
            m.out().writeLong(player.getGold());
            m.out().writeInt((int) player.getGems());
            m.out().writeByte(3);

            m.out().writeByte(inventory.sizeByCategory(ItemCategory.EQUIPMENT));
            for (int i = 0; i < inventory.getCapacity(); i++) {
                InventorySlot slot = inventory.slot(i);
                if (slot == null || slot.isEmpty() || slot.getItem() == null) continue;

                BaseItem base = slot.getItem();
                if (base instanceof EquipmentItem item) {
                    m.out().writeUTF(item.getName());
                    m.out().writeByte(item.getRole());
                    m.out().writeShort(i);
                    m.out().writeByte(item.getType());
                    m.out().writeShort(item.getIcon());
                    m.out().writeByte(item.getPlus());
                    m.out().writeShort(item.getLevel());
                    m.out().writeByte(item.getColor());
                    m.out().writeByte(1);
                    m.out().writeByte(item.isLock() ? 0 : 1);
                    m.out().writeByte(item.getOption().size());
                    for (Option op : item.getOption()) {
                        m.out().writeByte(op.getId());
                        m.out().writeInt(StatCalculator.getBonusPlus(op, item.getPlus()));
                    }

                    if (item.getTimeUse() != 0) {
                        long timeUse = (item.getTimeUse() - System.currentTimeMillis()) / 60_000;
                        m.out().writeInt((int) ((timeUse > 0) ? timeUse : 1));
                    } else {
                        m.out().writeInt(0);
                    }
                    m.out().writeByte(item.isLock() ? (byte) 1 : (byte) 0);
                    if (item.getExpireDate() <= 0) {
                        m.out().writeByte(0);
                    } else {
                        m.out().writeByte(1);
                        m.out().writeInt(0);
                        m.out().writeUTF("" + item.getExpireDate());
                    }
                    m.out().writeByte(0);
                }
            }

            player.send(m);

        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }


    }

    private void sendPotionInventory() {
        sendMaterialTypeInventory(4);
    }

    private void sendMaterialInventory() {
        sendMaterialTypeInventory(7);
    }

    private void sendMaterialTypeInventory(int type) {
        try {
            Message m = new Message(16);
            m.out().writeByte(0);
            m.out().writeByte(type);
            m.out().writeLong(player.getGold());
            m.out().writeInt((int) player.getGems());
            m.out().writeByte(type);

            List<InventorySlot> slots = inventory.itemsByCategory(ItemCategory.fromValue(type));
            m.out().writeByte(slots.size());
            for (InventorySlot slot : slots) {
                m.out().writeShort(slot.getItem().getId());
                m.out().writeShort(slot.getAmount());
                m.out().writeByte(1);
                m.out().writeByte(0);
            }

            player.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendStorageUpdate(int type) {
        switch (type) {
            case 3 -> sendEquipmentStorage();
            case 4 -> sendPotionStorage();
            case 7 -> sendMaterialStorage();
        }
    }

    private void sendEquipmentStorage() {
        try {
            Message m = new Message(65);
            m.out().writeByte(126);
            m.out().writeByte(0);
            m.out().writeByte(3);
            m.out().writeByte(3);

            int count = 0;
            for (int i = 0; i < activeStorage.getCapacity(); i++) {
                InventorySlot slot = activeStorage.slot(i);
                if (slot != null && !slot.isEmpty() && slot.getItem() instanceof EquipmentItem) {
                    count++;
                }
            }
            m.out().writeByte(count);

            for (int i = 0; i < activeStorage.getCapacity(); i++) {
                InventorySlot slot = activeStorage.slot(i);
                if (slot == null || slot.isEmpty()) continue;

                BaseItem base = slot.getItem();
                if (!(base instanceof EquipmentItem item)) continue;

                m.out().writeUTF(item.getName());
                m.out().writeByte(item.getType());
                m.out().writeShort(i);
                m.out().writeByte(item.getPart());
                m.out().writeShort(item.getIcon());
                m.out().writeByte(item.getPlus());
                m.out().writeShort(item.getLevel());
                m.out().writeByte(item.getColor());
                m.out().writeByte(1);
                m.out().writeByte(item.isLock() ? 0 : 1);

                m.out().writeByte(item.getOption().size());
                for (var op : item.getOption()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(StatCalculator.getBonusPlus(op, item.getPlus()));
                }

                if (item.getTimeUse() != 0) {
                    long timeUse = item.getTimeUse() - System.currentTimeMillis();
                    timeUse /= 3_600_000;
                    m.out().writeInt((int) ((timeUse > 0) ? timeUse : 1));
                } else {
                    m.out().writeInt(0);
                }

                m.out().writeByte(item.isLock() ? 1 : 0);
                m.out().writeByte(0);
                m.out().writeByte(0);
            }

            player.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    private void sendPotionStorage() {
        sendMaterialTypeStorage(4);
    }

    private void sendMaterialStorage() {
        sendMaterialTypeStorage(7);
    }

    private void sendMaterialTypeStorage(int type) {
        try {
            Message m = new Message(65);
            m.out().writeByte(126);
            m.out().writeByte(0);
            m.out().writeByte(type);
            m.out().writeByte(type);

            List<InventorySlot> slots = activeStorage.itemsByCategory(ItemCategory.fromValue(type));
            m.out().writeByte(slots.size());
            for (InventorySlot slot : slots) {
                m.out().writeShort(slot.getItem().getId());
                m.out().writeShort(slot.getAmount());
                m.out().writeByte(1);
                m.out().writeByte(0);
            }

            player.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void handleBoxTransfer(Message m) throws IOException {
        byte type = m.in().readByte();
        short id = m.in().readShort();
        ItemCategory category = ItemCategory.fromValue(m.in().readByte());
        short quantity = m.in().readShort();

        if (quantity <= 0) return;

        if (type == -1) {
            sendStorageUpdate(3);
            sendStorageUpdate(4);
            sendStorageUpdate(7);
            return;
        }

        if (category == null) {
            return;
        }

        // SAVE ITEM TO STORAGE
        if (type == 1) {
            if (!hasStorageSpace()) {
                NetworkService.gI().sendNoticeBox(player.getSession(), "Storage penuh!");
                return;
            }

            if (category == ItemCategory.EQUIPMENT) {
                InventorySlot slot = inventory.slot(id);
                if (slot == null || slot.isEmpty()) return;

                BaseItem item = slot.getItem();
                EquipmentItem eq = (EquipmentItem) item;
                if (activeStorage instanceof GuildInventory && eq.isLock()) {
                    player.sendMessageDialog("Kamu tidak bisa mendonasikan item yang terkunci");
                    return;
                }

                if (moveToBox(id, quantity, category)) {
                    if (activeStorage instanceof GuildInventory) {
                        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
                        if (guild == null) {
                            return;
                        }

                        GuildMember member = guild.getMember(player.getId());
                        if (member == null) return;

                        guild.donateItem(member, item, quantity);
                    }

                    sendInventoryUpdate(category.getValue());
                    sendStorageUpdate(category.getValue());
                }

            } else {

                if (moveToBox(id, quantity, category)) {
                    sendInventoryUpdate(category.getValue());
                    sendStorageUpdate(category.getValue());
                }

            }
        } else {
            if (!hasBagSpace()) {
                NetworkService.gI().sendNoticeBox(player.getSession(), "Inventory penuh!");
                return;
            }

            if (category == ItemCategory.EQUIPMENT) {
                InventorySlot slot = activeStorage.slot(id);
                if (slot == null || slot.isEmpty()) return;

                BaseItem item = slot.getItem();

                if (activeStorage instanceof GuildInventory) {
                    Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
                    if (guild == null) {
                        return;
                    }

                    GuildMember member = guild.getMember(player.getId());
                    if (member == null) return;


                    int cost = guild.getItemCost(item);
                    if (!member.spendCP(cost)) {
                        player.sendMessageDialog(String.format("Dibutuhkan %s point kontribusi untuk menerima item ini", cost));
                        return;
                    }
                }

            }

            if (moveToBag(id, quantity, category)) {
                sendInventoryUpdate(category.getValue());
                sendStorageUpdate(category.getValue());
            }
        }
    }

    public void save() {
        InventoryService service = InventoryService.gI();
        service.updateEquipment(wearing);
        service.updateInventory(inventory);
        service.updateStorage(storage);
    }

    public void handleEquipment(Message m2) throws IOException {
        byte action = m2.in().readByte();
        short slotIndex = m2.in().readShort();

        if (action == 0) {
            if (equipItem(slotIndex)) {
                broadcastWearing();
                sendInventoryUpdate(3);
            } else {
                NetworkService.gI().sendNoticeBox(player.getSession(), "Cannot equip item!");
            }
        } else if (action == 1) {
            if (unequipItem(slotIndex)) {
                broadcastWearing();
                sendInventoryUpdate(3);
            } else {
                NetworkService.gI().sendNoticeBox(player.getSession(), "Inventory full!");
            }
        }
    }

    public void clearInventory() {
        inventory.clear();
        sendInventoryUpdate(3);
        sendInventoryUpdate(4);
        sendInventoryUpdate(7);
    }

    public void clearStorage() {
        storage.clear();
        sendStorageUpdate(3);
        sendStorageUpdate(4);
        sendStorageUpdate(7);
    }

    public void clearEquipment() {
        List<EquipmentItem> items = wearing.unequipAllAndReturn();
        for (EquipmentItem item : items) {
            if (hasBagSpace()) {
                inventory.add(item, 1);
            }
        }
        broadcastWearing();
        sendInventoryUpdate(3);
    }


}