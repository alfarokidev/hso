package model.contest;

import game.entity.Position;
import game.entity.player.PlayerEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContestParticipant {
    private final PlayerEntity player;
    private int totalWins;
    private int totalLosses;
    private int currentRoundWins;
    private int arenaPointsEarned;
    
    public ContestParticipant(PlayerEntity player) {
        this.player = player;
        this.totalWins = 0;
        this.totalLosses = 0;
        this.currentRoundWins = 0;
        this.arenaPointsEarned = 0;
    }
    
    public void addWin() {
        totalWins++;
        currentRoundWins++;
    }
    
    public void addLoss() {
        totalLosses++;
    }
    
    public void addArenaPoints(int points) {
        arenaPointsEarned += points;
    }
    
    public void resetRoundWins() {
        currentRoundWins = 0;
    }
    
    public double getWinRate() {
        int totalMatches = totalWins + totalLosses;
        return totalMatches > 0 ? (double) totalWins / totalMatches : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("%s (Level %d, %d-%d)", 
            player.getName(), player.getLevel(), totalWins, totalLosses);
    }
}