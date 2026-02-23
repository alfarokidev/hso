package game.effects;


import game.stat.StatType;
import lombok.Data;


@Data
public final class StatModifier {
    private final StatType type;
    private final int value;



}