package manager;


import game.entity.Position;
import game.guild.GuildManager;
import game.map.GameMap;
import game.map.Zone;
import game.party.Party;
import game.pet.Pet;
import lombok.extern.slf4j.Slf4j;
import model.map.MapData;
import game.entity.player.PlayerEntity;
import model.map.MapName;
import model.monster.GuildMine;
import model.player.Player;
import model.player.PlayerMapper;
import service.NetworkService;
import service.PlayerService;
import utils.NumberUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class WorldManager {
    public final ConcurrentHashMap<Integer, MapName> mapNames = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, MapData> mapData = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, GameMap> gameMaps = new ConcurrentHashMap<>();


    private WorldManager() {
    }

    public static WorldManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final WorldManager INSTANCE = new WorldManager();
    }

    // Template operations
    public void addMapData(MapData data) {
        mapData.put(data.getId(), data);
    }

    public void addMapName(MapName mapName) {
        mapNames.put(mapName.getId(), mapName);
    }

    public void createGameMaps() {
        mapData.values().forEach(data ->
                gameMaps.put(data.getId(), new GameMap(data.getId(), data))

        );

        for (GuildMine mine : GuildManager.getInstance().getGuildCrystalMap().values()) {
            if (mine.getGuards().isEmpty())
                continue;

            for (Integer id : mine.getGuards()) {
                createClone(id, new Position(mine.getId(), mine.getX(), mine.getY()));
            }
        }

        log.debug("Created {} game maps", gameMaps.size());
    }

    public GameMap getGameMap(int mapId) {
        return gameMaps.get(mapId);
    }

    // Core map operations
    public void enterMap(PlayerEntity player) {
        transitionToMap(player, player.getPosition());
    }

    public void changeMap(PlayerEntity player, Position location) {

        leaveMap(player);
        transitionToMap(player, location);
        log.info("Player {} changed map to {}", player.getId(), location.getMap());
    }

    public void leaveMap(PlayerEntity player) {
        Zone zone = player.getZone();
        if (zone == null) return;


        zone.removePlayer(player);
        notifyExit(zone, player);
        log.info("Player {} left map statistic {} ", player.getId(), zone.getStats());
    }


    // Private helpers
    private void transitionToMap(PlayerEntity player, Position location) {
        GameMap map = getGameMap(location.getMap());
        if (map == null) {
            log.error("Map not found: {}", location.getMap());
            return;
        }

        Zone zone = map.assignZone(player);
        player.setLocation(location, map, zone);
        player.onSpawn();
        NetworkService.gI().sendChangeMap(player);
        if (location.getMap() == 50) {
            if (!player.getPlayerPet().getAllPet().isEmpty()) {
                NetworkService.gI().sendPetContainer(player);
            }

            if (!player.getPlayerPet().getEggs().isEmpty()) {
                for (Pet pet : player.getPlayerPet().getEggs()) {
                    NetworkService.gI().sendAddItemPetContainer(player, 3, pet);
                }
            }
        }
        if (player.getMount() != null) {
            zone.broadcast(notify -> NetworkService.gI().sendUseMount(notify, player));
        }

        NetworkService.gI().sendMainCharInfo(player);

        notifyEnter(player);
    }


    private void notifyEnter(PlayerEntity player) {
        player.setTeleport(false);

        Zone zone = player.getZone();
        if (zone == null) return;

        NetworkService net = NetworkService.gI();
        zone.broadcastExcept(player, p -> {
            net.sendMove(p, player);
            net.sendMove(player, p);
        });

        // Broadcast char wearing
        zone.broadcast(
                target -> {
                    NetworkService.gI().sendWearing(player, target);
                    NetworkService.gI().sendWearing(target, player);
                }
        );

        zone.getMonsters().forEach(monster ->
                net.sendMove(player, monster));

        Party party;
        if ((party = player.getParty()) != null) {
            party.broadcastPartyInfo();
        }


    }

    private void notifyExit(Zone zone, PlayerEntity exit) {

        if (zone == null) return;
        zone.broadcast(p -> {
                    NetworkService.gI().sendPlayerExit(p, exit.getId());
                    NetworkService.gI().sendRemoveActor(p, exit);
                }
        );

    }


    public boolean isOnline(int playerId) {
        PlayerEntity p;
        if ((p = findPlayer(playerId)) != null) {
            return p.isOnline();
        }

        return false;
    }

    public PlayerEntity findPlayer(int id) {
        for (GameMap map : gameMaps.values()) {
            PlayerEntity entity = map.findPlayer(id);
            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    public PlayerEntity findPlayer(String name) {
        for (GameMap map : gameMaps.values()) {
            PlayerEntity entity = map.findPlayer(name);
            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    public void worldBroadcast(Consumer<PlayerEntity> action) {

        if (action == null) return;

        for (GameMap map : gameMaps.values()) {
            Collection<PlayerEntity> players = map.getAllPlayers();
            for (PlayerEntity p : players) {
                if (p != null && !p.isModeBot()) {
                    action.accept(p);
                }
            }

        }

    }

    public synchronized void reloadMapData(List<MapData> freshData) {

        for (MapData newData : freshData) {

            MapData old = mapData.get(newData.getId());

            if (old != null) {
                old.updateFrom(newData);
            } else {
                mapData.put(newData.getId(), newData);
            }
        }

        log.debug("MapData reloaded (data-only) : {} maps", freshData.size());
    }

    public void createClone(int playerId, Position pos) {
        Player player = PlayerService.gI().getPlayer(playerId);
        PlayerEntity entity = PlayerMapper.toEntity(player);
        entity.setIdCopy(playerId);
        entity.setName(String.format("Guard - %s", player.getName()));
        entity.setPosition(new Position(pos.getMap(), pos.getX(), pos.getY() + 24));
        entity.initial();
        entity.setClone(true);
        entity.setTypePK((byte) 0);
        entity.setId(NumberUtils.next());
        entity.setAttackCooldown(8000L);
        enterMap(entity);
        entity.initBot();
    }

    public Collection<MapData> getMapData() {
        return mapData.values();
    }
}