package game.entity.base;

public enum GameObjectType {
    PLAYER(0),
    MONSTER(1),
    NPC(2),
    ITEM(3);

    public final int code;

    GameObjectType(int code) {
        this.code = code;
    }
}
