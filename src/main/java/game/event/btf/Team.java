package game.event.btf;


import game.entity.player.PlayerEntity;
import lombok.Data;
import manager.MenuManager;
import model.npc.Go;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static model.npc.Go.DESA_SRIGALA;

@Data
public class Team {
    private String name;
    private Go location;
    private int towerId;
    private boolean destroyed;
    private byte flag;
    private Map<Integer, PlayerEntity> players = new ConcurrentHashMap<>();

    public Team(String name, int towerId, Go location) {
        this.name = name;
        this.towerId = towerId;
        this.location = location;
    }

    public void addPlayer(PlayerEntity player) {
        players.put(player.getId(), player);
    }

    public boolean isMyTeam(int playerId) {
        return players.containsKey(playerId);
    }

    public void destroy() {
        destroyed = true;
        players.values().forEach(player -> {
            MenuManager.teleport(player, DESA_SRIGALA, 1);
        });
        players.clear();
    }


}
