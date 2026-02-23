package model.menu;

import lombok.Builder;
import lombok.Getter;
import game.entity.player.PlayerEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Builder
@Getter
public class Menu {
    private int npc;
    private int id;
    private String title;
    private String name;
    private List<Menu> menus;
    private Map<String, Object> args;
    private MenuAction action;
    private BiConsumer<PlayerEntity, Map<String, Object>> actionArgs;

    public void perform(PlayerEntity player) throws IOException {
        if (actionArgs != null) {
            actionArgs.accept(player, args);
        } else if (action != null) {
            action.execute(player);
        }
    }
}
