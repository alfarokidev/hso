package game.entity.ai;


import game.skill.SkillEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages an ordered skill rotation for Bot AI.
 *
 * <p>Sequence example:
 * <pre>
 *   Slot 0 → Skill A (opener)
 *   Slot 1 → Skill B (combo)
 *   Slot 2 → Skill C (finisher)
 *   Slot 3 → Skill A (repeat)
 * </pre>
 * <p>
 * Rules:
 * - Advances to next slot only when current skill is usable.
 * - Skips a slot (without advancing the cursor) if skill is on cooldown.
 * - Falls back to slot 0 if nothing is usable.
 */
@Slf4j
public class SkillSequence {

    public record SkillSlot(byte skillId, String label) {
    }

    private final List<SkillSlot> sequence = new ArrayList<>();
    @Getter
    private int cursor = 0;

    // ── Builder ───────────────────────────────────────────────────────────────

    public SkillSequence add(byte skillId, String label) {
        sequence.add(new SkillSlot(skillId, label));
        return this;
    }

    public SkillSequence add(byte skillId) {
        return add(skillId, "Skill#" + skillId);
    }

    // ── Core ──────────────────────────────────────────────────────────────────

    /**
     * Returns the next usable skill in the rotation,
     * or {@code null} if nothing is ready.
     *
     * @param skillMap the entity's skill data (skillId → SkillEntity)
     */
    public SkillEntity next(Map<Byte, SkillEntity> skillMap) {
        if (sequence.isEmpty()) return null;

        int attempts = 0;
        int size = sequence.size();

        while (attempts < size) {
            SkillSlot slot = sequence.get(cursor);
            SkillEntity skill = skillMap.get(slot.skillId());

            if (isUsable(skill)) {
                advance();
                log.debug("[SkillSequence] Using slot {} → {} (id={})",
                        cursor == 0 ? size - 1 : cursor - 1, slot.label(), slot.skillId());
                return skill;
            }

            // Skill not ready — try next slot without committing the cursor
            attempts++;
            cursor = (cursor + 1) % size;
        }

        log.debug("[SkillSequence] No skill ready in rotation.");
        return null;
    }

    /**
     * Resets the rotation back to slot 0 (e.g. on new combat engagement).
     */
    public void reset() {
        cursor = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isUsable(SkillEntity skill) {
        if (skill == null) return false;
        if (skill.getType() != 0) return false;
        if (skill.getCurrentLevel() <= 0) return false;
        if (skill.isOnCooldown()) return false;
        var lv = skill.getCurrentLevelData();
        return lv != null;  // mp check stays in Bot (entity context needed)
    }

    private void advance() {
        cursor = (cursor + 1) % sequence.size();
    }

    public List<SkillSlot> getSequence() {
        return List.copyOf(sequence);
    }
}