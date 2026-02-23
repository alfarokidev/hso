package service;

import game.equipment.EquipType;
import game.guild.Guild;
import game.guild.GuildManager;
import game.pet.PetManager;
import lombok.extern.slf4j.Slf4j;
import manager.ConfigManager;
import model.item.*;
import model.npc.NpcName;
import handler.Command;
import manager.ItemManager;
import manager.ShopManager;
import model.pet.PetData;
import model.shop.Shop;
import model.shop.ShopItem;
import network.Message;
import game.entity.player.PlayerEntity;
import utils.IconHelper;
import utils.IconType;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static game.equipment.EquipType.*;
import static game.equipment.EquipType.LEG;
import static game.equipment.EquipType.WING;
import static model.npc.NpcName.PET_MANAGER;
import static model.npc.NpcName.ZORO;

@Slf4j
public class ShopService {
    private ShopService() {
    }

    private static class Holder {
        private static final ShopService INSTANCE = new ShopService();
    }

    public static ShopService getInstance() {
        return Holder.INSTANCE;
    }


    public void sendEquipmentShop(PlayerEntity p, Shop shop) {
        Message m = new Message(Command.NPC_INFO);
        try {
            m.out().writeUTF(shop.getName());
            m.out().writeByte(1);
            m.out().writeShort(shop.getItems().size());
            for (ShopItem s : shop.getItems()) {
                EquipmentItem temp = ItemManager.getInstance().getEquipment(s.getItemId());
                m.out().writeShort(temp.getId());
                m.out().writeUTF(temp.getName());
                m.out().writeByte(temp.getRole());
                m.out().writeByte(temp.getType());
                m.out().writeShort(temp.getIcon());
                m.out().writeLong(s.getPrice()); // price
                m.out().writeShort(temp.getLevel()); // level
                m.out().writeByte(temp.getColor());
                m.out().writeByte(temp.getOption().size()); // option
                for (Option op : temp.getOption()) {
                    m.out().writeByte(op.getId());
                    m.out().writeInt(op.getValue());
                }
                m.out().writeByte(s.getPriceType()); // type money
            }
            p.setCurrentNpcShop(shop);
            p.send(m);
        } catch (Exception ignore) {
        }
    }

    public void sendShop(PlayerEntity p, NpcName name) throws IOException {
        sendShop(p, name, -1);
    }

    public void sendShop(PlayerEntity p, NpcName name, int index) throws IOException {

        switch (name) {
            case LISA -> {
                Shop s = ShopManager.getInstance().getShop("Toko Ramuan"); // Key name in database
                if (s == null) return;

                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF(s.getName());
                m.out().writeByte(0);
                m.out().writeShort(s.getItems().size());
                for (ShopItem item : s.getItems()) {
                    m.out().writeShort(item.getItemId());

                    PotionItem potion = ItemManager.getInstance().getPotion(item.getItemId());
                    item.setPriceType(potion.getPriceType());
                    item.setPrice(potion.getPrice());
                }
                p.setCurrentNpcShop(s);
                p.send(m);
            }
            case WIZARD -> {
                Shop s = ShopManager.getInstance().getShop("Toko Material"); // Key name in database
                if (s == null) return;


                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF(s.getName());
                m.out().writeByte(4);
                m.out().writeShort(s.getItems().size());
                for (ShopItem item : s.getItems()) {
                    m.out().writeShort(item.getItemId());

                    MaterialItem material = ItemManager.getInstance().getMaterial(item.getItemId());
                    item.setPriceType(material.getPriceType());
                    item.setPrice(material.getPrice());
                }
                p.setCurrentNpcShop(s);
                p.send(m);
            }
            case DOUBAR -> {
                switch (index) {
                    case 0 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Warrior"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 1 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Assasin"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 2 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Penyihir"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 3 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Penembak"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                }
            }
            case HAMMER -> {
                switch (index) {
                    case 0 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Warrior"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 1 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Assasin"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 2 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Penyihir"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 3 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Penembak"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                }
            }
            case ALISAMA -> {
                switch (index) {
                    case 0 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Warrior 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 1 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Assasin 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 2 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Penyihir 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 3 -> {
                        Shop s = ShopManager.getInstance().getShop("Armor Penembak 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                }
            }
            case BLACK_EYE -> {
                switch (index) {
                    case 0 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Warrior 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 1 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Assasin 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 2 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Penyihir 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                    case 3 -> {
                        Shop s = ShopManager.getInstance().getShop("Senjata Penembak 40"); // Key name in database
                        if (s == null) return;

                        sendEquipmentShop(p, s);
                    }
                }
            }
            case AMAN -> {
                p.getInventoryManager().sendStorageUpdate(3);
                p.getInventoryManager().sendStorageUpdate(4);
                p.getInventoryManager().sendStorageUpdate(7);

                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF("Penyimpanan");
                m.out().writeByte(3);
                m.out().writeShort(0);
                p.send(m);
            }
            case ZORO -> {
                List<Integer> icons = IconHelper.getIconIdByCategory(p.getSession().getZoomLv(), IconType.CLAN);
                List<ShopItem> items = icons.stream().map(id -> {
                    ShopItem item = new ShopItem();
                    item.setItemId(id);
                    item.setPrice(500);
                    item.setPriceType(1);
                    return item;
                }).toList();

                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF("Guild Icon");
                m.out().writeByte(7);

                m.out().writeShort(items.size()); // 31 in team server
                for (ShopItem item : items) {
                    m.out().writeShort(item.getItemId());
                }
                p.send(m);

                Shop s = new Shop();
                s.setNpcId(ZORO.getId());
                s.setItems(items);
                p.setCurrentNpcShop(s);
            }
            case PET_MANAGER -> {
                switch (index) {
                    case 0 -> {
                        Message m = new Message(Command.NPC_INFO);
                        m.out().writeUTF("Pet Container");
                        m.out().writeByte(11);
                        m.out().writeShort(0);
                        p.send(m);
                    }
                    case 1 -> {
                        Message m = new Message(Command.NPC_INFO);
                        m.out().writeUTF("Makanan Pet");
                        m.out().writeByte(0);
                        List<ShopItem> items = new ArrayList<>();
                        for (int i = 48; i < 52; i++) {
                            ShopItem it = new ShopItem();
                            it.setItemId(i);
                            it.setPrice(10);
                            it.setPriceType(1);
                            items.add(it);
                        }

                        m.out().writeShort(items.size());
                        for (ShopItem it : items) {
                            m.out().writeShort(it.getItemId());
                        }

                        Shop s = new Shop();
                        s.setCategory(4);
                        s.setId(PET_MANAGER.getId());
                        s.setItems(items);
                        p.setCurrentNpcShop(s);
                        p.send(m);
                    }
                    case 2 -> {
                        Shop s = new Shop();
                        s.setName("Toko Telur");
                        s.setNpcId(PET_MANAGER.getId());

                        s.setItems(PetManager.getInstance().getPetMap().values().stream().map(petData -> {
                            ShopItem item = new ShopItem();
                            item.setItemId(petData.getId());
                            item.setPrice(500);
                            item.setPriceType(1);
                            return item;
                        }).toList());
                        s.setCategory(3);
                        p.setCurrentNpcShop(s);
                        sendEquipmentShop(p, s);
                    }
                }
            }
            case CAOCAO -> {
                switch (index) {
                    case 0 -> {
                        List<ShopItem> items = ItemManager
                                .getInstance()
                                .filterByClass(EquipmentItem.class)
                                .stream()
                                .filter(item -> {
                                    EnumSet<EquipType> types = EnumSet.of(
                                            WEAPON,
                                            ARMOR,
                                            LEG,
                                            HELMET,
                                            BOOTS,
                                            RING_1,
                                            RING_2,
                                            NECKLACE,
                                            WING,
                                            PET,
                                            GLOVE,
                                            FASHION_MEDAL
                                            );
                                    EquipType type = fromValue(item.getType());
                                    return type != null && !types.contains(type);
                                })
                                .map(item -> {
                                    ShopItem shop = new ShopItem();
                                    shop.setItemId(item.getId());
                                    shop.setPrice(500);
                                    shop.setPriceType(1);
                                    shop.setDuration(720);
                                    return shop;
                                }).toList();

                        Shop shop = new Shop();
                        shop.setCategory(3);
                        shop.setName("Fashion");
                        shop.setItems(items);
                        sendEquipmentShop(p, shop);
                    }
                    case 1 -> {
                        Message m = new Message(Command.NPC_INFO);
                        m.out().writeUTF("Shop Hair");
                        m.out().writeByte(2);

                        List<ShopItem> items = new ArrayList<>();
                        for (int i = 0; i < 63; i++) {
                            ShopItem item = new ShopItem();
                            item.setItemId(i + 4);
                            item.setPrice(500);
                            item.setPriceType(1);
                            items.add(item);
                        }

                        m.out().writeShort(items.size());
                        for (ShopItem item : items) {
                            m.out().writeShort(item.getItemId());
                            m.out().writeUTF(String.format("Style %d", item.getItemId()));
                            m.out().writeShort(item.getItemId());
                            m.out().writeLong(item.getPrice());
                            m.out().writeByte(item.getPriceType());
                            m.out().writeByte(0); // Options
                        }

                        Shop s = new Shop();
                        s.setNpcId(NpcName.CAOCAO.getId());
                        s.setCategory(3);
                        s.setItems(items);
                        p.setCurrentNpcShop(s);

                        p.send(m);
                    }

                }
            }
        }


    }

    public void opeCraftingDialog(PlayerEntity p, int type) throws IOException {

        switch (type) {
            case 0 -> {
                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF("Upgrade Peralatan");
                m.out().writeByte(5);
                m.out().writeShort(0);
                p.send(m);
            }
            case 1 -> {
                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF("Create Medal");
                m.out().writeByte(19);
                m.out().writeShort(0);

                List<Short> materials = ConfigManager.getInstance().getSvConfig().getMaterialMedal().get(p.getRole());
                m.out().writeByte(materials.size());
                for (short id : materials) {
                    m.out().writeShort(id); // ID
                    m.out().writeShort(1); // Quantity
                }
                p.send(m);
            }
            case 2 -> {
                Message m = new Message(Command.NPC_INFO);
                m.out().writeUTF("Upgrade Medal");
                m.out().writeByte(20);
                m.out().writeShort(0);
                p.send(m);
            }
            default -> {
            }
        }
    }

    public void buyItem(PlayerEntity p, Message m) throws IOException {
        byte type = m.in().readByte();
        short itemId = m.in().readShort();
        int quantity = Short.toUnsignedInt(m.in().readShort());

        if (p.getCurrentNpcShop() == null) {
            log.debug("INVALID SHOP");
            return;
        }

        log.info("Buy Item Type {}", type);
        switch (type) {
            case 0 -> {

                Optional<ShopItem> opt = p.getCurrentNpcShop().find(itemId);
                if (opt.isEmpty()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak terdaftar");
                    return;
                }

                ShopItem shopItem = opt.get();
                PotionItem item = ItemManager.getInstance().getPotion(shopItem.getItemId());

                if (item == null) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak ditemukan");
                    return;
                }

                if (!p.getInventoryManager().hasBagSpace()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Inventory penuh");
                    return;
                }

                int totalPrice = item.getPrice() * quantity;

                if (shopItem.useGem()) {
                    if (p.getGems() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                        return;
                    }
                    p.spendGem(totalPrice);
                } else {
                    if (p.getGold() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Gold cukup permata");
                        return;
                    }
                    p.spendGold(totalPrice);
                }

                // Add the item to the player's inventory
                if (item.getId() > 10 && item.getId() < 17 || item.getId() == 26) {
                    quantity = quantity * 300;
                }

                p.getInventoryManager().addToBag(item, quantity);
                p.getInventoryManager().updateInventory();
                NetworkService.gI().sendNoticeBox(p.getSession(), "Pembelian berhasil");
            }
            case 1 -> {
                Optional<ShopItem> opt = p.getCurrentNpcShop().find(itemId);
                if (opt.isEmpty()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak terdaftar");
                    return;
                }
                ShopItem shopItem = opt.get();

                EquipmentItem item = ItemManager.getInstance().getEquipment(shopItem.getItemId());
                if (item == null) {
                    p.sendMessageDialog("Item tidak ditemukan");
                    return;
                }

                if (!p.getInventoryManager().hasBagSpace()) {
                    p.sendMessageDialog("Inventory penuh");
                    return;
                }

                int totalPrice = shopItem.getPrice();

                if (shopItem.useGem()) {
                    if (p.getGems() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                        return;
                    }
                    p.spendGem(totalPrice);
                } else {
                    if (p.getGold() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Gold cukup permata");
                        return;
                    }
                    p.spendGold(totalPrice);
                }


                p.getInventoryManager().addToBag(item);
                p.getInventoryManager().updateInventory();
                p.sendMessageDialog("Pembelian berhasil");

            }
            case 2 -> {
                Optional<ShopItem> opt = p.getCurrentNpcShop().find(itemId);
                if (opt.isEmpty()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak terdaftar");
                    return;
                }

                ShopItem shopItem = opt.get();
                int totalPrice = Math.toIntExact((long) shopItem.getPrice() * quantity);

                if (shopItem.useGem()) {
                    if (p.getGems() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                        return;
                    }
                    p.spendGem(totalPrice);
                } else {
                    if (p.getGold() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Gold cukup permata");
                        return;
                    }
                    p.spendGold(totalPrice);
                }


                byte[] body = p.getBody();
                body[2] = (byte) itemId;
                p.getInventoryManager().updateInventory();
                NetworkService.gI().sendMainCharInfo(p);
                p.getZone().broadcast(player -> NetworkService.gI().sendCharInfo(player, p));
                p.sendMessageDialog("Pembelian berhasil");

            }
            case 4 -> {
                Optional<ShopItem> opt = p.getCurrentNpcShop().find(itemId);
                if (opt.isEmpty()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak terdaftar");
                    return;
                }

                ShopItem shopItem = opt.get();
                int totalPrice = shopItem.getPrice();

                MaterialItem item = ItemManager.getInstance().getMaterial(itemId);
                if (item == null) {
                    p.sendMessageDialog("Item tidak ditemukan");
                    return;
                }

                if (!p.getInventoryManager().hasBagSpace()) {
                    p.sendMessageDialog("Inventory penuh");
                    return;
                }

                if (shopItem.useGem()) {
                    if (p.getGems() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                        return;
                    }
                    p.spendGem(totalPrice);
                } else {
                    if (p.getGold() < totalPrice) {
                        NetworkService.gI().sendNoticeBox(p.getSession(), "Gold tidak cukup");
                        return;
                    }
                    p.spendGold(totalPrice);
                }

                p.getInventoryManager().addToBag(item);
                p.getInventoryManager().updateInventory();
                p.sendMessageDialog("Pembelian berhasil");
            }
            default -> {
                Optional<ShopItem> opt = p.getCurrentNpcShop().find(itemId);
                if (opt.isEmpty()) {
                    NetworkService.gI().sendNoticeBox(p.getSession(), "Item tidak terdaftar");
                    return;
                }

                ShopItem shopItem = opt.get();
                int totalPrice = shopItem.getPrice() * quantity;

                BaseItem item = switch (p.getCurrentNpcShop().getCategory()) {
                    case 3 -> ItemManager.getInstance().getEquipment(itemId);
                    case 4 -> ItemManager.getInstance().getPotion(itemId);
                    case 7 -> ItemManager.getInstance().getMaterial(itemId);
                    default -> null;
                };

                if (item == null) {

                    if (type != 7) {
                        p.sendMessageDialog("Item tidak diketahui");
                        return;
                    }


                    Guild guild = GuildManager.getInstance().getPlayerGuild(p.getId());
                    if (guild == null) {
                        p.sendMessageDialog("Kamu tidak memiliki guild");
                        return;
                    }

                    if (!guild.isLeader(p)) {
                        p.sendMessageDialog("Hanya pemimpin guild yang dapat mengubah logo guild");
                        return;
                    }
                    if (shopItem.useGem()) {
                        if (!p.spendGem(totalPrice)) {
                            NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                            return;
                        }
                    } else {
                        if (!p.spendGold(totalPrice)) {
                            NetworkService.gI().sendNoticeBox(p.getSession(), "Gold tidak cukup");
                            return;
                        }
                    }
                    guild.setIcon((short) shopItem.getItemId());
                    p.getZone().broadcast(player -> NetworkService.gI().sendCharInfo(player, p));
                    p.getInventoryManager().updateInventory();
                    NetworkService.gI().sendMainCharInfo(p);
                    p.sendMessageDialog("Pembelian berhasil");


                } else {


                    if (shopItem.useGem()) {
                        if (!p.spendGem(totalPrice)) {
                            NetworkService.gI().sendNoticeBox(p.getSession(), "Permata tidak cukup");
                            return;
                        }
                    } else {
                        if (p.spendGold(totalPrice)) {
                            NetworkService.gI().sendNoticeBox(p.getSession(), "Gold tidak cukup");
                            return;
                        }
                    }

                    p.getZone().broadcast(player -> NetworkService.gI().sendCharInfo(player, p));
                    p.getInventoryManager().updateInventory();
                    NetworkService.gI().sendMainCharInfo(p);
                    p.sendMessageDialog("Pembelian berhasil");
                }
            }
        }
    }
}
