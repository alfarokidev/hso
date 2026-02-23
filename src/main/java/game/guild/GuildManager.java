package game.guild;


import model.monster.GuildMine;
import game.entity.player.PlayerEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class GuildManager {

    private final Map<Integer, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> playerGuildMap = new ConcurrentHashMap<>();
    private final Map<Integer, GuildMine> guildCrystalMap = new ConcurrentHashMap<>();


    private GuildManager() {
    }

    private static final class InstanceHolder {
        private static final GuildManager instance = new GuildManager();
    }

    public static GuildManager getInstance() {
        return InstanceHolder.instance;
    }


    public GuildResult createGuild(PlayerEntity leader, String name, String shortName) {
        if (playerGuildMap.containsKey(leader.getId())) {
            return GuildResult.ALREADY_IN_GUILD;
        }

        if (isGuildNameTaken(name)) {
            return GuildResult.NAME_TAKEN;
        }

        if (isShortNameTaken(shortName)) {
            return GuildResult.SHORT_NAME_TAKEN;
        }

        Guild guild = new Guild(name, shortName, leader);
        GuildMember leaderMember = new GuildMember(
                leader.getId(),
                leader.getName(),
                GuildRank.LEADER
        );
        guild.setSlogan("Tanpa drama, cuma kerja sama.");
        guild.setRules("Saling membantu");
        guild.setNotification("Bravo");
        guild.getMembers().add(leaderMember);
        long id = GuildService.getInstance().create(guild);
        if (id == -1) {
            return GuildResult.ERROR;
        }

        guild.setId((int) id);
        guild.createInventory();
        guilds.put((int) id, guild);
        playerGuildMap.put(leader.getId(), (int) id);

        log.debug("Guild created: {} by {}", name, leader.getName());
        return GuildResult.SUCCESS;
    }

    public GuildResult invitePlayer(PlayerEntity inviter, PlayerEntity target) {
        Guild guild = getPlayerGuild(inviter.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember member = guild.getMember(inviter.getId());
        if (!member.hasPermission(GuildPermission.INVITE)) {
            return GuildResult.NO_PERMISSION;
        }

        if (playerGuildMap.containsKey(target.getId())) {
            return GuildResult.TARGET_IN_GUILD;
        }

        if (guild.isFull()) {
            return GuildResult.GUILD_FULL;
        }

        // Send invite packet to target player
        return GuildResult.INVITE_SENT;
    }

    public GuildResult acceptInvite(PlayerEntity player, int guildId) {
        if (playerGuildMap.containsKey(player.getId())) {
            return GuildResult.ALREADY_IN_GUILD;
        }

        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return GuildResult.GUILD_NOT_FOUND;
        }

        if (guild.isFull()) {
            return GuildResult.GUILD_FULL;
        }


        GuildMember newMember = new GuildMember(
                player.getId(),
                player.getName(),
                GuildRank.RECRUIT
        );

        guild.getMembers().add(newMember);
        playerGuildMap.put(player.getId(), guildId);

        log.debug("Player {} joined guild {}", player.getName(), guild.getName());
        return GuildResult.SUCCESS;
    }

    public GuildResult kickMember(PlayerEntity kicker, int targetPlayerId) {
        Guild guild = getPlayerGuild(kicker.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember kickerMember = guild.getMember(kicker.getId());
        if (!kickerMember.hasPermission(GuildPermission.KICK_MEMBER)) {
            return GuildResult.NO_PERMISSION;
        }

        GuildMember target = guild.getMember(targetPlayerId);
        if (target == null) {
            return GuildResult.PLAYER_NOT_IN_GUILD;
        }

        if (!guild.isLeader(kicker)) {
            return GuildResult.CANNOT_KICK_LEADER;
        }

        guild.getMembers().remove(target);
        playerGuildMap.remove(targetPlayerId);

        log.debug("Player {} kicked from guild {}", target.getPlayerName(), guild.getName());
        return GuildResult.SUCCESS;
    }

    public GuildResult leaveGuild(PlayerEntity player) {
        Guild guild = getPlayerGuild(player.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        if (guild.isLeader(player)) {
            return GuildResult.LEADER_CANNOT_LEAVE;
        }

        GuildMember member = guild.getMember(player.getId());
        guild.getMembers().remove(member);
        playerGuildMap.remove(player.getId());

        log.debug("Player {} left guild {}", player.getName(), guild.getName());
        return GuildResult.SUCCESS;
    }

    public GuildResult disbandGuild(PlayerEntity leader) {
        Guild guild = getPlayerGuild(leader.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        if (!guild.isLeader(leader)) {
            return GuildResult.NO_PERMISSION;
        }

        for (GuildMember member : guild.getMembers()) {
            playerGuildMap.remove(member.getPlayerId());
        }

        guilds.remove(guild.getId());
        log.debug("Guild {} disbanded by {}", guild.getName(), leader.getName());
        return GuildResult.SUCCESS;
    }

    public Guild getPlayerGuild(int playerId) {
        Integer guildId = playerGuildMap.get(playerId);
        return guildId != null ? guilds.get(guildId) : null;
    }

    public Guild getGuildById(int guildId) {
        return guilds.get(guildId);
    }

    private boolean isGuildNameTaken(String name) {
        return guilds.values().stream()
                .anyMatch(g -> g.getName().equalsIgnoreCase(name));
    }

    private boolean isShortNameTaken(String shortName) {
        return guilds.values().stream()
                .anyMatch(g -> g.getShortName().equalsIgnoreCase(shortName));
    }

    public void setGuilds(List<Guild> data) {
        for (Guild guild : data) {
            guild.loadInventory();
            guilds.put(guild.getId(), guild);
            for (GuildMember member : guild.getMembers()) {
                playerGuildMap.put(member.getPlayerId(), guild.getId());
            }
        }
    }

    public GuildMine getByMap(int mapId) {

        for (GuildMine mine : guildCrystalMap.values()) {
            if (mine.getId() == mapId) return mine;
        }
        return null;
    }

    public void setGuildCrystal(List<GuildMine> data) {
        for (GuildMine crystal : data) {
            guildCrystalMap.put(crystal.getId(), crystal);
        }
    }

    public int getGuildRankByLevel(int guildId) {
        Guild targetGuild = guilds.get(guildId);
        if (targetGuild == null) {
            return -1; // guild not found
        }

        List<Guild> sortedGuilds = guilds.values()
                .stream()
                .sorted(
                        Comparator.comparingInt(Guild::getLevel)
                                .reversed()
                                .thenComparingInt(Guild::getId)
                )
                .toList();

        for (int i = 0; i < sortedGuilds.size(); i++) {
            if (sortedGuilds.get(i).getId() == guildId) {
                return i + 1; // rank is 1-based
            }
        }

        return 99;
    }
}
