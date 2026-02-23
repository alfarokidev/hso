package database;

import game.entity.player.PlayerEntity;
import game.guild.GuildService;
import lombok.extern.slf4j.Slf4j;
import model.player.PlayerMapper;
import service.PlayerService;

@Slf4j
public class DataSaver {

    public static void savePlayerData(PlayerEntity player) {
        PlayerService playerService = PlayerService.gI();
        playerService.update(PlayerMapper.toModel(player));
        player.getInventoryManager().save();
        playerService.saveFriendList(player.getFriendList());
        boolean sPet = playerService.savePlayerPet(player.getPlayerPet());

        log.debug("save player pet size{} status {}", player.getPlayerPet().getPets().size(), sPet);
    }

    public static void saveGlobalData() {
        GuildService.getInstance().save();
    }
}
