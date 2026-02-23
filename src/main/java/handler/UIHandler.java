package handler;

import game.entity.player.PlayerEntity;
import lombok.extern.slf4j.Slf4j;
import manager.MenuManager;
import model.menu.ConfirmDialog;
import model.menu.InputDialog;
import network.Message;
import network.Session;

import java.io.IOException;


@Slf4j
public class UIHandler {

    public static void onMessage(Session s, Message m) throws IOException {
        switch (m.command) {
            case Command.DIALOG_MORE_OPTION_SERVER -> onInputDialog(s, m);
            case Command.DIALOG_SERVER -> onConfirmDialog(s, m);
            case Command.DYNAMIC_MENU -> onDynamicMenu(s, m);
        }
    }

    private static void onDynamicMenu(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) {
            return;
        }

        short npcId = m.in().readShort();
        byte menuId = m.in().readByte();
        byte index = m.in().readByte();
        MenuManager.handleMenuSelection(p, npcId, menuId, index);
    }

    private static void onConfirmDialog(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        short id = m.in().readShort(); // id
        if (id != p.getId()) {
            return;

        }
        byte dialogId = m.in().readByte();
        boolean flag = (m.in().readByte() == 1);

        ConfirmDialog dialog = p.getConfirmDialog();
        if (dialog != null && dialog.getId() == dialogId) {
            dialog.perform(p, flag, dialog.getArgs());
        }
    }

    private static void onInputDialog(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

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
