package handler;

import game.entity.player.PlayerEntity;
import game.guild.*;
import lombok.extern.slf4j.Slf4j;
import manager.WorldManager;
import model.npc.NpcName;
import network.Message;
import network.Session;
import service.NetworkService;
import service.ShopService;

import java.io.IOException;

@Slf4j
public class GuildHandler {

    public static void handleGuild(Session s, Message m) throws IOException {
        PlayerEntity p = s.getPlayer();
        if (p == null) return;

        byte type = m.in().readByte();

        // Accept Guild Request
        if (type == 11) {
            String from = m.in().readUTF();
            handleAccept(p, from);
            return;
        }

        Guild guild = GuildManager.getInstance().getPlayerGuild(p.getId());
        if (guild == null) return;


        log.debug("GUILD TYPE {}", type);
        switch (type) {
            case 6 -> {
                int gold = m.in().readInt();
                if (!p.spendGold(gold)) {
                    p.sendMessageDialog("Gold tidak cukup");
                    return;
                }
                GuildMember member = guild.getMember(p.getId());
                if (member == null) return;

                int cp = guild.donateGold(member, gold);
                p.sendMessageDialog(String.format("%d gold telah di donasikan, kamu menerima %d point kontribusi", gold, cp));
            }
            case 7 -> {
                int gem = m.in().readInt();
                if (!p.spendGem(gem)) {
                    p.sendMessageDialog("Permata tidak cukup");
                    return;
                }
                GuildMember member = guild.getMember(p.getId());
                if (member == null) return;


                int cp = guild.donateGem(member, gem);
                p.sendMessageDialog(String.format("%d permata telah di donasikan, kamu menerima %d point kontribusi", gem, cp));
            }
            case 13 -> GuildService.getInstance().sendMemberList(p, guild);
            case 16 -> {
                if (!guild.isLeader(p)) {
                    p.sendMessageDialog("Tidak ada izin");
                    return;
                }
                guild.setSlogan(m.in().readUTF());
                p.sendMessageDialog("Slogan guild berhasil diubah");
            }

            case 17 -> {
                if (!guild.isLeader(p)) {
                    p.sendMessageDialog("Tidak ada izin");
                    return;
                }
                guild.setRules(m.in().readUTF());
                p.sendMessageDialog("Peraturan guild berhasil diubah");
            }
            case 2 -> {
                if (!guild.isLeader(p)) {
                    p.sendMessageDialog("Tidak ada izin");
                    return;
                }
                guild.setNotification(m.in().readUTF());
                p.sendMessageDialog("Notifikasi guild berhasil diubah");
            }
            case 18 -> {
                String name = m.in().readUTF();

                if (!guild.isLeader(p)) {
                    p.sendMessageDialog("Kamu tidak memiliki izin");
                    return;
                }

                GuildMember member = guild.getMember(name);
                if (member == null) {
                    p.sendMessageDialog("Anggota tidak ditemukan");
                    return;
                }

                GuildResult result = GuildManager.getInstance().kickMember(p, member.getPlayerId());
                p.sendMessageDialog(result.getMessage());

            }
            case 4 -> {
                byte pos = m.in().readByte();
                String name = m.in().readUTF();
                GuildMember member = guild.getMember(name);
                if (member == null) {
                    p.sendMessageDialog("Anggota tidak ditemukan");
                    return;
                }
                String notif;
                if (pos < member.getPosition()) {
                    notif = "telah di turunkan menjadi";
                } else {
                    notif = "telah di promosikan sebagai";
                }

                member.setPosition(pos);
                p.sendMessageDialog(String.format("%s %s %s", name, notif, member.getRank().name()));

                GuildService.getInstance().broadcast(guild, player -> {
                    NetworkService.gI().sendChatTab(player, "Guild", String.format("Pengumuman: %s %s %s", name, notif, member.getRank().name()));
                });

            }
            case 14 -> {
                String name = m.in().readUTF();
                PlayerEntity target = WorldManager.getInstance().findPlayer(name);
                if (target == null) {
                    p.sendMessageDialog("Player ini tidak aktif");
                    return;
                }
                GuildMember member = guild.getMember(target.getId());
                if (member == null) {
                    p.sendMessageDialog("Player ini bukan anggota guild");
                    return;
                }

                GuildService.getInstance().sendMemberInfo(p, member, target);

            }
            case 15 -> GuildService.getInstance().sendGuildInfo(p, guild);
            case 21 -> {
                p.getInventoryManager().openGuildStorage(guild.getInventory());
                ShopService.getInstance().sendShop(p, NpcName.AMAN);
            }
            case 10 -> {
                String name = m.in().readUTF();
                PlayerEntity target = p.getZone().findPlayerByName(name);

                if (target == null) {
                    p.sendMessageDialog("Pemain tidak ditemukan");
                    return;
                }

                Guild targetGuild = GuildManager.getInstance().getPlayerGuild(target.getId());
                if (targetGuild != null) {
                    p.sendMessageDialog("Pemain sudah memiliki guild");
                    return;
                }

                if (guild.isFull()) {
                    p.sendMessageDialog("Guild sudah penuh");
                    return;
                }
                target.addGuildRequest(p.getName());
                GuildService.getInstance().sendGuildRequest(target, p.getName());
            }
            default -> p.sendMessageDialog("Belum di buka");
        }
    }

    private static void handleAccept(PlayerEntity p, String from) {
        GuildRequest request = p.getGuildRequest(from);
        if (request == null) {
            p.sendMessageDialog("Batas waktu undangan telah kadaluarsa");
            return;
        }

        if (request.isExpired()) {
            p.sendMessageDialog("Batas waktu undangan telah kadaluarsa");
            return;
        }


        Guild accepterGuild = GuildManager.getInstance().getPlayerGuild(p.getId());
        if (accepterGuild != null) {
            p.sendMessageDialog("Kamu sudah memiliki guild");
            return;
        }

        PlayerEntity inviter = WorldManager.getInstance().findPlayer(from);
        if (inviter == null) {
            p.sendMessageDialog("Ketua guild telah offline");
            return;
        }

        Guild inviterGuild = GuildManager.getInstance().getPlayerGuild(inviter.getId());
        if (inviterGuild == null) {
            p.sendMessageDialog("Guild tidak ditemukan");
            return;
        }

        GuildManager.getInstance().acceptInvite(p, inviterGuild.getId());
        p.getZone().broadcast(notify -> NetworkService.gI().sendCharInfo(notify, p));
        NetworkService.gI().sendMainCharInfo(p);
    }


}
