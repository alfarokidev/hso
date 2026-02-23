package model.menu;

import game.entity.player.PlayerEntity;

import java.io.IOException;

@FunctionalInterface
public interface MenuAction {
    void execute(PlayerEntity player) throws IOException;
}
