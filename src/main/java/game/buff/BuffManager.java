package game.buff;

import game.entity.base.LivingEntity;
import game.stat.StatType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manages all buffs for a living entity
 */
@Slf4j
public class BuffManager {
    private final LivingEntity owner;
    private final ConcurrentHashMap<Integer, BuffEffect> activeBuffs;

    public BuffManager(LivingEntity owner) {
        this.owner = owner;
        this.activeBuffs = new ConcurrentHashMap<>();
    }


    public void applyBuff(BuffEffect buff) {
        BuffEffect existing = activeBuffs.get(buff.getId());

        if (existing != null) {
            // Refresh duration if same buff
            existing.refresh();
            log.debug("Refreshed buff {} on {}", buff.getId(), owner.getId());
        } else {
            // Add new buff
            activeBuffs.put(buff.getId(), buff);
            owner.recalculateStats(); // Recalc with new buff
            log.debug("Applied buff {} to {}", buff.getId(), owner.getId());
        }
    }

    public void removeBuff(int buffId) {
        BuffEffect removed = activeBuffs.remove(buffId);
        if (removed != null) {
            owner.recalculateStats(); // Recalc without buff
            log.debug("Removed buff {} from {}", removed.getId(), owner.getId());
        }
    }


    public void clearAllBuffs() {
        activeBuffs.clear();
        owner.recalculateStats();
        log.debug("Cleared all buffs from {}", owner.getId());
    }

    public void update() {
        List<Integer> expired = new ArrayList<>();

        for (BuffEffect buff : activeBuffs.values()) {
            if (buff.isExpired()) {
                expired.add(buff.getId());
            }
        }

        expired.forEach(this::removeBuff);
    }


    public boolean hasBuff(int buffId) {
        return activeBuffs.containsKey(buffId);
    }

    public int getStatBonus(StatType statType) {
        return activeBuffs.values().stream()
                .mapToInt(buff -> buff.getStatModifiers().getOrDefault(statType, 0))
                .sum();
    }

}