package game.pet;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.item.PetOption;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pet {
    private static final int[] EVOLUTION_LEVELS = {19, 29, 39, 49, 59, 69, 79, 89, 99};

    private int id;
    private String name;
    private int[] image;
    private int icon;
    protected int frameCount;
    private int color;
    private List<PetOption> options = new ArrayList<>();
    private int typeMove;

    // PET INFO
    private int level;
    private long experience;
    private int grow;
    private int maxGrow;
    private int age;
    private int strength, dexterity, vitality, intelligence;
    private int maxPoints;
    private long hatchTime;
    private long expireDate;
    private boolean isFollow;

    public int getLevelPercent() {
        return Math.toIntExact((experience * 1000) / getRequiredExp());
    }

    public int getImage() {
        if (level < 10) return image[0];
        if (level < 20) return image[1];
        return image[2];
    }

    public int getAgeHours() {
        long age = System.currentTimeMillis() - hatchTime;
        age /= 3_600_000;
        if (age < 0) {
            age = 0;
        } else if (age > Integer.MAX_VALUE) {
            age = Integer.MAX_VALUE;
        }
        return (int) age;
    }

    public long getRequiredExp() {
        return 100_000L + (level * 25_000L);
    }

    public void addExperience(long amount) {
        if (amount <= 0) {
            return;
        }
        experience += amount;
        if (canEvolution()) {
            return;
        }

        while (experience >= getRequiredExp()) {

            experience -= getRequiredExp();
            level++;

            if (level == 20 || level == 30) {
                color += 1;
            }

            if (level > 30) {
                level = 30;
            }
        }
    }

    public boolean addPoint(int index, int points) {
        if (points <= 0) return false;

        switch (index) {
            case 0 -> strength = Math.min(strength + points, maxPoints);
            case 1 -> dexterity = Math.min(dexterity + points, maxPoints);
            case 2 -> vitality = Math.min(vitality + points, maxPoints);
            case 3 -> intelligence = Math.min(intelligence + points, maxPoints);
        }

        return true;
    }

    private boolean isMax(int index) {
        return switch (index) {
            case 0 -> strength >= maxPoints;
            case 1 -> dexterity >= maxPoints;
            case 2 -> vitality >= dexterity;
            case 3 -> intelligence >= maxPoints;
            default -> false;
        };
    }

    public boolean canEvolution() {
        if (!isEvolutionLevel()) {
            return false;
        }

        // 99% = 990 because getLevelPercent() uses 0–1000 scale
        return getLevelPercent() >= 990;
    }

    private boolean isEvolutionLevel() {
        for (int evoLevel : EVOLUTION_LEVELS) {
            if (level == evoLevel) {
                return true;
            }
        }
        return false;
    }

    public void evolve() {
        experience = 0;

        level++;

        if (level > 100) {
            level = 100;
        }

        color++;
        if (color > 5) {
            color = 5;
        }
    }

    public boolean isHatched() {
        return getRemainingSeconds() <= 0;
    }

    public long getRemainingHatchTimeMs() {
        return Math.max(0, hatchTime - System.currentTimeMillis());
    }

    public long getRemainingSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getRemainingHatchTimeMs());
    }

    public int getRemainingMinutes() {
        return Math.toIntExact(getRemainingHatchTimeMs() / 60_000L);
    }

}