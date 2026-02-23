package model.item;

import game.entity.player.PlayerEntity;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public abstract class BaseItem {
    protected int id;
    protected String name;
    protected int icon;
    public abstract ItemCategory getCategory();

    public abstract void onUse(PlayerEntity player)throws IOException;
}