package model.npc;

import lombok.Getter;

public enum NpcName {
    ZORO(-2),
    LISA(-3),
    DOUBAR(-4),
    HAMMER(-5),
    AMAN(-7),
    TELEPORT(-10),
    TELEPORT_LV40(-33),
    TELEPORT_LV100(-55),
    WIZARD(-36),
    ANNA(-44),
    ODA(-81),
    ALISAMA(-77),
    BLACK_EYE(-75),
    ZONE(-43),
    BALLARD(-53),
    RANKING(-32),
    CAOCAO(-91),
    PET_MANAGER(-42),
    ZULU(-8);

    @Getter
    private final int id;

    NpcName(int id) {
        this.id = id;
    }

    public static NpcName fromId(int id) {
        for (NpcName npc : values()) {
            if (npc.id == id) return npc;
        }
        return null;
    }
}
