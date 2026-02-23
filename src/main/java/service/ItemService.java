package service;

import game.entity.player.PlayerEntity;
import handler.Command;
import lombok.extern.slf4j.Slf4j;
import manager.ConfigManager;
import manager.ItemManager;
import model.item.EquipmentItem;
import model.item.Option;
import network.Message;

import java.util.List;

@Slf4j
public class ItemService {
    private ItemService() {
    }

    private static class Holder {
        private static final ItemService INSTANCE = new ItemService();
    }

    public static ItemService getInstance() {
        return Holder.INSTANCE;
    }


    public void sendCreateMedal(PlayerEntity notify, EquipmentItem item) {
        Message m = new Message(Command.HOP_RAC);
        try {
            m.out().writeByte(3);
            m.out().writeByte(3);
            m.out().writeUTF("You've created " + item.getName());
            m.out().writeByte(3);
            m.out().writeUTF(item.getName());
            m.out().writeByte(item.getRole());
            m.out().writeShort(item.getId());
            m.out().writeByte(item.getType());
            m.out().writeShort(item.getIcon());
            m.out().writeByte(item.getPlus()); // tier
            m.out().writeShort(item.getLevel()); // level required
            m.out().writeByte(item.getColor()); // color
            m.out().writeByte(0); // can sell
            m.out().writeByte(1); // can trade
            m.out().writeByte(item.getOption().size());
            for (Option op : item.getOption()) {
                m.out().writeByte(op.getId());
                m.out().writeInt(op.getValue());
            }
            m.out().writeInt(0); // time use
            m.out().writeByte(0);
            m.out().writeByte(0);
            m.out().writeByte(0);
            notify.send(m);
        } catch (Exception ignore) {
        }
    }


    public void sendUpgradeMaterialMedal(PlayerEntity notify) {
        Message m = new Message(Command.HOP_RAC);
        try {
            m.out().writeByte(4);
            List<Short> materials = ConfigManager.getInstance().getSvConfig().getMaterialMedal().get(notify.getRole());
            m.out().writeByte(materials.size());
            for (short id : materials) {
                m.out().writeShort(id); // ID
                m.out().writeShort(2); // Quantity
            }
            notify.send(m);
        } catch (Exception ignore) {
        }
    }

}
