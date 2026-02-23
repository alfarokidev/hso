package game.entity;

import game.map.PathFinding;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Data
public class Position {
    private transient final int TILE_PIXEL = 24;
    private int map;
    private short x;
    private short y;

    public Position(short x, short y) {
        this.x = x;
        this.y = y;
    }

    public Position(int x, int y) {
        this.x = (short) x;
        this.y = (short) y;
    }

    public Position(int map, short x, short y) {
        this.map = map;
        this.x = x;
        this.y = y;
    }

    public Position(int map, int x, int y) {
        this.map = map;
        this.x = (short) x;
        this.y = (short) y;
    }

    public double distanceTo(Position other) {
        if (other == null) {
            return Integer.MAX_VALUE;
        }

        int dx = this.x - (int) other.x;
        int dy = this.y - (int) other.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    public int getTileX() {
        return x / TILE_PIXEL;
    }

    public int getTileY() {
        return y / TILE_PIXEL;
    }

    public void setTilePosition(int xTile, int yTile) {
        x = (short) (xTile * TILE_PIXEL);
        y = (short) (yTile * TILE_PIXEL);
        log.debug("setTilePosition: tile ({},{}) -> pixel ({},{})", xTile, yTile, x, y);
    }


    public Position add(short dx, short dy) {
        return new Position((short) (this.x + dx), (short) (this.y + dy));
    }

    public Position copy() {
        return new Position(this.map, this.x, this.y);
    }

    public Position getAroundPosition(PathFinding pathFinding, int range) {
        final int maxAttempts = 10;

        int centerX = getTileX();
        int centerY = getTileY();

        int minX = centerX - range;
        int maxX = centerX + range;

        int minY = centerY - range;
        int maxY = centerY + range;

        for (int i = 0; i < maxAttempts; i++) {

            int randomX = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int randomY = ThreadLocalRandom.current().nextInt(minY, maxY + 1);

            Position candidate = new Position(
                    getMap(),
                    randomX * TILE_PIXEL,
                    randomY * TILE_PIXEL
            );

            if (pathFinding.isReachable(this, candidate)) {
                return candidate;
            }
        }

        // fallback → return current position if no valid tile found
        return this;
    }


    public boolean isSameMap(Position target) {
        return map == target.getMap();
    }
}