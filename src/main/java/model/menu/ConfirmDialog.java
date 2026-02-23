package model.menu;


import game.entity.player.PlayerEntity;
import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Builder
@Getter
public class ConfirmDialog {
    private int id;
    private String text;
    private Map<String, Object> args;
    private TriConsumer<PlayerEntity, Boolean, Map<String, Object>> onRespond;

    public void perform(PlayerEntity player, boolean yesOrNo, Map<String, Object> args) {
        if (onRespond != null) {
            onRespond.accept(player, yesOrNo, args);
        }
    }
}