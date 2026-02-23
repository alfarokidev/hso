package game.event;


public interface GameEvent {
    int getId();
    String getName();

    void onStart();
    void onUpdate(long currentTime);
    void onEnd();

    // Schedule management
    void schedule(long startTime, long endTime);
    void cancel();

    boolean isScheduled();
    boolean isActive();
    boolean shouldStart(long currentTime);
    boolean shouldEnd(long currentTime);
}