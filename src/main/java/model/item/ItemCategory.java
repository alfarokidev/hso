package model.item;

import lombok.Getter;

@Getter
public enum ItemCategory {

    MATERIAL(7, true, 3200),
    POTION(4, true, 3200),
    EQUIPMENT(3, false, 1);
    private final int value;
    private final boolean stackable;
    private final int maxStack;

    ItemCategory(int value, boolean stackable, int maxStack) {
        this.value = value;
        this.stackable = stackable;
        this.maxStack = maxStack;
    }



    public static ItemCategory fromValue(int value) {
        for (ItemCategory type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
