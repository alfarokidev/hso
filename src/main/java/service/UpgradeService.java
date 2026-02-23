package service;

import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import handler.Command;
import lombok.extern.slf4j.Slf4j;
import model.item.EquipmentItem;
import model.item.Option;
import network.Message;
import utils.NumberUtils;

@Slf4j
public class UpgradeService {
    public static final int UPGRADE_COOLDOWN = 1500;
    public static final int MAX_LEVEL = 15;
    private static final int[] BASE_SUCCESS_RATE = {
            100, // +0 -> +1
            95, // +1 -> +2
            90, // +2 -> +3
            85, // +3 -> +4
            80, // +4 -> +5
            75, // +5 -> +6
            70, // +6 -> +7
            65, // +7 -> +8
            60, // +8 -> +9
            50, // +9 -> +10
            45, // +10 -> +11
            35, // +11 -> +12
            25, // +12 -> +13
            15, // +13 -> +14
            5  // +14 -> +15
    };

    private UpgradeService() {
    }

    private static class Holder {
        private static final UpgradeService INSTANCE = new UpgradeService();
    }

    public static UpgradeService getInstance() {
        return UpgradeService.Holder.INSTANCE;
    }

    public boolean tryUpgrade(
            EquipmentItem item,
            int bonusPercent,
            boolean protectFail
    ) {
        int currentLevel = item.getPlus();

        if (currentLevel >= MAX_LEVEL) {
            return false;
        }

        int baseChance = BASE_SUCCESS_RATE[currentLevel];
        int finalChance = Math.min(100, baseChance + bonusPercent);

        int roll = NumberUtils.randomInt(1, 100);

        if (roll <= finalChance) {
            item.setPlus((byte) (currentLevel + 1));
            return true;
        } else {
            if (!protectFail) {
                item.setPlus((byte) Math.max(0, currentLevel - 1));
            }
            return false;
        }
    }

    public boolean canUpgrade(EquipmentItem item) {
        EquipType type = EquipType.fromValue(item.getType());
        if (type == null) return false;

        switch (type) {
            case WEAPON, ARMOR, LEG, RING_1, RING_2, BOOTS, HELMET, WING, GLOVE, NECKLACE -> {
                return getBaseRate(item) != 0;
            }
            default -> {
                return false;
            }
        }
    }

    public int getBaseRate(EquipmentItem item) {
        if (item.getPlus() < 0 || item.getPlus() >= MAX_LEVEL) return 0;

        return BASE_SUCCESS_RATE[item.getPlus() + 1];
    }

    public void sendSelectItem(PlayerEntity p, int index, String percent) {
        Message m = new Message(Command.REBUILD_ITEM);
        try {
            m.out().writeByte(0);
            m.out().writeShort(index);
            m.out().writeByte(3);
            m.out().writeUTF(percent);
            p.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendUpgradeResult(PlayerEntity p, boolean isSuccess, String percent) {
        Message m = new Message(Command.REBUILD_ITEM);
        try {
            m.out().writeByte(isSuccess ? 3 : 0);
            m.out().writeUTF(percent);
            p.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }

    public void sendUpgradeMedalResult(PlayerEntity notify, boolean success, EquipmentItem item) {
        try {
            Message m = new Message(-105);
            m.out().writeByte(3);
            if (success) {
                m.out().writeByte(3);
            } else {
                m.out().writeByte(4);
            }
            if (success) {
                m.out().writeUTF(String.format("You have successfully upgraded %s into +%d", item.getName(), item.getPlus()));
            } else {
                m.out().writeUTF(String.format("“Failed to upgrade %s to +%d.", item.getName(), item.getPlus() + 1));
            }
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
            m.out().writeByte(0); // can trade
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


    public void sendClear(PlayerEntity p) {
        Message m = new Message(Command.REBUILD_ITEM);
        try {
            m.out().writeByte(6);
            p.send(m);
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
        }
    }
}
