package game.guild;


import database.SQL;
import game.entity.player.PlayerEntity;
import handler.Command;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;
import model.player.Part;
import model.player.Player;
import network.Message;
import service.PlayerService;
import utils.PlayerHelper;

import java.lang.reflect.Member;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class GuildService {


    private GuildService() {
    }

    private static final class InstanceHolder {
        private static final GuildService instance = new GuildService();
    }

    public static GuildService getInstance() {
        return GuildService.InstanceHolder.instance;
    }

    public GuildResult promoteMember(PlayerEntity promoter, int targetPlayerId) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(promoter.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember promoterMember = guild.getMember(promoter.getId());
        if (!promoterMember.hasPermission(GuildPermission.PROMOTE)) {
            return GuildResult.NO_PERMISSION;
        }

        GuildMember target = guild.getMember(targetPlayerId);
        if (target == null) {
            return GuildResult.PLAYER_NOT_IN_GUILD;
        }

        GuildRank currentRank = target.getRank();
        GuildRank newRank = getNextRank(currentRank);

        if (newRank == null || newRank == GuildRank.LEADER) {
            return GuildResult.NO_PERMISSION;
        }

        target.setRank(newRank);
        log.debug("Player {} promoted to {} in guild {}",
                target.getPlayerName(), newRank, guild.getName());

        return GuildResult.SUCCESS;
    }

    public GuildResult demoteMember(PlayerEntity demoter, int targetPlayerId) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(demoter.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember demoterMember = guild.getMember(demoter.getId());
        if (!demoterMember.hasPermission(GuildPermission.DEMOTE)) {
            return GuildResult.NO_PERMISSION;
        }

        GuildMember target = guild.getMember(targetPlayerId);
        if (target == null) {
            return GuildResult.PLAYER_NOT_IN_GUILD;
        }

        GuildRank currentRank = target.getRank();
        GuildRank newRank = getPreviousRank(currentRank);

        if (newRank == null) {
            return GuildResult.NO_PERMISSION;
        }

        target.setRank(newRank);
        log.info("Player {} demoted to {} in guild {}",
                target.getPlayerName(), newRank, guild.getName());

        return GuildResult.SUCCESS;
    }

    public GuildResult transferLeadership(PlayerEntity leader, int targetPlayerId) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(leader.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        if (!guild.isLeader(leader)) {
            return GuildResult.NO_PERMISSION;
        }

        GuildMember target = guild.getMember(targetPlayerId);
        if (target == null) {
            return GuildResult.PLAYER_NOT_IN_GUILD;
        }

        GuildMember oldLeader = guild.getMember(leader.getId());
        oldLeader.setRank(GuildRank.OFFICER);
        target.setRank(GuildRank.LEADER);

        guild.setLeader(targetPlayerId);

        log.debug("Guild {} leadership transferred from {} to {}",
                guild.getName(), leader.getName(), target.getPlayerName());

        return GuildResult.SUCCESS;
    }

    public GuildResult depositGold(PlayerEntity player, long amount) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        if (player.getGold() < amount) {
            return GuildResult.INSUFFICIENT_FUNDS;
        }

        player.spendGold(amount);
        guild.setGold(guild.getGold() + amount);

        GuildMember member = guild.getMember(player.getId());
        member.addContribution((int) (amount / 100));

        log.debug("Player {} deposited {} gold to guild {}",
                player.getName(), amount, guild.getName());

        return GuildResult.SUCCESS;
    }

    public GuildResult withdrawGold(PlayerEntity player, long amount) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember member = guild.getMember(player.getId());
        if (!member.hasPermission(GuildPermission.WITHDRAW_GOLD)) {
            return GuildResult.NO_PERMISSION;
        }

        if (guild.getGold() < amount) {
            return GuildResult.INSUFFICIENT_FUNDS;
        }

        guild.setGold(guild.getGold() - amount);
        player.addGold(amount);

        log.info("Player {} withdrew {} gold from guild {}",
                player.getName(), amount, guild.getName());

        return GuildResult.SUCCESS;
    }

    public GuildResult updateRules(PlayerEntity player, String newRules) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember member = guild.getMember(player.getId());
        if (!member.hasPermission(GuildPermission.EDIT_RULES)) {
            return GuildResult.NO_PERMISSION;
        }

        guild.setRules(newRules);
        log.info("Guild {} rules updated by {}", guild.getName(), player.getName());

        return GuildResult.SUCCESS;
    }

    public GuildResult updateSlogan(PlayerEntity player, String newSlogan) {
        Guild guild = GuildManager.getInstance().getPlayerGuild(player.getId());
        if (guild == null) {
            return GuildResult.NOT_IN_GUILD;
        }

        GuildMember member = guild.getMember(player.getId());
        if (!member.hasPermission(GuildPermission.EDIT_ANNOUNCEMENT)) {
            return GuildResult.NO_PERMISSION;
        }

        guild.setSlogan(newSlogan);
        log.info("Guild {} slogan updated by {}", guild.getName(), player.getName());

        return GuildResult.SUCCESS;
    }

    private GuildRank getNextRank(GuildRank current) {
        return switch (current) {
            case RECRUIT -> GuildRank.MEMBER;
            case MEMBER -> GuildRank.ELITE;
            case ELITE -> GuildRank.OFFICER;
            case OFFICER -> GuildRank.DEPUTY;
            default -> null;
        };
    }

    private GuildRank getPreviousRank(GuildRank current) {
        return switch (current) {
            case DEPUTY -> GuildRank.OFFICER;
            case OFFICER -> GuildRank.ELITE;
            case ELITE -> GuildRank.MEMBER;
            case MEMBER -> GuildRank.RECRUIT;
            default -> null;
        };
    }

    public void sendGuildRequest(PlayerEntity notify, String name) {
        try {
            Message m = new Message(Command.CLAN);
            m.out().writeByte(10);
            m.out().writeUTF(name);
            notify.send(m);
        } catch (Exception e) {
            log.error("Guild info error", e);
        }
    }

    public void broadcast(Guild guild, Consumer<PlayerEntity> notify) {
        for (GuildMember member : guild.getMembers()) {
            PlayerEntity player = WorldManager.getInstance().findPlayer(member.getPlayerId());
            if (player == null) continue;

            notify.accept(player);
        }
    }

    public void sendMemberList(PlayerEntity notify, Guild guild) {
        List<Player> playerData = PlayerService.gI().findAllById(guild.getMembers().stream().map(GuildMember::getPlayerId).toList());

        try {
            Message m = new Message(Command.SET_PAGE);
            m.out().writeByte(4);
            m.out().writeUTF("Guild Member");
            m.out().writeByte(50); // No Use
            m.out().writeInt(0);

            m.out().writeByte(playerData.size());
            for (Player player : playerData) {

                m.out().writeUTF(player.getName());
                m.out().writeByte(player.getBody()[0]);
                m.out().writeByte(player.getBody()[1]);
                m.out().writeByte(player.getBody()[2]);
                m.out().writeShort(player.getLevel());

                List<Part> parts = PlayerHelper.getPartPlayer(player.getId());
                m.out().writeByte(parts.size());
                for (Part part : parts) {
                    m.out().writeByte(part.getPart());
                    m.out().writeByte(part.getType());
                }

                m.out().writeByte(WorldManager.getInstance().isOnline(player.getId()) ? 1 : 0);
                m.out().writeUTF(guild.getMember(player.getId()).getRank().name());

                m.out().writeShort(guild.getIcon());
                m.out().writeUTF(guild.getShortName());
                m.out().writeByte(guild.getMember(player.getId()).getPosition());
            }
            notify.send(m);
        } catch (Exception e) {
            log.error("Guild info error", e);
        }
    }

    public void sendMemberInfo(PlayerEntity notify, GuildMember member, PlayerEntity player) {
        try {
            Message m = new Message(Command.CLAN);
            m.out().writeByte(14);
            m.out().writeUTF(player.getName());
            m.out().writeShort(player.getLevel());
            m.out().writeByte(member.getPosition());
            m.out().writeLong(member.getGoldContribution());
            m.out().writeInt(member.getGemContribution());
            notify.send(m);
        } catch (Exception e) {
            log.error("Guild info error", e);
        }
    }

    public void sendGuildInfo(PlayerEntity p, Guild guild) {
        try {
            Message m = new Message(Command.CLAN);
            m.out().writeByte(15);
            if (guild.isLeader(p)) {
                m.out().writeByte(0);
            } else {
                m.out().writeByte(1);
            }
            m.out().writeByte(0);
            m.out().writeInt(guild.getId());
            m.out().writeShort(guild.getIcon());
            m.out().writeUTF(guild.getShortName());
            m.out().writeUTF(guild.getName());
            m.out().writeShort(guild.getLevel());
            m.out().writeShort(guild.getLevelPercent());

            m.out().writeShort(GuildManager.getInstance().getGuildRankByLevel(guild.getId())); // Guild RANK

            m.out().writeShort(guild.getMembers().size()); // mem
            m.out().writeShort(guild.getMaxMembers()); // max mem

            m.out().writeUTF(guild.getLeaderName());
            m.out().writeUTF(guild.getSlogan()); // slogan
            m.out().writeUTF(guild.getRules()); // noi quy
            m.out().writeLong(guild.getGold());
            m.out().writeInt(guild.getGem());
            m.out().writeByte(0); // thanh tich
            p.send(m);
        } catch (Exception e) {
            log.error("Guild info error", e);
        }
    }


    public long create(Guild guild) {
        try {
            return SQL.insert(guild).execute();
        } catch (Exception e) {
            return -1;
        }
    }

    public void save() {
        GuildManager.getInstance().getGuilds().forEach(
                (integer, guild) -> {
                    guild.saveInventory();
                    try {
                        SQL.save(guild);
                    } catch (SQLException e) {
                        log.error("Error saving guild", e);
                    }
                }
        );

        GuildManager.getInstance().getGuildCrystalMap().forEach(
                (mapId, mine) -> {
                    try {
                        SQL.save(mine);
                    } catch (SQLException ignore) {
                    }
                }
        );
    }
}