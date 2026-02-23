package game.guild;

import java.util.EnumSet;
import java.util.Set;

import static game.guild.GuildPermission.*;

public enum GuildRank {
    LEADER(EnumSet.allOf(GuildPermission.class)),
    DEPUTY(EnumSet.of(
            INVITE,
            KICK_MEMBER,
            EDIT_ANNOUNCEMENT,
            EDIT_RULES,
            MANAGE_WAREHOUSE)),
    OFFICER(EnumSet.of(
            INVITE,
            KICK_MEMBER,
            MANAGE_WAREHOUSE
    )),
    ELITE(EnumSet.of(
            INVITE,
            MANAGE_WAREHOUSE
    )),
    MEMBER(EnumSet.of(
            GuildPermission.CHAT,
            MANAGE_WAREHOUSE
    )),
    RECRUIT(EnumSet.of(
            GuildPermission.CHAT
    ));

    private final Set<GuildPermission> permissions;

    GuildRank(Set<GuildPermission> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(GuildPermission permission) {
        return permissions.contains(permission);
    }

    public Set<GuildPermission> getPermissions() {
        return EnumSet.copyOf(permissions);
    }
}