package model.item;

import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class EquipmentItem extends BaseItem {
    private int type;
    private int part;
    private int level;
    private int color;
    private int role;
    private List<Option> option;
    private boolean lock = true;
    private byte grade;
    private byte plus;
    private int timeUse;
    private int expireDate;

    private byte[] gem = new byte[]{-1, 1, 1};

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.EQUIPMENT;
    }

    @Override
    public void onUse(PlayerEntity player) {

    }

    public EquipType getEquipmentType() {
        return EquipType.fromValue(type);
    }


    public EquipmentItem copy() {
        EquipmentItem copy = new EquipmentItem();

        copy.id = this.id;
        copy.name = this.name;
        copy.icon = this.icon;
        copy.type = this.type;
        copy.part = this.part;
        copy.level = this.level;
        copy.color = this.color;
        copy.role = this.role;
        copy.lock = this.lock;
        copy.grade = this.grade;
        copy.plus = this.plus;
        copy.timeUse = this.timeUse;
        copy.expireDate = this.expireDate;

        if (this.option != null) {
            copy.option = this.option.stream()
                    .map(o -> new Option(o.getId(), o.getValue()))
                    .toList();
        }

        copy.gem = this.gem != null ? this.gem.clone() : null;

        return copy;
    }

}
