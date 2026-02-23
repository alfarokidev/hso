package game.event;

import lombok.Data;

@Data
public class EventSchedule {
    private final GameEvent event;
    private final long startTime;
    private final long endTime;
    private boolean active;
    private boolean started;

    public EventSchedule(GameEvent event, long startTime, long endTime) {
        if (startTime >= endTime) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.event = event;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = false;
        this.started = false;
    }

    public boolean shouldStart(long currentTime) {
        return !started && currentTime >= startTime && currentTime < endTime;
    }

    public boolean shouldEnd(long currentTime) {
        return active && currentTime >= endTime;
    }

    public void start() {
        if (!started) {
            started = true;
            active = true;
            event.onStart();
        }
    }

    public void end() {
        if (active) {
            active = false;
            event.onEnd();
        }
    }
    public boolean hasStarted() {
        return started;
    }

    public long getRemainingTime(long currentTime) {
        return active ? Math.max(0, endTime - currentTime) : 0;
    }
}