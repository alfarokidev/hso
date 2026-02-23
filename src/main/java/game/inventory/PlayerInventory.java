package game.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerInventory extends BaseInventory {
    private int playerId;


    public PlayerInventory() {
        super();
    }

    public PlayerInventory(int capacity) {
        super(capacity);
    }

}