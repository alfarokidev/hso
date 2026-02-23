package game.event;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseEvent implements GameEvent {
    @Getter
    protected int id;
    @Getter
    protected String name;

    private long startTime;
    private long endTime;
    private boolean scheduled;
    private boolean active;

    protected BaseEvent(int id, String name) {
        this.id = id;
        this.name = name;
        this.scheduled = false;
        this.active = false;
    }

    @Override
    public void schedule(long startTime, long endTime) {
        if (startTime >= endTime) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduled = true;

        log.debug("Event scheduled: {} from {} to {}", name, startTime, endTime);
    }

    @Override
    public void cancel() {
        if (active) {
            onEnd();
        }

        scheduled = false;
        active = false;

        log.info("Event cancelled: {}", name);
    }

    @Override
    public boolean isScheduled() {
        return scheduled;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean shouldStart(long currentTime) {
        return scheduled && !active && currentTime >= startTime && currentTime < endTime;
    }

    @Override
    public boolean shouldEnd(long currentTime) {
        return active && currentTime >= endTime;
    }

    public void start() {
        if (!active) {
            active = true;
            onStart();
            log.info("Event started: {}", name);
        }
    }

    public void end() {
        if (active) {
            active = false;
            onEnd();
            log.info("Event ended: {}", name);
        }
    }

}