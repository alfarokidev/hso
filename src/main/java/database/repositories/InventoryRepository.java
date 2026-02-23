package database.repositories;

import database.SQL;
import game.equipment.PlayerEquipment;
import game.inventory.PlayerInventory;


import java.sql.SQLException;
import java.util.List;

public class InventoryRepository {

    public List<PlayerEquipment> findEquipmentById(List<Integer> ids) throws SQLException {
        return SQL.from(PlayerEquipment.class).whereIn("playerId", ids).get();
    }

    public PlayerEquipment findEquipmentById(int playerId) throws SQLException {
        return SQL.from(PlayerEquipment.class).where("playerId", playerId).first();
    }

    public void createEquipment(PlayerEquipment inventoy) throws SQLException {
        SQL.insert(inventoy).execute();
    }

    public void updateEquipment(PlayerEquipment inventoy) throws SQLException {
        SQL.save(inventoy);
    }

    public PlayerInventory findInventoryById(int playerId) throws SQLException {
        return SQL.from(PlayerInventory.class).where("playerId", playerId).first();
    }

    public void createInventory(PlayerInventory inventoy) throws SQLException {
        SQL.insert(inventoy).execute();
    }

    public void updateInventory(PlayerInventory inventoy) throws SQLException {
        SQL.save(inventoy);
    }

    public void createStorage(PlayerInventory inventoy) throws SQLException {
        SQL.insert(inventoy).table("player_storage").execute();
    }

    public void updateStorage(PlayerInventory inventoy) throws SQLException {
        SQL.save(inventoy, "id", "player_storage");
    }

    public PlayerInventory findStorageById(int playerId) throws SQLException {
        return SQL.from(PlayerInventory.class).table("player_storage").where("playerId", playerId).first();
    }

}
