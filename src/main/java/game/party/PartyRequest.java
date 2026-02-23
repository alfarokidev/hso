package game.party;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartyRequest {
    private final String from;
    private final long createdAt;

    public PartyRequest(String from) {
        this.from = from;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return now - createdAt > 5 * 60 * 1000; // 5 minutes
    }
}
