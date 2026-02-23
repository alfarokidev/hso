package handler;

import game.entity.player.PlayerEntity;
import lombok.extern.slf4j.Slf4j;
import model.npc.NpcName;
import manager.MenuManager;
import network.Message;
import network.Session;
import service.NetworkService;
import service.ShopService;

import java.io.IOException;

@Slf4j
public class NpcHandler {

    public static void handle(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte id = m.in().readByte();
        NpcName name = NpcName.fromId(id);
        if (name == null) {
            NetworkService.gI().sendNoticeBox(s, "Fitur belum ada");
            log.debug("NPC ID {}", id);
            return;
        }

        switch (name) {

            case LISA -> ShopService.getInstance().sendShop(p, NpcName.LISA, 0);
            case RANKING -> MenuManager.openRankMenu(p);
            case TELEPORT -> MenuManager.openTeleportLevel1(p, NpcName.TELEPORT.getId());
            case TELEPORT_LV40 -> MenuManager.openTeleportLevel40(p, NpcName.TELEPORT_LV40.getId());
            case WIZARD -> MenuManager.openWizardMenu(p);
            case DOUBAR -> MenuManager.openDoubarMenu(p);
            case HAMMER -> MenuManager.openHammerMenu(p);
            case ANNA -> MenuManager.openAnnaMenu(p);
            case AMAN -> MenuManager.openAmanMenu(p);
            case ZORO -> MenuManager.openZoroMenu(p);
            case ALISAMA -> MenuManager.openAlisamaMenu(p);
            case BLACK_EYE -> MenuManager.openBlackEyeMenu(p);
            case ZONE -> NetworkService.gI().sendStatusArea(p);
            case BALLARD -> MenuManager.openBallardMenu(p);
            case CAOCAO -> MenuManager.openCacaoMenu(p);
            case PET_MANAGER -> MenuManager.openPetManagerMenu(p);
            case ZULU -> MenuManager.openZuluMenu(p);
        }
    }


}
