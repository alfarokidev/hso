package game.inventory;


import lombok.Getter;
import lombok.Setter;
import model.item.BaseItem;
import model.item.ItemCategory;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class BaseInventory {
    protected int id;
    protected int capacity;
    protected InventorySlot[] items;

    public BaseInventory() {
        this(126);
    }

    public BaseInventory(int capacity) {
        this.capacity = capacity;
        this.items = new InventorySlot[capacity];
        initSlots();
    }

    private void initSlots() {
        for (int i = 0; i < capacity; i++) {
            if (items[i] == null) items[i] = new InventorySlot();
        }
    }

    // ==================== CAPACITY ====================

    public int used() {
        return countOccupied();
    }

    public int available() {
        return capacity - used();
    }

    public boolean isFull() {
        return available() == 0;
    }

    // ==================== ADD ====================

    public boolean add(BaseItem item, int amt) {
        if (item == null || amt <= 0) return false;

        ItemCategory category = item.getCategory();
        return category.isStackable()
                ? addStackable(item, amt, category.getMaxStack())
                : addNonStackable(item, amt);
    }

    public boolean add(BaseItem item) {
        return add(item, 1);
    }

    private boolean addNonStackable(BaseItem item, int amt) {
        if (!canAdd(amt)) return false;

        int added = 0;
        for (InventorySlot s : items) {
            if (added >= amt) break;
            if (s.isEmpty()) {
                s.set(item, 1);
                added++;
            }
        }
        return added == amt;
    }

    private boolean addStackable(BaseItem item, int amt, int maxStack) {
        int remaining = amt;

        // Fill existing stacks
        for (InventorySlot s : items) {
            if (remaining <= 0) break;
            if (!s.isEmpty() && s.getItem().getId() == item.getId()) {
                int canAdd = maxStack - s.getAmount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining);
                    s.setAmount(s.getAmount() + toAdd);
                    remaining -= toAdd;
                }
            }
        }

        // Use empty slots
        for (InventorySlot s : items) {
            if (remaining <= 0) break;
            if (s.isEmpty()) {
                int toAdd = Math.min(maxStack, remaining);
                s.set(item, toAdd);
                remaining -= toAdd;
            }
        }

        return remaining == 0;
    }

    // ==================== REMOVE ====================

    public int remove(int idx) {
        if (idx < 0 || idx >= capacity) return 0;
        InventorySlot s = items[idx];
        if (s.isEmpty()) return 0;

        int removed = s.getAmount();
        s.clear();
        return removed;
    }

    public int removeById(int itemId, int amt) {
        int remaining = amt;

        for (int i = 0; i < capacity && remaining > 0; i++) {
            InventorySlot s = items[i];
            if (!s.isEmpty() && s.getItem().getId() == itemId) {
                int current = s.getAmount();
                if (current <= remaining) {
                    s.clear();
                    remaining -= current;
                } else {
                    s.setAmount(current - remaining);
                    remaining = 0;
                }
            }
        }

        return amt - remaining;
    }

    public int removeByIdAndCategory(int itemId, int amt, ItemCategory category) {
        int remaining = amt;

        for (int i = 0; i < capacity && remaining > 0; i++) {
            InventorySlot s = items[i];
            if (!s.isEmpty()
                    && s.getItem().getId() == itemId
                    && s.getItem().getCategory() == category) {

                int current = s.getAmount();
                if (current <= remaining) {
                    s.clear();
                    remaining -= current;
                } else {
                    s.setAmount(current - remaining);
                    remaining = 0;
                }
            }
        }

        return amt - remaining;
    }


    public void clear() {
        Arrays.stream(items).forEach(InventorySlot::clear);
    }

    // ==================== QUERY ====================

    public InventorySlot slot(int idx) {
        return (idx >= 0 && idx < capacity) ? items[idx] : null;
    }

    public int count(int itemId) {
        return Arrays.stream(items)
                .filter(s -> !s.isEmpty() && s.getItem().getId() == itemId)
                .mapToInt(InventorySlot::getAmount)
                .sum();
    }

    public int countByCategory(ItemCategory category) {
        return Arrays.stream(items)
                .filter(s -> !s.isEmpty() && s.getItem().getCategory() == category)
                .mapToInt(InventorySlot::getAmount)
                .sum();
    }

    public int sizeByCategory(ItemCategory category) {
        return (int) Arrays.stream(items)
                .filter(s -> !s.isEmpty() && s.getItem().getCategory() == category)
                .count();
    }

    public boolean has(int itemId) {
        return count(itemId) > 0;
    }

    public boolean has(int itemId, int amt) {
        return count(itemId) >= amt;
    }

    public Optional<InventorySlot> findSlot(int itemId, ItemCategory category) {
        List<InventorySlot> reversed =
                Arrays.stream(items)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    Collections.reverse(list);
                                    return list;
                                }
                        ));

        for (InventorySlot slot : reversed) {
            if (!slot.isEmpty()
                    && slot.getItem().getId() == itemId
                    && slot.getItem().getCategory() == category) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public List<InventorySlot> itemsByCategory(ItemCategory category) {
        return Arrays.stream(items)
                .filter(s -> !s.isEmpty() && s.getItem().getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<InventorySlot> allItems() {
        return Arrays.stream(items)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== METRICS ====================

    public int countEmpty() {
        return (int) Arrays.stream(items).filter(InventorySlot::isEmpty).count();
    }

    public int countOccupied() {
        return capacity - countEmpty();
    }

    public boolean canAdd(int amt) {
        return available() >= amt;
    }

    public boolean isEmpty() {
        return countOccupied() == 0;
    }

    public double fillPercentage() {
        return (double) used() / capacity * 100.0;
    }


}