package model.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Attribute {
    private int id;
    private double value;


    public int getIntValue() {
        return (int) Math.round(value * 10000 / 100.0);
    }
}
