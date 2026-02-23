package game.guild;

import lombok.Data;

@Data
public class GuildRequest {
    private final String from;
    private final long createdAt;

    public GuildRequest(String from) {
        this.from = from;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return now - createdAt > 5 * 60 * 1000; // 5 minutes
    }
}
