package model.item;

import game.entity.player.PlayerEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class MaterialItem extends BaseItem {

    private String description;
    private int priceType;
    private int materialType;
    private int price;
    private int value;
    private int sell;
    private int canTrade;
    private int colorName;


    @Override
    public ItemCategory getCategory() {
        return ItemCategory.MATERIAL;
    }

    @Override
    public void onUse(PlayerEntity player) {

    }
}
