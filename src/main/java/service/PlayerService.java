package service;

import database.repositories.PlayerRepository;
import game.friend.FriendList;
import game.pet.PlayerPet;
import lombok.extern.slf4j.Slf4j;
import model.player.Player;

import java.sql.SQLException;
import java.util.List;

@Slf4j
public class PlayerService {
    private final PlayerRepository repository = new PlayerRepository();
    private static final PlayerService INSTANCE = new PlayerService();

    public static PlayerService gI() {
        return INSTANCE;
    }

    /**
     * Get character list for account (lightweight)
     */
    public List<Player> getAllByAccountId(int accountId) {
        try {
            return repository.findAllByAccountId(accountId);
        } catch (SQLException e) {
            log.info("getAllByAccountId() Failed : {}", e.getMessage());
            return List.of();
        }
    }

    public List<Player> findAllById(List<Integer> ids) {
        try {
            return repository.findAllById(ids);
        } catch (SQLException e) {
            log.info("findAllById() Failed : {}", e.getMessage());
            return List.of();
        }
    }


    /**
     * Get full character data by character id
     */
    public Player getPlayer(int characterId) {
        try {
            return repository.findById(characterId);
        } catch (SQLException e) {
            log.error("getPlayer() Failed : {}", e.getMessage());
            return null;
        }
    }

    public long createPlayer(Player player) {
        try {
            return repository.save(player);
        } catch (SQLException e) {
            log.error("createPlayer() Failed: {}", e.getMessage());
            return -1;
        }
    }

    public boolean existsByName(String name) {
        try {
            return repository.existsByName(name);
        } catch (SQLException e) {
            log.error("existsByName() Failed: {}", e.getMessage());
            return false;
        }
    }

    public void update(Player player) {
        try {
            repository.update(player);
        } catch (SQLException e) {
            log.error("update() Failed: {}", e.getMessage());
        }
    }

    public List<Player> findTopByLevel(int limit) {
        try {
            return repository.findTopByLevel(limit);
        } catch (Exception e) {
            log.error("findTopByLevel() Failed: {}", e.getMessage());
            return List.of();
        }
    }

    public FriendList findFriendList(int id) {
        try {
            return repository.findFriendList(id);
        } catch (Exception e) {
            log.error("findFriendList() Failed: {}", e.getMessage());
            return null;
        }
    }

    public void saveFriendList(FriendList list) {
        try {
            repository.saveFriendList(list);
        } catch (Exception e) {
            log.error("saveFriendList() Failed: {}", e.getMessage());
        }
    }

    public PlayerPet findPlayerPetById(int playerId) {
        try {
            return repository.findPlayerPetById(playerId);
        } catch (Exception e) {
            log.error("findPlayerPet() Failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean savePlayerPet(PlayerPet playerPet) {
        try {
            return repository.savePlayerPet(playerPet);
        } catch (SQLException e) {
            log.error("savePlayerPet() Failed: {}", e.getMessage(), e);
            return false;
        }
    }


}
