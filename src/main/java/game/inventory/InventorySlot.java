package game.inventory;

import lombok.Data;
import model.item.BaseItem;

@Data
public class InventorySlot {

    private BaseItem item;
    private int amount;

    public InventorySlot() {
        this.item = null;
        this.amount = 0;
    }

    public void set(BaseItem item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public void clear() {
        this.item = null;
        this.amount = 0;
    }

    public boolean isEmpty() {
        return item == null || amount == 0;
    }

    public void decrease() {
        if (amount - 1 <= 0) {
            clear();
            return;
        }

        amount -= 1;

    }
    public void decrease(int amt) {
        if (amount - amt <= 0) {
            clear();
            return;
        }

        amount -= amt;

    }

    @Override
    public String toString() {
        return isEmpty()
                ? "InventorySlot[empty]"
                : String.format("InventorySlot[item=%s, amount=%d]", item.getName(), amount);
    }
}