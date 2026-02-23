package utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StatUtils {
    private static final int PERCENT_SCALE = 10000;
    /**
     * Apply a single integer-based percentage bonus.
     * @param baseValue The original value
     * @param percentBonus Bonus in integer format (10000 = 100%)
     * @return The final value after bonus
     */
    public static int applyBonus(int baseValue, int percentBonus) {
        return baseValue + (baseValue * percentBonus / PERCENT_SCALE);
    }

    /**
     * Apply multiple bonuses sequentially.
     * @param baseValue Original value
     * @param bonuses Array of integer-based percentage bonuses
     * @return Final value after all bonuses
     */
    public static int applyBonuses(int baseValue, int... bonuses) {
        int result = baseValue;
        for (int bonus : bonuses) {
            result = applyBonus(result, bonus);
        }
        return result;
    }
}