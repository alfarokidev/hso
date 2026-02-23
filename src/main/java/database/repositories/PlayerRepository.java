package database.repositories;

import database.SQL;
import game.friend.FriendList;
import game.pet.PlayerPet;
import model.player.Player;

import java.sql.SQLException;
import java.util.List;

public class PlayerRepository {

    public PlayerPet findPlayerPetById(int id) throws SQLException {
        return SQL.from(PlayerPet.class).where("playerId", id).first();
    }

    public Player findById(int id) throws SQLException {
        return SQL.from(Player.class).where("id", id).first();
    }

    public List<Player> findAllById(List<Integer> ids) throws SQLException {
        return SQL.from(Player.class).whereIn("id", ids).get();
    }


    public Player findByAccountId(int accountId) throws SQLException {
        return SQL.from(Player.class).where("uid", accountId).first();
    }

    public List<Player> findAllByAccountId(int accountId) throws SQLException {
        return SQL.from(Player.class).where("uid", accountId).get();
    }

    public Player findByName(String name) throws SQLException {
        return SQL.from(Player.class).where("name", name).first();
    }

    public List<Player> findAll() throws SQLException {
        return SQL.from(Player.class).get();
    }

    public List<Player> findByLevel(int level) throws SQLException {
        return SQL.from(Player.class).where("level", level).get();
    }

    public List<Player> findTopByLevel(int limit) throws SQLException {
        return SQL.from(Player.class).orderByDesc("level").limit(limit).get();
    }

    public long save(Player player) throws SQLException {
        return SQL.insert(player).execute();
    }

    public boolean savePlayerPet(PlayerPet playerPet) throws SQLException {
        return SQL.save(playerPet, "playerId");
    }

    public void update(Player player) throws SQLException {
        SQL.update(player).whereId().execute();
    }

    public void delete(int id) throws SQLException {
        SQL.delete(Player.class).where("id", id).execute();
    }

    public boolean existsByName(String name) throws SQLException {
        return SQL.from(Player.class).where("name", name).exists();
    }

    public int countByAccountId(int accountId) throws SQLException {
        return (int) SQL.from(Player.class).where("account_id", accountId).count();
    }

    public FriendList findFriendList(int id) throws SQLException {
        return SQL.from(FriendList.class).where("id", id).first();
    }

    public boolean saveFriendList(FriendList fl) throws SQLException {
        return SQL.save(fl);
    }

}
