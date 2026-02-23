package model.item;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetOption extends Option {
    private int maxValue;

    public PetOption(int id, int value, int maxValue) {
        super(id, value);
        this.maxValue = maxValue;
    }
}
