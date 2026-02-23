package game.entity;

import game.map.PathFinding;
import io.ytcode.pathfinding.astar.Path;
import io.ytcode.pathfinding.astar.Point;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class Move {

    private final PathFinding pathFinding;

    private Path path;
    private int pathIndex;

    // position IN TILES
    private int tileX, tileY;

    // target tile
    private int targetTileX, targetTileY;

    private boolean moving;

    private static final int TILE_SIZE = 24;

    public Move(PathFinding pathFinding) {
        this.pathFinding = pathFinding;
    }

    // -------------------------------------------------
    // START MOVE
    // -------------------------------------------------
    public boolean startMove(Position start, Position dest) {
        path = pathFinding.find(start, dest);

        if (path == null || path.isEmpty()) {
            moving = false;
            return false;
        }

        // convert start PIXEL -> TILE
        this.tileX = start.getTileX();
        this.tileY = start.getTileY();

        pathIndex = 0;
        setNextWaypoint();
        moving = true;
        return true;
    }

    // -------------------------------------------------
    // LOAD NEXT WAYPOINT
    // -------------------------------------------------
    private void setNextWaypoint() {
        if (pathIndex >= path.size()) {
            moving = false;
            return;
        }

        long p = path.get(pathIndex++);
        targetTileX = Point.getX(p);
        targetTileY = Point.getY(p);
    }

    // -------------------------------------------------
    // UPDATE — 1 TILE PER CALL (FAST)
    // -------------------------------------------------
    public void update() {
        if (!moving) return;

        int dx = targetTileX - tileX;
        int dy = targetTileY - tileY;

        // reached this node
        if (dx == 0 && dy == 0) {
            setNextWaypoint();
            return;
        }

        // step direction
        int stepX = Integer.compare(dx, 0); // -1,0,1
        int stepY = Integer.compare(dy, 0); // -1,0,1

        // MOVE EXACTLY ONE TILE
        tileX += stepX;
        tileY += stepY;
    }

    // -------------------------------------------------
    // PIXEL POSITION FOR ENTITY
    // -------------------------------------------------
    public int getPixelX() {
        return tileX * TILE_SIZE + TILE_SIZE / 2;
    }

    public int getPixelY() {
        return tileY * TILE_SIZE + TILE_SIZE / 2;
    }

    // -------------------------------------------------
    // STOP
    // -------------------------------------------------
    public void stop() {
        moving = false;
        path = null;
        pathIndex = 0;
    }
}
