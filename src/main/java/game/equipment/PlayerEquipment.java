package game.equipment;


import lombok.Getter;
import lombok.Setter;
import model.item.EquipmentItem;
import model.item.ItemCategory;

import java.util.*;

@Getter
@Setter
public class PlayerEquipment {
    private static final int EQUIPMENT_SLOTS = 24;

    private int id;
    private int playerId;
    private EquipmentSlot[] items;

    public PlayerEquipment() {
        this.items = new EquipmentSlot[EQUIPMENT_SLOTS];
        initSlots();
    }

    private void initSlots() {
        for (int i = 0; i < EQUIPMENT_SLOTS; i++) {
            if (items[i] == null) items[i] = new EquipmentSlot(i);
        }
    }

    // ==================== EQUIP ====================

    public boolean equip(EquipmentItem item) {
        if (item == null) return false;

        EquipType equipType = EquipType.fromValue(item.getType());
        if (equipType == null) return false;

        int targetSlot = equipType.getPrimarySlotIndex();
        return equip(targetSlot, item);
    }

    public boolean equip(int slotIdx, EquipmentItem item) {
        if (!isValidSlot(slotIdx) || item == null) return false;

        EquipmentSlot slot = items[slotIdx];
        slot.set(item);
        return true;
    }

    public EquipmentItem equipAndReturn(EquipmentItem item) {
        if (item == null) return null;

        EquipType equipType = EquipType.fromValue(item.getType());
        if (equipType == null) return item;

        int targetSlot = equipType.getPrimarySlotIndex();
        EquipmentItem oldItem = unequip(targetSlot);
        equip(targetSlot, item);

        return oldItem;
    }

    public EquipmentItem equipAndReturn(EquipmentItem item, int slotIndex) {
        if (item == null) return null;
        EquipmentItem oldItem = unequip(slotIndex);
        equip(slotIndex, item);

        return oldItem;
    }

    // ==================== UNEQUIP ====================

    public EquipmentItem unequip(int slotIdx) {
        if (!isValidSlot(slotIdx)) return null;

        EquipmentSlot slot = items[slotIdx];
        if (slot.isEmpty()) return null;

        EquipmentItem item = slot.getItem();
        slot.clear();
        return item;
    }

    public EquipmentItem unequip(EquipType equipType) {
        if (equipType == null) return null;
        return unequip(equipType.getPrimarySlotIndex());
    }

    public void unequipAll() {
        Arrays.stream(items).forEach(EquipmentSlot::clear);
    }

    public List<EquipmentItem> unequipAllAndReturn() {
        List<EquipmentItem> items = new ArrayList<>();
        for (EquipmentSlot slot : this.items) {
            if (!slot.isEmpty()) {
                items.add(slot.getItem());
                slot.clear();
            }
        }
        return items;
    }

    public List<EquipmentItem> unequipAllAndReturnExcept(Set<EquipType> except) {
        List<EquipmentItem> items = new ArrayList<>();
        for (EquipmentSlot slot : this.items) {
            if (slot.isEmpty()) continue;

            EquipType slotType = EquipType.fromIndex(slot.getSlotIndex());
            if (except.contains(slotType)) {
                continue;
            }

            items.add(slot.getItem());
            slot.clear();

        }
        return items;
    }


    // ==================== QUERY ====================

    public EquipmentSlot slot(int idx) {
        return isValidSlot(idx) ? items[idx] : null;
    }

    public EquipmentItem item(int idx) {
        return isValidSlot(idx) ? items[idx].getItem() : null;
    }

    public EquipmentItem item(EquipType equipType) {
        if (equipType == null) return null;
        return item(equipType.getPrimarySlotIndex());
    }

    public boolean hasEquipped(int itemId) {
        return Arrays.stream(items)
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> s.getItem().getId() == itemId);
    }

    public boolean hasEquipped(EquipType equipType) {
        if (equipType == null) return false;
        int slotIdx = equipType.getPrimarySlotIndex();
        return !items[slotIdx].isEmpty();
    }

    public Optional<Integer> findEquipped(int itemId) {
        for (int i = 0; i < EQUIPMENT_SLOTS; i++) {
            if (!items[i].isEmpty() && items[i].getItem().getId() == itemId) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public List<Integer> findAllEquipped(int itemId) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < EQUIPMENT_SLOTS; i++) {
            if (!items[i].isEmpty() && items[i].getItem().getId() == itemId) {
                indices.add(i);
            }
        }
        return indices;
    }

    public List<EquipmentItem> allEquipped() {
        List<EquipmentItem> equipped = new ArrayList<>();
        for (EquipmentSlot slot : items) {
            if (!slot.isEmpty()) equipped.add(slot.getItem());
        }
        return equipped;
    }

    public List<EquipmentSlot> allEquippedExcept(EnumSet<EquipType> exclude) {
        List<EquipmentSlot> equipped = new ArrayList<>();

        for (EquipmentSlot slot : items) {
            if (slot.isEmpty()) continue;

            // Find the EquipType for this slot
            EquipType slotType = EquipType.fromIndex(slot.getSlotIndex());

            // If this slot type is in the exclude set, skip it
            if (slotType != null && exclude.contains(slotType)) continue;

            equipped.add(slot);
        }

        return equipped;
    }


    public List<EquipmentItem> equippedByCategory(ItemCategory category) {
        List<EquipmentItem> equipped = new ArrayList<>();
        for (EquipmentSlot slot : items) {
            if (!slot.isEmpty() && slot.getItem().getCategory() == category) {
                equipped.add(slot.getItem());
            }
        }
        return equipped;
    }

    public Map<EquipType, EquipmentItem> getEquipmentMap() {
        Map<EquipType, EquipmentItem> map = new HashMap<>();
        for (EquipmentSlot slot : items) {
            if (!slot.isEmpty()) {
                EquipmentItem item = slot.getItem();
                EquipType type = EquipType.fromValue(item.getType());
                if (type != null) {
                    map.put(type, item);
                }
            }
        }
        return map;
    }

    // ==================== STATUS ====================

    public int countEquipped() {
        return (int) Arrays.stream(items)
                .filter(s -> !s.isEmpty())
                .count();
    }

    public int countEmpty() {
        return EQUIPMENT_SLOTS - countEquipped();
    }

    public boolean isEmpty() {
        return countEquipped() == 0;
    }

    public boolean isFull() {
        return countEmpty() == 0;
    }

    public boolean hasSpace() {
        return countEmpty() > 0;
    }

    public boolean canEquip(EquipmentItem item) {
        if (item == null) return false;

        EquipType equipType = EquipType.fromValue(item.getType());
        return equipType != null;
    }

    // ==================== HELPERS ====================

    private boolean isValidSlot(int idx) {
        return idx >= 0 && idx < EQUIPMENT_SLOTS;
    }


    @Override
    public String toString() {
        return String.format("PlayerEquipment[equipped=%d/%d, empty=%d]",
                countEquipped(), EQUIPMENT_SLOTS, countEmpty());
    }

    public int[] getBaseVisual() {
        int[] result = new int[7];
        Arrays.fill(result, -1);
        return result;
    }
}