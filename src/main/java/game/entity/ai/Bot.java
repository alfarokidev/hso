package game.entity.ai;

import game.entity.Position;
import game.entity.base.LivingEntity;
import game.entity.monster.MonsterEntity;
import game.entity.player.PlayerEntity;
import game.map.PathFinding;
import game.skill.SkillEntity;
import game.stat.StatCalculator;
import io.ytcode.pathfinding.astar.Path;
import io.ytcode.pathfinding.astar.Point;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;

import java.util.Comparator;
import java.util.List;

/**
 * Bot AI controller for LivingEntity (monsters & clones).
 * <p>
 * State machine:
 * <p>
 * IDLE   ──► enemy detected                    ──► CHASE
 * IDLE   ──► no enemy after PATROL_DELAY        ──► PATROL
 * PATROL ──► enemy detected mid-patrol          ──► CHASE
 * PATROL ──► all 4 waypoints visited            ──► IDLE
 * CHASE  ──► in attack range                    ──► ATTACK
 * CHASE  ──► target lost / off-spawn            ──► RETURN
 * ATTACK ──► target out of range                ──► CHASE
 * ATTACK ──► target dead / left map             ──► RETURN
 * RETURN ──► reached spawn                      ──► IDLE
 */
@Slf4j
@Getter
public class Bot {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final long MOVE_DELAY = 500L;
    private static final long IDLE_SCAN_DELAY = 1500L;  // ms between scans in IDLE/PATROL
    private static final long PATROL_DELAY = 1500L;  // ms idle before patrol starts
    private static final int DETECTION_RANGE = 142;
    private static final int ATTACK_RANGE = 120;    // 5 tiles
    private static final int TILE_SIZE = 24;
    private static final int ARRIVE_THRESHOLD = TILE_SIZE;
    private static final int PATROL_RADIUS = 48;     // 2 tiles from spawn

    // ── Core references ───────────────────────────────────────────────────────
    private final PlayerEntity entity;
    private final PathFinding pathFinding;
    private final Position spawnLocation;
    private final PatrolRoute patrolRoute;    // pre-built clockwise waypoints
    private SkillSequence skillSequence;
    // ── Runtime state ─────────────────────────────────────────────────────────
    private BotState currentState = BotState.IDLE;
    private LivingEntity target;
    private Path currentPath;
    private int pathIndex;
    private long lastMoveTime;
    private long lastScanTime;
    private long idleSinceTime;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Bot(PlayerEntity entity) {
        this.entity = entity;
        this.pathFinding = entity.getMap().getPathFinding();
        this.spawnLocation = entity.getPosition().copy();
        this.patrolRoute = new PatrolRoute(spawnLocation, PATROL_RADIUS);
        this.idleSinceTime = System.currentTimeMillis();
        setupSkill();
    }


    // ── Main tick ─────────────────────────────────────────────────────────────

    public void update() {
        if (entity.isClone() && entity.isDead()) {
            WorldManager.getInstance().leaveMap(entity);
            return;
        }

        switch (currentState) {
            case IDLE -> onIdle();
            case PATROL -> onPatrol();
            case CHASE -> onChase();
            case ATTACK -> onAttack();
            case RETURN -> onReturn();
        }
    }

    // ── State: IDLE ───────────────────────────────────────────────────────────

    private void onIdle() {
        if (isOffSpawn(entity.getPosition())) {
            returnToSpawn();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastScanTime < IDLE_SCAN_DELAY) return;
        lastScanTime = now;

        target = findTarget();
        if (target != null) {
            transitionTo(BotState.CHASE);
            return;
        }

        // No enemy found — begin clockwise patrol after delay
        if (now - idleSinceTime >= PATROL_DELAY) {
            patrolRoute.reset();
            beginWalkToWaypoint();
        }
    }

    // ── State: PATROL ─────────────────────────────────────────────────────────

    private void onPatrol() {
        // Scan for enemies while walking
        long now = System.currentTimeMillis();
        if (now - lastScanTime >= IDLE_SCAN_DELAY) {
            lastScanTime = now;
            target = findTarget();
            if (target != null) {
                currentPath = null;
                transitionTo(BotState.CHASE);
                return;
            }
        }

        // Path exhausted → waypoint reached
        if (currentPath == null) {
            patrolRoute.advance();

            // All 4 corners visited — return to IDLE
            if (isPatrolComplete()) {
                idleSinceTime = System.currentTimeMillis();
                transitionTo(BotState.IDLE);
                return;
            }

            // Walk to the next corner
            beginWalkToWaypoint();
            return;
        }

        advanceOnePath();
    }

    // ── State: CHASE ──────────────────────────────────────────────────────────

    private void onChase() {
        if (!isValidTarget() || isOffSpawn(entity.getPosition())) {
            returnToSpawn();
            return;
        }

        if (entity.distanceTo(target) <= ATTACK_RANGE) {
            transitionTo(BotState.ATTACK);
            return;
        }

        if (currentPath == null) {
            currentPath = findPath(target.getPosition());
            pathIndex = 0;
            if (currentPath == null) {
                returnToSpawn();
                return;
            }
        }

        advanceOnePath();
    }

    // ── State: ATTACK ─────────────────────────────────────────────────────────

    private void onAttack() {
        if (!isValidTarget() || isOffSpawn(entity.getPosition())) {
            returnToSpawn();
            return;
        }

        if (entity.distanceTo(target) > ATTACK_RANGE) {
            currentPath = null;
            transitionTo(BotState.CHASE);
            return;
        }

        if (entity.canAttack()) {
            SkillEntity skill = skillSequence.next(entity.getSkillData());
            if (skill != null) {
                if (entity.getMp() < skill.getCurrentLevelData().mpCost) {
                    entity.setMp(entity.getMaxMp());
                }
                entity.useSkill(skill.getSkillId(), target);

            }
        }
    }

    // ── State: RETURN ─────────────────────────────────────────────────────────

    private void onReturn() {
        if (entity.getPosition().distanceTo(spawnLocation) <= ARRIVE_THRESHOLD) {
            snapToSpawn();
            reset();
            return;
        }

        if (currentPath == null) {
            currentPath = findPath(spawnLocation);
            pathIndex = 0;
            if (currentPath == null) {
                snapToSpawn();
                reset();
                return;
            }
        }

        advanceOnePath();
    }

    // ── Patrol helpers ────────────────────────────────────────────────────────

    /**
     * Builds a path to the current patrol waypoint and enters PATROL state.
     * If the waypoint is unreachable, skip it and advance to the next one.
     */
    private void beginWalkToWaypoint() {
        Position waypoint = patrolRoute.current();
        currentPath = findPath(waypoint);
        pathIndex = 0;

        if (currentPath == null) {
            // Waypoint blocked — skip to next corner silently
            patrolRoute.advance();
            if (!isPatrolComplete()) {
                beginWalkToWaypoint();  // try next
            } else {
                idleSinceTime = System.currentTimeMillis();
                transitionTo(BotState.IDLE);
            }
            return;
        }

        transitionTo(BotState.PATROL);
    }

    /**
     * Delegates to PatrolRoute — true once all 4 corners have been visited this lap.
     */
    private boolean isPatrolComplete() {
        return patrolRoute.isComplete();
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    private void advanceOnePath() {
        long now = System.currentTimeMillis();
        if (now - lastMoveTime < MOVE_DELAY) return;
        lastMoveTime = now;

        if (pathIndex >= currentPath.size()) {
            currentPath = null;  // signal waypoint reached
            return;
        }

        long point = currentPath.get(pathIndex++);
        short px = (short) (Point.getX(point) * TILE_SIZE);
        short py = (short) (Point.getY(point) * TILE_SIZE);

        entity.setPosition(px, py);
        entity.broadcastMovement();
    }

    private void snapToSpawn() {
        entity.setPosition(spawnLocation.getX(), spawnLocation.getY());
        entity.broadcastMovement();
    }

    // ── Target search ─────────────────────────────────────────────────────────

    public LivingEntity findTarget() {
        return entity.isClone() ? findPlayer() : findMonster();
    }

    public LivingEntity findPlayer() {
        List<PlayerEntity> candidates = entity.getZone().getPlayersInRadius(entity, DETECTION_RANGE);

        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;

        for (LivingEntity enemy : candidates) {
            if (!isEligibleTarget(enemy)) continue;
            if (isSameGuild(enemy)) continue;

            double dist = entity.distanceTo(enemy);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        }
        return nearest;
    }

    public LivingEntity findMonster() {
        List<MonsterEntity> candidates = entity.getZone().getMonstersInRadius(entity, DETECTION_RANGE);

        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;

        for (LivingEntity enemy : candidates) {
            if (!isEligibleTarget(enemy)) continue;
            double dist = entity.distanceTo(enemy);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        }
        return nearest;
    }

    private boolean isEligibleTarget(LivingEntity enemy) {
        if (enemy.isDead()) return false;
        if (isOffSpawn(enemy.getPosition())) return false;
        return pathFinding.isReachable(entity.getPosition(), enemy.getPosition());
    }

    private boolean isSameGuild(LivingEntity enemy) {
        if (!entity.isClone()) return false;
        if (entity.getGuild() == null || enemy.getGuild() == null) return false;
        return entity.getGuild().getId() == enemy.getGuild().getId();
    }

    // ── Skill selection ───────────────────────────────────────────────────────

    public SkillEntity getBestSkill() {
        SkillEntity best = null;
        int bestPriority = -1;

        for (SkillEntity skill : entity.getSkillData().values()) {
            if (skill == null || skill.getType() != 0) continue;
            if (skill.getCurrentLevel() <= 0 || skill.isOnCooldown()) continue;

            var lv = skill.getCurrentLevelData();
            if (lv == null || lv.mpCost > entity.getMp()) continue;

            int priority = (skill.getCurrentLevel() * 1000) + StatCalculator.calculateSkillDamage(0, skill);
            if (priority > bestPriority) {
                bestPriority = priority;
                best = skill;
            }
        }

        log.debug("selectBestSkill: found={}, priority={}",
                best != null ? best.getSkillId() : "NONE", bestPriority);

        return best;
    }


    public void setupSkill() {
        skillSequence = entity.getSkillData().values().stream()
                .filter(this::isValidSkill)
                .sorted(Comparator.comparingInt(this::skillPriority).reversed())
                .collect(SkillSequence::new,
                        (seq, skill) -> seq.add(skill.getSkillId(), "Lv" + skill.getCurrentLevel()),
                        (a, b) -> {
                        });   // no parallel merge needed

        log.debug("[Bot] Skill sequence built: {}", skillSequence.getSequence());
    }

    private boolean isValidSkill(SkillEntity skill) {
        if (skill == null || skill.getType() != 0) return false;
        if (skill.getCurrentLevel() <= 0) return false;
        var lv = skill.getCurrentLevelData();
        return lv != null;
    }

    private int skillPriority(SkillEntity skill) {
        return (skill.getCurrentLevel() * 1000) + StatCalculator.calculateSkillDamage(0, skill);
    }

    // ── Pathfinding ───────────────────────────────────────────────────────────

    private Path findPath(Position destination) {
        if (destination == null) return null;
        if (!pathFinding.isReachable(entity.getPosition(), destination)) return null;

        Path path = pathFinding.find(entity.getPosition(), destination);
        return (path != null && !path.isEmpty()) ? path : null;
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    private boolean isOffSpawn(Position pos) {
        return spawnLocation.distanceTo(pos) > DETECTION_RANGE;
    }

    private boolean isValidTarget() {
        if (target == null) return false;
        if (target.isDead()) return false;
        return target.getPosition().isSameMap(entity.getPosition());
    }

    // ── Transitions ───────────────────────────────────────────────────────────

    private void returnToSpawn() {
        target = null;
        currentPath = null;
        pathIndex = 0;
        transitionTo(BotState.RETURN);
    }

    private void reset() {
        target = null;
        currentPath = null;
        pathIndex = 0;
        lastMoveTime = 0;
        idleSinceTime = System.currentTimeMillis();
        transitionTo(BotState.IDLE);
    }

    private void transitionTo(BotState next) {
        currentState = next;
    }
}