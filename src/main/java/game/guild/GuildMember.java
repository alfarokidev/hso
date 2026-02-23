package game.guild;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import static game.guild.GuildRank.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuildMember {
    private int playerId;
    private String playerName;
    private GuildRank rank;
    private long joinedAt;
    private int contributionPoints;
    private long lastOnline;
    private long goldContribution;
    private int gemContribution;

    public GuildMember(int playerId, String playerName, GuildRank rank) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.rank = rank;
        this.joinedAt = System.currentTimeMillis();
        this.lastOnline = System.currentTimeMillis();
        this.contributionPoints = 0;
    }

    public void addContribution(int points) {
        this.contributionPoints += points;
    }

    public boolean hasPermission(GuildPermission permission) {
        return rank.hasPermission(permission);
    }

    public boolean spendCP(int amount) {
        if (contributionPoints >= amount) {
            contributionPoints -= amount;
            return true;
        }
        return false;
    }

    public int getPosition() {
        return switch (rank) {
            case LEADER -> 127;
            case DEPUTY -> 126;
            case OFFICER -> 125;
            case ELITE -> 124;
            case MEMBER -> 123;
            case RECRUIT -> 122;
        };
    }

    public void setPosition(int type) {
        switch (type) {
            case 127 -> rank = LEADER;
            case 126 -> rank = DEPUTY;
            case 125 -> rank = OFFICER;
            case 124 -> rank = ELITE;
            case 123 -> rank = MEMBER;
            case 122 -> rank = RECRUIT;
        }

    }
}