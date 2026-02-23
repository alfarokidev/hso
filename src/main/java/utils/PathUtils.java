package utils;



import game.entity.Position;
import io.ytcode.pathfinding.astar.Path;
import io.ytcode.pathfinding.astar.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for working with Path objects from the pathfinding library
 */
public final class PathUtils {

    private static final int TILE_SIZE = 24;

    private PathUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get the point at a specific index in the path
     * @param path The path object
     * @param index The index (0 = start, size-1 = end)
     * @return The point as a long value
     */
    public static long getPoint(Path path, int index) {
        if (path == null || index < 0 || index >= path.size()) {
            throw new IndexOutOfBoundsException("Invalid path index: " + index);
        }
        return path.get(index);
    }

    /**
     * Get the X coordinate (in tiles) at a specific path index
     */
    public static int getTileX(Path path, int index) {
        long point = getPoint(path, index);
        return Point.getX(point);
    }

    /**
     * Get the Y coordinate (in tiles) at a specific path index
     */
    public static int getTileY(Path path, int index) {
        long point = getPoint(path, index);
        return Point.getY(point);
    }

    /**
     * Convert a path point to a Position object (in pixels)
     * @param mapId The map ID
     * @param path The path
     * @param index The index in the path
     * @return Position in pixel coordinates
     */
    public static Position toPosition(int mapId, Path path, int index) {
        int tileX = getTileX(path, index);
        int tileY = getTileY(path, index);

        // Convert tile coordinates to pixel coordinates (center of tile)
        int pixelX = tileX * TILE_SIZE + TILE_SIZE / 2;
        int pixelY = tileY * TILE_SIZE + TILE_SIZE / 2;

        return new Position(mapId, pixelX, pixelY);
    }

    /**
     * Convert entire path to a list of positions
     */
    public static List<Position> toPositionList(int mapId, Path path) {
        List<Position> positions = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            return positions;
        }

        for (int i = 0; i < path.size(); i++) {
            positions.add(toPosition(mapId, path, i));
        }

        return positions;
    }

    /**
     * Get the total distance of the path in pixels
     */
    public static int getPathDistance(Path path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }

        int totalDistance = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            int x1 = getTileX(path, i);
            int y1 = getTileY(path, i);
            int x2 = getTileX(path, i + 1);
            int y2 = getTileY(path, i + 1);

            int dx = (x2 - x1) * TILE_SIZE;
            int dy = (y2 - y1) * TILE_SIZE;

            totalDistance += (int) Math.sqrt(dx * dx + dy * dy);
        }

        return totalDistance;
    }

    /**
     * Check if a path is valid and not empty
     */
    public static boolean isValidPath(Path path) {
        return path != null && !path.isEmpty();
    }

    /**
     * Get the first position in the path (starting point)
     */
    public static Position getStartPosition(int mapId, Path path) {
        if (!isValidPath(path)) {
            return null;
        }
        return toPosition(mapId, path, 0);
    }

    /**
     * Get the last position in the path (destination)
     */
    public static Position getEndPosition(int mapId, Path path) {
        if (!isValidPath(path)) {
            return null;
        }
        return toPosition(mapId, path, path.size() - 1);
    }

    /**
     * Calculate remaining distance from a specific index
     */
    public static int getRemainingDistance(Path path, int fromIndex) {
        if (!isValidPath(path) || fromIndex >= path.size() - 1) {
            return 0;
        }

        int distance = 0;

        for (int i = fromIndex; i < path.size() - 1; i++) {
            int x1 = getTileX(path, i);
            int y1 = getTileY(path, i);
            int x2 = getTileX(path, i + 1);
            int y2 = getTileY(path, i + 1);

            int dx = (x2 - x1) * TILE_SIZE;
            int dy = (y2 - y1) * TILE_SIZE;

            distance += (int) Math.sqrt(dx * dx + dy * dy);
        }

        return distance;
    }

    /**
     * Simplify path by removing unnecessary waypoints (straightline optimization)
     * Useful for reducing network bandwidth when sending paths to clients
     */
    public static List<Position> simplifyPath(int mapId, Path path, int tolerance) {
        List<Position> simplified = new ArrayList<>();

        if (!isValidPath(path)) {
            return simplified;
        }

        // Always add first point
        simplified.add(toPosition(mapId, path, 0));

        if (path.size() <= 2) {
            simplified.add(toPosition(mapId, path, path.size() - 1));
            return simplified;
        }

        Position current = toPosition(mapId, path, 0);

        for (int i = 1; i < path.size() - 1; i++) {
            Position point = toPosition(mapId, path, i);
            Position next = toPosition(mapId, path, i + 1);

            // Check if point is significant (changes direction significantly)
            if (isSignificantPoint(current, point, next, tolerance)) {
                simplified.add(point);
                current = point;
            }
        }

        // Always add last point
        simplified.add(toPosition(mapId, path, path.size() - 1));

        return simplified;
    }

    private static boolean isSignificantPoint(Position prev, Position current, Position next, int tolerance) {
        // Calculate perpendicular distance from point to line
        int dx = next.getX() - prev.getX();
        int dy = next.getY() - prev.getY();

        if (dx == 0 && dy == 0) {
            return true;
        }

        int numerator = Math.abs(dy * current.getX() - dx * current.getY() +
                next.getX() * prev.getY() - next.getY() * prev.getX());
        double denominator = Math.sqrt(dx * dx + dy * dy);

        double distance = numerator / denominator;

        return distance > tolerance;
    }
}