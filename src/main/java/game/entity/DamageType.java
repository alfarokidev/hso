package game.entity;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum DamageType {

    PHYSICAL(List.of(0, 7)),
    FIRE(List.of(1, 8)),
    ICE(List.of(2, 9)),
    POISON(List.of(3, 10)),
    LIGHTING(List.of(4, 11)),
    LIGHT(List.of(5, 12)),
    DARK(List.of(6, 13));

    private final List<Integer> ids;

    // fast lookup map
    private static final Map<Integer, DamageType> LOOKUP = new HashMap<>();

    static {
        for (DamageType type : values()) {
            for (int id : type.ids) {
                LOOKUP.put(id, type);
            }
        }
    }

    DamageType(List<Integer> ids) {
        this.ids = ids;
    }

    public static DamageType fromValue(int id) {
        return LOOKUP.getOrDefault(id, PHYSICAL);
    }
}
