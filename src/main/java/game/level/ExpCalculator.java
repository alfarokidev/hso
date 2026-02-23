package game.level;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExpCalculator {


    // Configuration constants - adjust these for game balance
    private static final double BASE_EXP_MULTIPLIER = 15.0;  // Lower since it's per-hit
    private static final double LEVEL_SCALING_POWER = 1.8;
    private static final double REQUIRED_EXP_POWER = 2.5;
    private static final long BASE_REQUIRED_EXP = 100;

    /**
     * Calculate EXP reward for hitting a monster
     * This is called on EVERY hit, not just on kill
     *
     * @param monsterLevel     The level of the monster being hit
     * @param attackerLevel    The level of the attacker
     * @param damageDealt      Damage dealt by this single hit
     * @param monsterMaxHp     Maximum HP of the monster
     * @param monsterCurrentHp Current HP before this hit
     * @param isPartyMember    Whether attacker is in a party
     * @param isKillingBlow    Whether this hit kills the monster
     * @return EXP reward for this hit
     */
    public long calculateHitExpReward(int monsterLevel, int attackerLevel,
                                      long damageDealt, long monsterMaxHp,
                                      long monsterCurrentHp, boolean isPartyMember,
                                      boolean isKillingBlow) {

        // Base EXP from monster level (total EXP pool for the monster)
        long totalMonsterExp = (long) (BASE_EXP_MULTIPLIER * Math.pow(monsterLevel, LEVEL_SCALING_POWER) * 10);

        // Calculate damage percentage (how much HP was taken)
        double damagePercent = Math.min(1.0, (double) damageDealt / monsterMaxHp);

        // EXP for this hit based on damage contribution
        long hitExp = (long) (totalMonsterExp * damagePercent);

        // ONE-SHOT PROTECTION: If this is a killing blow and monster was at full HP
        // Give full monster EXP (monster died in single hit)
        if (isKillingBlow && monsterCurrentHp >= monsterMaxHp * 0.95) {
            hitExp = totalMonsterExp;
        }

        // Apply level difference multiplier
        double levelMultiplier = calculateLevelDifferenceMultiplier(monsterLevel, attackerLevel);

        // Apply party bonus if applicable
        double partyBonus = isPartyMember ? 1.20 : 1.0;  // 20% bonus for party

        // Final calculation
        long finalExp = (long) (hitExp * levelMultiplier * partyBonus);

        return Math.max(1, finalExp);
    }


    public long calculateKillBonusExp(int monsterLevel, int attackerLevel,
                                      long monsterMaxHp, long monsterHpBeforeKill,
                                      boolean isPartyMember) {

        // If monster was one-shot (had >95% HP), no kill bonus
        // (Full EXP already given in the hit calculation)
        if (monsterHpBeforeKill >= monsterMaxHp * 0.95) {
            return 0;
        }

        // Small bonus for finishing the monster
        long baseBonus = (long) (BASE_EXP_MULTIPLIER * Math.pow(monsterLevel, LEVEL_SCALING_POWER) * 2);

        double levelMultiplier = calculateLevelDifferenceMultiplier(monsterLevel, attackerLevel);
        double partyBonus = isPartyMember ? 1.05 : 1.0;

        return (long) (baseBonus * levelMultiplier * partyBonus);
    }

    private double calculateLevelDifferenceMultiplier(int monsterLevel, int attackerLevel) {
        int diff = monsterLevel - attackerLevel;

        // Smooth curve center
        double x = diff / 20.0;
        double smooth = Math.tanh(x);   // -1 → 1

        // Base range
        double min = 0.1;   // very low monster
        double mid = 1.0;   // same level
        double max = 1.3;   // slightly higher monster

        // Map smooth curve to base multiplier
        double multiplier = mid + smooth * (max - mid);

        // PENALTY for monster too HIGH
        if (diff >= 50) {
            double penalty = 1.0 - (diff - 50) * 0.015;
            multiplier *= Math.max(0.3, penalty);
        }

        // PENALTY for monster too LOW
        if (diff <= -50) {
            double penalty = 1.0 - (Math.abs(diff) - 50) * 0.015;
            multiplier *= Math.max(0.3, penalty);
        }

        return Math.max(0.05, multiplier);
    }

//    private double calculateLevelDifferenceMultiplier(int monsterLevel, int attackerLevel) {
//        int levelDiff = monsterLevel - attackerLevel;
//
//        if (levelDiff >= 10) {
//            // Monster much higher: 150% bonus
//            return 1.5;
//        } else if (levelDiff >= 5) {
//            // Monster higher: 120-150% (smooth curve)
//            return 1.2 + (0.3 * (levelDiff - 5) / 5.0);
//        } else if (levelDiff >= 1) {
//            // Monster slightly higher: 105-120% (smooth curve)
//            return 1.05 + (0.15 * (levelDiff - 1) / 4.0);
//        } else if (levelDiff >= -2) {
//            // Same level or slightly lower: 85-105% (smooth curve)
//            return 0.85 + (0.2 * (levelDiff + 2) / 3.0);
//        } else if (levelDiff >= -5) {
//            // Monster lower: 50-85% (smooth curve)
//            return 0.5 + (0.35 * (levelDiff + 5) / 3.0);
//        } else if (levelDiff >= -10) {
//            // Monster much lower: 10-50% (smooth curve)
//            return 0.1 + (0.4 * (levelDiff + 10) / 5.0);
//        } else {
//            // Monster way too low: 1-10% (diminishing)
//            double penalty = Math.max(0.01, 0.1 - (Math.abs(levelDiff) - 10) * 0.01);
//            return Math.max(0.01, penalty);
//        }
//    }


    public long calculateRequiredExp(int currentLevel) {
        if (currentLevel <= 0) return 100;
        if (currentLevel >= 301) return Long.MAX_VALUE;

        return (long) (BASE_REQUIRED_EXP * Math.pow(currentLevel, REQUIRED_EXP_POWER));
    }


    public long calculateCumulativeExp(int targetLevel) {
        long total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += calculateRequiredExp(i);
        }
        return total;
    }


    public int getLevelFromExp(long totalExp) {
        int level = 1;
        long expForNextLevel = calculateRequiredExp(level);

        while (totalExp >= expForNextLevel && level < 100) {
            totalExp -= expForNextLevel;
            level++;
            expForNextLevel = calculateRequiredExp(level);
        }

        return level;
    }


}