package model.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Vgo {
    private String name;
    private int x;
    private int y;
    private int toMap;
    private int toX;
    private int toY;
}
