package model.item;


import lombok.Getter;

public enum PartType {
    BODY(0),
    LEG(1),
    HAT(2),
    WING(4),
    HAIR(5),
    HEAD(6),
    EYE(3);   // e

    @Getter
    private final int id;

    PartType(int arrayIndex) {
        this.id = arrayIndex;
    }

    public static PartType fromId(int id) {
        for (PartType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return null; // or throw exception
    }
}
