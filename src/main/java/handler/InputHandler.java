package handler;

import game.entity.player.PlayerEntity;
import lombok.extern.slf4j.Slf4j;
import model.menu.InputDialog;
import network.Message;

import java.io.IOException;

@Slf4j
public class InputHandler {

    private InputHandler() {
    }

    public static void handle(PlayerEntity p, Message m) throws IOException {
        short npcId = m.in().readShort();
        short menuId = m.in().readShort();
        byte size = m.in().readByte();


        if (p.getInputDialog() != null) {

            InputDialog box = p.getInputDialog();
            String[] values = new String[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = m.in().readUTF();
            }
            if (box.getAction() != null) {
                box.getAction().accept(p, values);
            }
            p.setInputDialog(null);

            log.info("NPC_ID {} MENU_ID {} SIZE {}", npcId, menuId, size);
        }

    }
}
