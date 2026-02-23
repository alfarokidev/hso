package game.guild;

import lombok.Getter;

@Getter
public enum GuildResult {
    SUCCESS("Operation successful"),
    ALREADY_IN_GUILD("Already in a guild"),
    NOT_IN_GUILD("Not in a guild"),
    NAME_TAKEN("Guild name already taken"),
    SHORT_NAME_TAKEN("Guild short name already taken"),
    GUILD_NOT_FOUND("Guild not found"),
    GUILD_FULL("Guild is full"),
    NO_PERMISSION("No permission"),
    TARGET_IN_GUILD("Target player already in a guild"),
    PLAYER_NOT_IN_GUILD("Player not in guild"),
    CANNOT_KICK_LEADER("Cannot kick guild leader"),
    LEADER_CANNOT_LEAVE("Leader cannot leave, must transfer leadership or disband"),
    INVITE_SENT("Invite sent successfully"),
    INVALID_NAME("Invalid guild name"),
    ERROR("Terjadi kesalahan"),
    INSUFFICIENT_FUNDS("Insufficient funds");

    private final String message;

    GuildResult(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return this == SUCCESS || this == INVITE_SENT;
    }
}