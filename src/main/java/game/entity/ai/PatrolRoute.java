package game.entity.ai;


import game.entity.Position;
import lombok.Getter;

/**
 * Pre-built clockwise square patrol route around a spawn anchor.
 * <p>
 * Visual layout (top-down, Y increases downward):
 * <p>
 * [3] NW ──────► [0] NE
 * ▲                │
 * │                ▼
 * [2] SW ◄────── [1] SE
 * <p>
 * The bot walks:  NE → SE → SW → NW → (complete, back to IDLE)
 */
public class PatrolRoute {

    private final Position[] waypoints;

    @Getter
    private int currentIndex = 0;
    @Getter
    private int visitCount = 0;   // how many waypoints visited this lap

    public PatrolRoute(Position spawn, int radius) {
        short cx = spawn.getX();
        short cy = spawn.getY();
        short r = (short) radius;

        waypoints = new Position[]{
                new Position(spawn.getMap(), (short) (cx + r), (short) (cy - r)), // [0] NE
                new Position(spawn.getMap(), (short) (cx + r), (short) (cy + r)), // [1] SE
                new Position(spawn.getMap(), (short) (cx - r), (short) (cy + r)), // [2] SW
                new Position(spawn.getMap(), (short) (cx - r), (short) (cy - r)), // [3] NW
        };
    }

    /**
     * Current target waypoint.
     */
    public Position current() {
        return waypoints[currentIndex];
    }

    /**
     * All waypoints — for external reference checks.
     */
    public Position[] waypoints() {
        return waypoints;
    }

    /**
     * Total number of corners in the route.
     */
    public int size() {
        return waypoints.length;
    }

    /**
     * Marks the current waypoint as visited and advances clockwise.
     * visitCount is incremented so Bot can detect when the full lap is done.
     */
    public void advance() {
        visitCount++;
        currentIndex = (currentIndex + 1) % waypoints.length;
    }

    /**
     * True when all corners have been visited exactly once this lap.
     */
    public boolean isComplete() {
        return visitCount >= waypoints.length;
    }

    /**
     * Resets for a fresh lap starting at NE.
     */
    public void reset() {
        currentIndex = 0;
        visitCount = 0;
    }
}