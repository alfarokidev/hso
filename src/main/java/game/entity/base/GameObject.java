package game.entity.base;

import game.guild.Guild;
import game.map.GameMap;
import game.map.Zone;
import lombok.Data;
import game.entity.Position;

@Data
public abstract class GameObject {
    protected int id;
    protected int idCopy;
    protected String name;
    protected Position position;
    protected GameObjectType type;
    protected boolean isMoveOut;
    // Spatial management
    protected GameMap map;
    protected Zone zone;
    protected Guild guild;

    // State flags
    protected boolean isActive = true;
    protected boolean isVisible = true;

    // Timestamps
    protected long createdAt;
    protected long lastUpdateTime;

    // Default constructor for Lombok
    protected GameObject() {
        this.createdAt = System.currentTimeMillis();
        this.lastUpdateTime = createdAt;
    }

    protected GameObject(GameObjectType type) {
        this();
        this.type = type;
    }

    // Core lifecycle methods
    public abstract void onSpawn();

    public abstract void onUpdate(long deltaTime);

    public abstract void onDestroy();

    public void setPosition(short x, short y) {
        if (this.position == null) {
            setPosition(new Position(x, y));
        } else {
            this.position.setX(x);
            this.position.setY(y);
        }
    }


    public int getMapId() {
        return map.getId();
    }

    public short getX() {
        return position != null ? position.getX() : 0;
    }

    public short getY() {
        return position != null ? position.getY() : 0;
    }

    // Spatial methods
    public void setLocation(Position position, GameMap map, Zone zone) {
        setPosition(position);
        this.map = map;
        this.zone = zone;
    }

    public double distanceTo(GameObject other) {
        if (other == null || other.position == null || this.position == null) {
            return Integer.MAX_VALUE;
        }
        return position.distanceTo(other.position);
    }


}
