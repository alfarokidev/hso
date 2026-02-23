package game.equipment;

import lombok.Getter;

import java.util.Set;


@Getter
public enum EquipType {
    ARMOR(Set.of(0), Set.of(1)),
    WEAPON(Set.of(8, 9, 10, 11), Set.of(0)), // type IDs, default slot indices
    GLOVE(Set.of(3), Set.of(2)),
    RING_1(Set.of(4), Set.of(3)),
    RING_2(Set.of(4), Set.of(9)),
    NECKLACE(Set.of(5), Set.of(4)),
    PET(Set.of(14), Set.of(5)),
    HELMET(Set.of(2), Set.of(6)),
    LEG(Set.of(1), Set.of(7)),
    BOOTS(Set.of(6), Set.of(8)),
    WING(Set.of(7), Set.of(10)),
    COSTUME(Set.of(15), Set.of(11)),

    FASHION_MEDAL(Set.of(16), Set.of(12)),
    FASHION_MASK(Set.of(21), Set.of(13)),
    FASHION_WING(Set.of(22), Set.of(14)),
    FASHION_CLOAK(Set.of(23), Set.of(15)),
    FASHION_WEAPON(Set.of(24), Set.of(16)),
    FASHION_HAIR(Set.of(25), Set.of(17)),
    FASHION_HEADPHONE(Set.of(26), Set.of(18)),
    FASHION_TITLE(Set.of(27), Set.of(19)),
    FASHION_BODY(Set.of(28), Set.of(20)),
    FASHION_CROWN(Set.of(26), Set.of(21)),
    FASHION_MOUNT(Set.of(29), Set.of(22)),
    FASHION_12(Set.of(30), Set.of(23));

    private final Set<Integer> typeIds;      // your existing type numbers
    private final Set<Integer> slotIndices;  // slots in the 24-slot array

    EquipType(Set<Integer> typeIds, Set<Integer> slotIndices) {
        this.typeIds = typeIds;
        this.slotIndices = slotIndices;
    }

    /**
     * Map a type ID to its EquipType
     */
    public static EquipType fromValue(int typeId) {
        for (EquipType type : values()) {
            if (type.typeIds.contains(typeId)) return type;
        }
        return null;
    }

    public static EquipType fromIndex(int slotIndex) {
        for (EquipType type : values()) {
            if (type.slotIndices.contains(slotIndex)) {
                return type;
            }
        }
        return null;
    }

    public int getPrimarySlotIndex() {
        // deterministic, safe
        return slotIndices.iterator().next();
    }
}
