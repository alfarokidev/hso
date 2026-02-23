package model.contest;

import game.entity.player.PlayerEntity;
import game.map.Zone;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import service.NetworkService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
@Setter
public class ContestMatch {
    private final String id;
    private final ContestParticipant participant1;
    private final ContestParticipant participant2;
    private final int currentRound;
    
    private Zone arena;
    private MatchState state;
    private ContestParticipant winner;
    private ContestParticipant loser;
    
    private long startTime;
    private long endTime;
    private final AtomicInteger roundsCompleted = new AtomicInteger(0);
    
    private static final int MAX_ROUNDS = 3;
    private static final long MATCH_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes per match
    
    public enum MatchState {


        WAITING,
        STARTING,
        IN_PROGRESS,
        FINISHED,
        CANCELLED
    }
    
    public ContestMatch(ContestParticipant p1, ContestParticipant p2, int round) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.participant1 = p1;
        this.participant2 = p2;
        this.currentRound = round;
        this.state = MatchState.WAITING;
    }
    
    public boolean canStart() {
        return state == MatchState.WAITING && 
               participant1.getPlayer().isOnline() && 
               participant2.getPlayer().isOnline();
    }
    
    public void start() {
        if (!canStart()) {
            log.warn("Cannot start match {}: players not ready", id);
            return;
        }
        
        state = MatchState.STARTING;
        startTime = System.currentTimeMillis();
        
        // Prepare players for combat
        preparePlayer(participant1.getPlayer());
        preparePlayer(participant2.getPlayer());
        
        // Start first round
        startNextRound();
        
        log.info("Match {} started: {} vs {}", id, 
            participant1.getPlayer().getName(), 
            participant2.getPlayer().getName());
    }
    
    private void preparePlayer(PlayerEntity player) {
        // Restore full HP/MP
        player.setHp(player.getMaxHp());
        player.setMp(player.getMaxMp());
        
        // Clear any existing buffs/debuffs
//        if (player.getBuffManager() != null) {
//            player.getBuffManager().clearAll();
//        }
        
        // Set PK mode for contest
        player.setTypePK((byte) 1); // Enable PK mode
        
        // Notify player
        NetworkService.gI().sendToast(player,
            "Match starting! Best of 3 rounds. Good luck!");
    }
    
    private void startNextRound() {
        int round = roundsCompleted.get() + 1;
        
        if (round > MAX_ROUNDS) {
            finishMatch();
            return;
        }
        
        state = MatchState.IN_PROGRESS;
        
        // Reset player positions for new round
        resetPlayerPositions();
        
        // Notify players
        PlayerEntity p1 = participant1.getPlayer();
        PlayerEntity p2 = participant2.getPlayer();
        
        NetworkService.gI().sendToast(p1, "Round " + round + " - Fight!");
        NetworkService.gI().sendToast(p2, "Round " + round + " - Fight!");
        
        log.info("Match {} round {} started", id, round);
    }
    
    private void resetPlayerPositions() {
        if (arena != null) {
            PlayerEntity p1 = participant1.getPlayer();
            PlayerEntity p2 = participant2.getPlayer();
            
            // Position players at opposite ends of arena
            p1.setPosition((short) 0, (short) 0);
            p2.setPosition((short) 5, (short) 5);
            
            // Restore HP/MP for new round
            p1.setHp(p1.getMaxHp());
            p1.setMp(p1.getMaxMp());
            p2.setHp(p2.getMaxHp());
            p2.setMp(p2.getMaxMp());
            
            // Clear buffs
//            if (p1.getBuffManager() != null) p1.getBuffManager().clearAll();
//            if (p2.getBuffManager() != null) p2.getBuffManager().clearAll();
        }
    }
    
    public void onPlayerDeath(PlayerEntity deadPlayer) {
        if (state != MatchState.IN_PROGRESS) {
            return;
        }
        
        ContestParticipant deadParticipant = getParticipant(deadPlayer);
        ContestParticipant aliveParticipant = getOpponent(deadParticipant);
        
        if (deadParticipant == null || aliveParticipant == null) {
            log.warn("Invalid participants in match {}", id);
            return;
        }
        
        // Award round win
        int currentRounds = roundsCompleted.incrementAndGet();
        
        // Check if match is decided (best of 3)
        int p1Wins = getRoundWins(participant1);
        int p2Wins = getRoundWins(participant2);
        
        if (p1Wins >= 2) {
            winner = participant1;
            loser = participant2;
            finishMatch();
        } else if (p2Wins >= 2) {
            winner = participant2;
            loser = participant1;
            finishMatch();
        } else if (currentRounds < MAX_ROUNDS) {
            // Continue to next round
            startNextRound();
        } else {
            // All rounds completed, determine winner by total rounds won
            if (p1Wins > p2Wins) {
                winner = participant1;
                loser = participant2;
            } else if (p2Wins > p1Wins) {
                winner = participant2;
                loser = participant1;
            } else {
                // Tie - determine by remaining HP
                if (participant1.getPlayer().getHp() > participant2.getPlayer().getHp()) {
                    winner = participant1;
                    loser = participant2;
                } else {
                    winner = participant2;
                    loser = participant1;
                }
            }
            finishMatch();
        }
        
        log.info("Round {} completed in match {}: {} defeated {}", 
            currentRounds, id, aliveParticipant.getPlayer().getName(), deadPlayer.getName());
    }
    
    private int getRoundWins(ContestParticipant participant) {
        // This would be tracked per round, simplified for now
        return participant == winner ? roundsCompleted.get() : 0;
    }
    
    private void finishMatch() {
        state = MatchState.FINISHED;
        endTime = System.currentTimeMillis();
        
        // Reset PK mode
        participant1.getPlayer().setTypePK((byte) 0);
        participant2.getPlayer().setTypePK((byte) 0);
        
        // Announce result
        if (winner != null && loser != null) {
            NetworkService.gI().sendToast(winner.getPlayer(),
                "Victory! You won the match " + getRoundWins(winner) + "-" + getRoundWins(loser));
            NetworkService.gI().sendToast(loser.getPlayer(),
                "Defeat! You lost the match " + getRoundWins(loser) + "-" + getRoundWins(winner));
            
            log.debug("Match {} finished: {} defeated {}", id,
                winner.getPlayer().getName(), loser.getPlayer().getName());
        }
    }
    
    public void forceEnd() {
        if (state == MatchState.FINISHED || state == MatchState.CANCELLED) {
            return;
        }
        
        state = MatchState.CANCELLED;
        endTime = System.currentTimeMillis();
        
        // Reset PK mode
        participant1.getPlayer().setTypePK((byte) 0);
        participant2.getPlayer().setTypePK((byte) 0);
        
        // Notify players
        participant1.getPlayer().sendMessageDialog("Match cancelled");
        participant2.getPlayer().sendMessageDialog("Match cancelled");
        
        log.debug("Match {} force ended", id);
    }
    
    public boolean isFinished() {
        return state == MatchState.FINISHED || state == MatchState.CANCELLED ||
               (startTime > 0 && System.currentTimeMillis() - startTime > MATCH_TIMEOUT_MS);
    }
    
    public ContestParticipant getParticipant(PlayerEntity player) {
        if (participant1.getPlayer().getId() == player.getId()) {
            return participant1;
        } else if (participant2.getPlayer().getId() == player.getId()) {
            return participant2;
        }
        return null;
    }
    
    public ContestParticipant getOpponent(ContestParticipant participant) {
        return participant == participant1 ? participant2 : participant1;
    }
    
    public long getElapsedTime() {
        return startTime > 0 ? System.currentTimeMillis() - startTime : 0;
    }
    
    public long getRemainingTime() {
        return Math.max(0, MATCH_TIMEOUT_MS - getElapsedTime());
    }
    
    @Override
    public String toString() {
        return String.format("Match[%s] %s vs %s (Round %d, %s)", 
            id, participant1.getPlayer().getName(), participant2.getPlayer().getName(), 
            currentRound, state);
    }
}