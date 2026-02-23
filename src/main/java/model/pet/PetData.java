package model.pet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.item.PetOption;

import java.util.ArrayList;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class PetData {
    private int id;
    private String name;
    private int[] image;
    private int icon;
    protected int frameCount;
    private int color;
    private int maxGrow;
    private int hatchTime;
    private List<PetOption> options = new ArrayList<>();
    private int typeMove;
}
