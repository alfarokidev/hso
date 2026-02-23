package game.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuildInventory extends BaseInventory {
    private int guildId;

    public GuildInventory() {
        super();
    }

    public GuildInventory(int capacity) {
        super(capacity);
    }
}