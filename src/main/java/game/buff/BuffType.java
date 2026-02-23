package game.buff;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BuffType {
    BUFF(1),
    DEBUFF(2);

    private final int value;


}
