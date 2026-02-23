package dev;

import database.DatabaseLoader;
import database.SQL;
import game.guild.GuildManager;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;
import model.ModelMapper;
import model.account.Account;
import model.map.MapData;
import model.map.Point;
import model.monster.GuildMine;
import model.player.Player;
import utils.CryptoUtils;
import utils.FileUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
public class Test {
    public static void main(String[] args) throws SQLException {
        DatabaseLoader.getInstance().loadAll();

        byte[] data = FileUtils.loadFile("data/player.json");
        if (data == null) {
            log.debug("JSON FILE NOT FOUND");
            return;
        }

        String json = new String(data, StandardCharsets.UTF_8);
        List<Player> players = ModelMapper.fromJsonList(json, Player.class);
        for (Player p : players) {
            SQL.insert(p).execute();
        }
        log.debug("ACCOUNT SIZE {}", players.size());
    }
}
