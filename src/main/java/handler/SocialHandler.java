package handler;

import game.entity.player.PlayerEntity;
import game.friend.FriendRequest;
import game.guild.Guild;
import game.guild.GuildManager;
import game.guild.GuildService;
import game.party.Party;
import game.party.PartyManager;
import game.party.PartyRequest;
import lombok.extern.slf4j.Slf4j;
import manager.ConfigManager;
import manager.ItemManager;
import manager.WorldManager;
import model.config.SVConfig;
import network.Message;
import network.Session;
import service.NetworkService;

import java.io.IOException;

import static handler.Command.*;

@Slf4j
public class SocialHandler {
    public static void onMessage(Session s, Message m) throws IOException {
        switch (m.command) {
            case CHAT_TAB -> onUserChat(s, m);
            case CHAT_WORLD -> onChatWorld(s, m);
            case CHAT_POPUP -> onChatPopup(s, m);
            case PARTY -> onParty(s, m);
            case FRIEND -> onFriend(s, m);
        }
    }

    private static void onFriend(Session s, Message m) throws IOException {

        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte type = m.in().readByte();
        String name = m.in().readUTF();

        switch (type) {
            // FRIEND REQUEST
            case 0 -> {
                PlayerEntity target = WorldManager.getInstance().findPlayer(name);
                if (target == null) {
                    NetworkService.gI().sendToast(p, String.format("%s telah offline", name));
                    return;
                }
                if (p.getFriendList().isFriend(target.getId())) {
                    NetworkService.gI().sendToast(p, String.format("%s telah berteman", name));
                    return;
                }

                target.addFriendRequest(p.getName());
                NetworkService.gI().sendFriendRequest(target, p.getName());
            }
            // Accept Friend Request
            case 1 -> {
                PlayerEntity notify = WorldManager.getInstance().findPlayer(name);
                if (notify == null) {
                    NetworkService.gI().sendToast(p, String.format("%s telah offline", name));
                    return;
                }
                FriendRequest req = p.getFriendRequest(name);

                if (req == null) {
                    NetworkService.gI().sendToast(p, "Permintaan pertemanan tidak ditemukan");
                    return;
                }


                if (req.isExpired()) {
                    p.removeFriendRequest(name);
                    NetworkService.gI().sendToast(p, "Permintaan pertemanan telah kadaluarsa");
                    return;
                }
                p.getFriendList().add(notify.getId());
                notify.getFriendList().add(p.getId());
                NetworkService.gI().sendToast(notify, String.format("%s menerima pertemanan", p.getName()));
            }
            // Reject Friend Request
            case 2 -> {
                PlayerEntity notify = WorldManager.getInstance().findPlayer(name);
                if (notify == null) {
                    NetworkService.gI().sendToast(p, String.format("%s telah offline", name));
                    return;
                }
                p.removeFriendRequest(name);
                NetworkService.gI().sendToast(notify, String.format("%s menolak pertemanan", p.getName()));
            }
            // Friend List
            case 4 -> NetworkService.gI().sendFriendList(p, p.getFriendList());
        }


    }

    private static void onParty(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte type = m.in().readByte();


        log.debug("type {}  ", type);
        switch (type) {
            // Invite
            case 1 -> {
                String name = m.in().readUTF();
                PlayerEntity target = p.getZone().findPlayerByName(name);
                if (target == null) {
                    NetworkService.gI().sendToast(p, String.format("%s tidak ditemukan", name));
                    return;
                }

                if (target.getParty() != null) {
                    NetworkService.gI().sendToast(p, String.format("%s sudah memiliki group", name));
                    return;
                }

                Party party = p.getParty();
                if (party == null) {
                    party = PartyManager.getInstance().createParty(p);
                }

                if (!party.canJoin()) {
                    NetworkService.gI().sendToast(p, "Group sudah penuh");
                    return;
                }

                target.addPartyRequest(p.getName());
                NetworkService.gI().sendPartyRequest(target, p.getName());
            }
            case 2 -> {
                String name = m.in().readUTF();
                PartyRequest req = p.getPartyRequest(name);

                if (req == null) {
                    NetworkService.gI().sendToast(p, "Permintaan group tidak ditemukan");
                    return;
                }

                if (req.isExpired()) {
                    p.getPartyRequests().remove(name);
                    NetworkService.gI().sendToast(p, "Permintaan group telah kadaluarsa");
                    return;
                }

                // Accept
                PlayerEntity target = p.getZone().findPlayerByName(name);
                if (target == null) {
                    NetworkService.gI().sendToast(p, String.format("%s tidak ditemukan", name));
                    return;
                }

                Party party = target.getParty();
                if (party == null) {
                    return;
                }

                if (!party.canJoin()) {
                    NetworkService.gI().sendToast(p, "Group sudah penuh");
                    return;
                }
                PartyManager.getInstance().joinParty(party.getId(), p);
                party.broadcastPartyInfo();
                NetworkService.gI().sendToast(target, String.format("%s bergabung kedalam group", p.getName()));
            }
            case 3 -> {
                String name = m.in().readUTF();
                PlayerEntity target = p.getZone().findPlayerByName(name);
                if (target == null) {
                    NetworkService.gI().sendToast(p, String.format("%s tidak ditemukan", name));
                    return;
                }

                Party party = p.getParty();
                if (party != null) {
                    party.removeMember(target);
                    party.broadcastPartyInfo();
                    NetworkService.gI().leaveParty(target);
                }
            }
            case 4 -> {
                Party party = p.getParty();
                if (party != null) {
                    PartyManager.getInstance().destroyParty(party.getId());
                }
            }
            case 5 -> {
                Party party = p.getParty();
                if (party == null) return;

                if (party.getLeader().getId() == p.getId() && party.size() <= 1) {
                    PartyManager.getInstance().destroyParty(party.getId());
                    return;
                }

                PartyManager.getInstance().leaveParty(party.getId(), p);
                party.broadcastPartyInfo();
                NetworkService.gI().leaveParty(p);
            }

        }
    }

    private static void onChatPopup(Session s, Message m) throws IOException {

        PlayerEntity p = s.getPlayer();
        if (p == null) {
            return;
        }

        String chat = m.in().readUTF();
        p.getZone().broadcastExcept(p, player -> NetworkService.gI().sendChatPopup(player, p, chat));

    }

    private static void onChatWorld(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        String chat = m.in().readUTF();
        SVConfig svConfig = ConfigManager.getInstance().getSvConfig();
        if (p.getGems() < svConfig.getPriceChatWorld()) {
            p.sendMessageDialog(String.format("Butuh %d permata untuk menggunakan fitur ini", svConfig.getPriceChatWorld()));
            return;
        }

        p.spendGem(svConfig.getPriceChatWorld());
        p.getInventoryManager().updateInventory();

        String playerChat = String.format("@%s: %s", p.getName(), chat);
        WorldManager.getInstance().worldBroadcast(player -> NetworkService.gI().sendChatWorld(player, playerChat));

    }

    private static void onUserChat(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        String chatFrom = m.in().readUTF();
        String chat = m.in().readUTF();

        log.debug("{} >> {}", chatFrom, chat);
        switch (chatFrom) {
            case "Team", "Grup" -> {
                Party party = p.getParty();
                if (party == null) return;

                String body = String.format("@%s: %s", p.getName(), chat);
                party.broadcast(player -> NetworkService.gI().sendChatTab(player, chatFrom, body));

            }
            case "Guild", "Clan" -> {
                Guild guild = GuildManager.getInstance().getPlayerGuild(p.getId());
                if (guild == null) return;
                String body = String.format("@%s: %s", p.getName(), chat);
                GuildService.getInstance().broadcast(guild, player -> NetworkService.gI().sendChatTab(player, chatFrom, body));
            }
            default -> {
                PlayerEntity target = WorldManager.getInstance().findPlayer(chatFrom);
                if (target == null) {
                    p.sendMessageDialog("Pemain tidak aktif");
                    return;
                }
                NetworkService.gI().sendChatTab(target, p.getName(), chat);
            }
        }
    }
}
