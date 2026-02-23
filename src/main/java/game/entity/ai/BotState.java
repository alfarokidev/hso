package game.entity.ai;


public enum BotState {
    IDLE,     // standing still, scanning for targets
    PATROL,   // wandering near spawn when no enemy found
    CHASE,    // moving toward a detected target
    ATTACK,   // in range — using skills on target
    RETURN    // walking back to spawn after combat
}