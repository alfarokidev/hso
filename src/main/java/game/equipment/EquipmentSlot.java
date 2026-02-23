package game.equipment;

import lombok.Data;
import model.item.EquipmentItem;

@Data
public class EquipmentSlot {
    private int slotIndex;
    private EquipmentItem item;

    public EquipmentSlot(int slotIndex) {
        this.slotIndex = slotIndex;
        this.item = null;
    }

    public void set(EquipmentItem item) {
        this.item = item;
    }

    public void clear() {
        this.item = null;
    }

    public boolean isEmpty() {
        return item == null;
    }

    @Override
    public String toString() {
        return isEmpty()
                ? String.format("EquipmentSlot[%d: empty]", slotIndex)
                : String.format("EquipmentSlot[%d: %s]", slotIndex, item.getName());
    }
}