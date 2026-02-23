package game.event.task;


import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DailyTask {
    private final String id;
    private final Runnable resetAction;
    private LocalDateTime lastExecuted;

    public DailyTask(String id, Runnable resetAction) {
        this.id = id;
        this.resetAction = resetAction;
        this.lastExecuted = null;
    }

    public void execute() {
        resetAction.run();
        lastExecuted = LocalDateTime.now();
    }
}