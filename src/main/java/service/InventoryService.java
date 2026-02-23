package service;

import database.repositories.InventoryRepository;
import game.equipment.PlayerEquipment;
import game.inventory.PlayerInventory;
import lombok.extern.slf4j.Slf4j;
import manager.ItemManager;


import java.sql.SQLException;
import java.util.List;

@Slf4j
public class InventoryService {
    private final InventoryRepository repository = new InventoryRepository();

    private InventoryService() {
    }

    private static class Holder {
        private static final InventoryService INSTANCE = new InventoryService();
    }

    public static InventoryService gI() {
        return InventoryService.Holder.INSTANCE;
    }


    public boolean createEquipment(PlayerEquipment playerInventory) {
        try {
            repository.createEquipment(playerInventory);
            return true;
        } catch (SQLException e) {
            log.error("createEquipment() Failed: {}", e.getMessage());
            return false;
        }
    }

    public void updateEquipment(PlayerEquipment playerInventory) {
        try {
            repository.updateEquipment(playerInventory);
        } catch (SQLException e) {
            log.error("updateEquipment() Failed: {}", e.getMessage());
        }
    }

    public PlayerEquipment findEquipmentById(int playerId) {
        try {
            return repository.findEquipmentById(playerId);

        } catch (SQLException e) {
            log.error("findEquipmentByPlayerId() Failed: {}", e.getMessage());
            return null;
        }
    }

    public List<PlayerEquipment> findEquipmentById(List<Integer> ids) {
        try {
            return repository.findEquipmentById(ids);
        } catch (SQLException e) {
            log.error("findEquipmentByPlayerId() Failed: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean createInventory(PlayerInventory playerInventory) {
        try {
            repository.createInventory(playerInventory);
            return true;
        } catch (SQLException e) {
            log.error("createInventory() Failed: {}", e.getMessage());
            return false;
        }
    }

    public void updateInventory(PlayerInventory playerInventory) {
        try {
            repository.updateInventory(playerInventory);
        } catch (SQLException e) {
            log.error("updateInventory() Failed: {}", e.getMessage());
        }
    }

    public PlayerInventory findInventoryById(int playerId) {
        try {
            return repository.findInventoryById(playerId);

        } catch (SQLException e) {
            log.error("findInventoryByPlayerId() Failed: {}", e.getMessage());
            return null;
        }
    }

    public PlayerInventory findStorageById(int playerId) {
        try {
            return repository.findStorageById(playerId);

        } catch (SQLException e) {
            log.error("findStorageById() Failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean createStorage(PlayerInventory playerInventory) {
        try {
            repository.createStorage(playerInventory);
            return true;
        } catch (SQLException e) {
            log.error("createStorage() Failed: {}", e.getMessage());
            return false;
        }
    }

    public void updateStorage(PlayerInventory playerInventory) {
        try {
            repository.updateStorage(playerInventory);
        } catch (SQLException e) {
            log.error("updateStorage() Failed: {}", e.getMessage());
        }
    }

    public static PlayerEquipment createStarterEquipment(int role) {
        PlayerEquipment equipment = new PlayerEquipment();
        switch (role) {
            case 0 -> {
                equipment.equip(ItemManager.getInstance().getEquipment(0));
                equipment.equip(ItemManager.getInstance().getEquipment(80));
                equipment.equip(ItemManager.getInstance().getEquipment(20));
            }
            case 1 -> {
                equipment.equip(ItemManager.getInstance().getEquipment(5));
                equipment.equip(ItemManager.getInstance().getEquipment(105));
                equipment.equip(ItemManager.getInstance().getEquipment(145));
            }
            case 2 -> {
                equipment.equip(ItemManager.getInstance().getEquipment(10));
                equipment.equip(ItemManager.getInstance().getEquipment(90));
                equipment.equip(ItemManager.getInstance().getEquipment(50));
                equipment.equip(ItemManager.getInstance().getEquipment(130));
            }
            default -> {
                equipment.equip(ItemManager.getInstance().getEquipment(15));
                equipment.equip(ItemManager.getInstance().getEquipment(95));
                equipment.equip(ItemManager.getInstance().getEquipment(55));
                equipment.equip(ItemManager.getInstance().getEquipment(135));
            }
        }

        return equipment;
    }

}
