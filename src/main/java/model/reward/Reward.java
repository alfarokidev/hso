package model.reward;

import lombok.AllArgsConstructor;
import lombok.Data;
import manager.ItemManager;
import model.item.BaseItem;
import model.item.ItemCategory;
import model.item.PotionItem;

@Data
@AllArgsConstructor
public class Reward {
    private BaseItem item;
    private int quantity;

    public static Reward create(int id, ItemCategory category) {
        return Reward.create(id, 1, category);
    }
    public static Reward create(int id, int quantity, ItemCategory category) {
        BaseItem item = switch (category) {
            case EQUIPMENT -> ItemManager.getInstance().getEquipment(id);
            case POTION -> {
                if (id < 0) {
                    BaseItem potion = new PotionItem();
                    potion.setId(id);
                    yield potion;
                } else {
                    yield ItemManager.getInstance().getPotion(id);
                }
            }
            case MATERIAL -> ItemManager.getInstance().getMaterial(id);
        };

        return new Reward(item, quantity);
    }
}
