package model.item;

import lombok.Data;

import java.util.Arrays;

@Data
public class PartSettings {
    private int[] fashion = new int[]{-1, -1, -1, -1, -1, -1, -1};
    private boolean showWing = true;
    private boolean showCloak = true;
    private boolean showHair = true;
    private boolean showWeapon = true;
    private boolean showTitle = true;
    private boolean showMask = true;

    public void reset() {
        Arrays.fill(fashion, -3);
        showWing = true;
        showCloak = true;
        showHair = true;
        showWeapon = true;
        showTitle = true;
        showMask = true;
    }

}
