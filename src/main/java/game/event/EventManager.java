package game.event;

import game.event.btf.BTF;
import game.event.task.DailyAbsent;
import game.event.task.DailyTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class EventManager {
    private final Map<Integer, GameEvent> events = new ConcurrentHashMap<>();
    private final Map<String, DailyTask> dailyResets = new ConcurrentHashMap<>();

    @Getter
    private LocalDate lastResetDate;

    private EventManager() {
        this.lastResetDate = LocalDate.now();
        setup();
    }

    public static EventManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final EventManager INSTANCE = new EventManager();
    }

    public void setup() {
        DailyAbsent dailyAbsent = new DailyAbsent();
        registerEvent(dailyAbsent);

        registerDailyReset("Daily Reset", dailyAbsent::reset);

        BTF btf = new BTF();
        registerEvent(btf);
    }

    public void registerEvent(GameEvent event) {
        events.put(event.getId(), event);
        log.info("Event registered: {}", event.getName());
    }

    public void registerDailyReset(String taskId, Runnable resetAction) {
        dailyResets.put(taskId, new DailyTask(taskId, resetAction));
        log.debug("Registered daily reset task: {}", taskId);
    }

    public void cancelEvent(int eventId) {
        GameEvent event = events.get(eventId);
        if (event != null) {
            event.cancel();
        }
    }

    public void onUpdate(long currentTime) {
        checkDailyReset();

        events.values().forEach(event -> {
            if (event.shouldStart(currentTime)) {
                ((BaseEvent) event).start();
            } else if (event.shouldEnd(currentTime)) {
                ((BaseEvent) event).end();
            } else if (event.isActive()) {
                event.onUpdate(currentTime);
            }
        });
    }

    private void checkDailyReset() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(lastResetDate)) {
            executeDailyReset();
            lastResetDate = today;
        }
    }

    private void executeDailyReset() {
        dailyResets.values().forEach(task -> {
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Daily reset failed for task: {}", task.getId(), e);
            }
        });
    }

    public void forceReset() {
        log.warn("Force executing daily reset");
        executeDailyReset();
        lastResetDate = LocalDate.now();
    }

    @SuppressWarnings("unchecked")
    public <T extends GameEvent> T getEvent(Class<T> clazz) {
        for (GameEvent event : events.values()) {
            if (clazz.isInstance(event)) {
                return (T) event;
            }
        }
        return null;
    }
}