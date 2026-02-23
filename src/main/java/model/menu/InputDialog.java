package model.menu;

import game.entity.player.PlayerEntity;
import lombok.Builder;
import lombok.Getter;


import java.util.List;
import java.util.function.BiConsumer;

@Builder
@Getter
public class InputDialog {

    private int npcId;
    private String title;
    private List<String> fields;
    private BiConsumer<PlayerEntity, String[]> action;
}
