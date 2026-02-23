package model.item;

import lombok.Data;

@Data
public class ItemOption {
    private int id;
    private String name;
    private int color;
    private boolean percent;
    private double upgradeBonus;


    public int getUpgradeBonusInt() {
        return (int) Math.round(upgradeBonus * 10000 / 100.0);
    }
}
