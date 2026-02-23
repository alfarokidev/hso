package utils;

import game.entity.player.PlayerEntity;
import game.equipment.EquipType;
import game.equipment.PlayerEquipment;
import model.player.Part;
import model.player.PlayerMapper;
import service.InventoryService;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static game.equipment.EquipType.*;
import static game.equipment.EquipType.HELMET;
import static game.equipment.EquipType.LEG;

public class PlayerHelper {

    public static List<Part> getPartPlayer(int playerId) {

        PlayerEquipment wear = InventoryService.gI().findEquipmentById(playerId);
        if (wear == null) {
            return List.of();
        }

        EnumSet<EquipType> only = EnumSet.of(WEAPON, ARMOR, HELMET, LEG, WING);

        return wear.allEquipped().stream()
                .filter(Objects::nonNull) // extra safety
                .map(item -> Map.entry(item, Objects.requireNonNull(fromValue(item.getType()))))
                .filter(e -> e.getValue() != null && only.contains(e.getValue()))
                .map(e -> new Part(e.getKey().getType(), e.getKey().getPart()))
                .toList();
    }



}
