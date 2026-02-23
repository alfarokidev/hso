package model.monster;

import lombok.Data;

@Data
public class Monster {
    private int mid;
    private String name;
    private int level;
    private int hp;
    private byte typeMove;
}
