package manager;

import model.item.Fashion;
import lombok.Getter;
import lombok.Setter;
import model.item.*;
import model.config.SVConfig;
import utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ItemManager {

    public static final int EQUIPMENT_OFFSET = 0;
    public static final int POTION_OFFSET = 10_000;
    public static final int MATERIAL_OFFSET = 20_000;
    private static final int LEVEL_STEP = 10;
    private static final int MAX_LEVEL_INDEX = 13;


    private final Map<Integer, BaseItem> itemMap = new ConcurrentHashMap<>();

    private final Map<Integer, ItemOption> itemOptionMap = new ConcurrentHashMap<>();

    private final Map<Integer, Fashion> itemFashion = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private final ArrayList<EquipmentItem>[] equipmentByLevel =
            new ArrayList[MAX_LEVEL_INDEX + 1];


    private ItemManager() {
        for (int i = 0; i <= MAX_LEVEL_INDEX; i++) {
            equipmentByLevel[i] = new ArrayList<>();
        }
    }

    private static class Holder {
        private static final ItemManager INSTANCE = new ItemManager();
    }

    public static ItemManager getInstance() {
        return Holder.INSTANCE;
    }

    public EquipmentItem getEquipment(int id) {
        EquipmentItem item = (EquipmentItem) itemMap.get(EQUIPMENT_OFFSET + id);
        return item != null ? item.copy() : null;
    }

    public PotionItem getPotion(int id) {
        return (PotionItem) itemMap.get(POTION_OFFSET + id);
    }

    public MaterialItem getMaterial(int id) {
        return (MaterialItem) itemMap.get(MATERIAL_OFFSET + id);
    }

    public void addEquipment(EquipmentItem item) {
        itemMap.put(EQUIPMENT_OFFSET + item.getId(), item);
        if (item.getColor() != 5) {
            equipmentByLevel[levelIndex(item.getLevel())].add(item);
        }
    }

    public void addPotion(PotionItem item) {
        itemMap.put(POTION_OFFSET + item.getId(), item);
    }

    public void addMaterial(MaterialItem item) {
        itemMap.put(MATERIAL_OFFSET + item.getId(), item);
    }

    public ItemOption getOption(int id) {
        return itemOptionMap.get(id);
    }

    public <T extends BaseItem> List<T> filterByClass(Class<T> clazz) {
        return itemMap.values().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    public EquipmentItem randomEquipment(int level) {
        return randomFrom(equipmentByLevel[levelIndex(level)]);
    }

    private static int levelIndex(int level) {
        int idx = level / LEVEL_STEP;
        if (idx < 0) return 0;
        return Math.min(idx, MAX_LEVEL_INDEX);
    }

    private static <T> T randomFrom(List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(NumberUtils.randomInt(0, list.size() - 1));
    }

    public void addItemOption(ItemOption item) {
        itemOptionMap.put(item.getId(), item);
    }


    public List<ItemOption> getAllItemOption() {
        return itemOptionMap.values().stream().toList();
    }

    public void addFashion(Fashion item) {
        itemFashion.put(item.getItemId(), item);
    }

    public Fashion getFashion(int itemId) {
        return itemFashion.get(itemId);
    }

    public void clearAll() {

        itemMap.clear();
        itemOptionMap.clear();
        itemFashion.clear();

        // clear equipment by level
        for (int i = 0; i <= MAX_LEVEL_INDEX; i++) {
            equipmentByLevel[i].clear();
        }
    }

}
