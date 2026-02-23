package game.event.btf;

import game.entity.player.PlayerEntity;
import game.event.BaseEvent;
import lombok.Getter;
import lombok.Setter;
import manager.WorldManager;
import model.npc.Go;
import service.NetworkService;
import utils.Timer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class BTF extends BaseEvent {

    private final Map<Integer, PlayerEntity> participants = new HashMap<>();
    private final List<Team> teams = new ArrayList<>();

    private BTFState state = BTFState.REGISTRATION;
    private Timer.TimerHandle registerTimer;
    private static final int START_TIME = 2;
    private long nextBroadcastTime;

    public BTF() {
        super(0, "BTF");
        ZoneId zone = ZoneId.of("Asia/Jakarta");

        long start = ZonedDateTime.now(zone)
                .withHour(11).withMinute(0).withSecond(0)
                .toInstant().toEpochMilli();

        long end = ZonedDateTime.now(zone)
                .withHour(15).withMinute(0).withSecond(0)
                .toInstant().toEpochMilli();
        schedule(start, end);
    }

    // ================= REGISTRATION =================

    public void registerPlayer(PlayerEntity player) {

        if (state != BTFState.REGISTRATION) {
            player.sendMessageDialog("Pendaftaran telah ditutup");
            return;
        }

        if (participants.containsKey(player.getId())) {
            player.sendMessageDialog("Kamu sudah terdaftar");
            return;
        }

        participants.put(player.getId(), player);
        player.sendMessageDialog("Pendaftaran berhasil!");
    }

    // ================= EVENT LOOP =================

    @Override
    public void onUpdate(long currentTime) {

        switch (state) {
            case REGISTRATION:
                if (registerTimer.isDone()) {
                    closeRegistrationAndPrepare();
                }else{
                    if (currentTime >= nextBroadcastTime) {
                        broadcast(String.format("Pendaftaran BTF tersisa %d menit lagi, segera mendaftar !", TimeUnit.MILLISECONDS.toMinutes(registerTimer.getElapsedMs())));
                        nextBroadcastTime = currentTime + TimeUnit.MINUTES.toMillis(5);
                    }
                }
                break;

            case PREPARATION:
                startBattle();
                break;

            case FIGHTING:
                updateBattle();
                break;

            case FINISHED:
                break;
        }
    }

    // ================= PHASE CHANGES =================

    private void closeRegistrationAndPrepare() {
        state = BTFState.PREPARATION;
        distributeTeams();
        teleportPlayersToArena();
    }

    private void startBattle() {
        state = BTFState.FIGHTING;
        broadcast("BattleField telah di mulai");
    }

    private void updateBattle() {
        long aliveTeams = teams.stream().filter(t -> !t.isDestroyed()).count();

        if (aliveTeams <= 1) {
            finishBattle();
        }
    }

    private void finishBattle() {
        state = BTFState.FINISHED;
        teams.stream().filter(t -> !t.isDestroyed()).findFirst().ifPresent(winner -> broadcast(String.format("Battlefiled telah berakhir, Selamat %s telah memenangkan pertarungan", winner.getName())));

        teleportPlayersToVillage();
    }

    // ================= TEAM SYSTEM =================

    private void distributeTeams() {

        List<PlayerEntity> players = new ArrayList<>(participants.values());

        players.sort(Comparator.comparingInt(PlayerEntity::getLevel).reversed());

        teams.clear();
        teams.add(new Team("DESA BARAT", 89, Go.DESA_BARAT));
        teams.add(new Team("DESA UTARA", 90, Go.DESA_UTARA));
        teams.add(new Team("DESA SELATAN", 91, Go.DESA_SELATAN));
        teams.add(new Team("DESA TIMUR", 92, Go.DESA_TIMUR));

        int index = 0;
        for (PlayerEntity player : players) {
            Team team = teams.get(index % 4);
            team.setFlag((byte) ((index % 4) + 1));
            team.addPlayer(player);
            index++;
        }
    }

    private void teleportPlayersToArena() {
        for (Team team : teams) {
            for (PlayerEntity player : team.getPlayers().values()) {
                if (!player.isOnline()) continue;
                player.setTypePK(team.getFlag());
                player.teleport(team.getLocation());
            }
        }
    }

    private void teleportPlayersToVillage() {
        for (Team team : teams) {
            for (PlayerEntity player : team.getPlayers().values()) {
                if (!player.isOnline())
                    continue;


                player.teleport(Go.DESA_SRIGALA);
            }
        }
    }

    private void broadcast(String message) {
        WorldManager.getInstance().worldBroadcast(player -> NetworkService.gI().sendChatWorld(player, message));
    }

    @Override
    public void onStart() {
        nextBroadcastTime = System.currentTimeMillis()
                + TimeUnit.MINUTES.toMillis(5);
        registerTimer = Timer.countdown(
                TimeUnit.MINUTES.toMillis(START_TIME), null, null);
        state = BTFState.REGISTRATION;
        participants.clear();
        teams.clear();
    }

    @Override
    public void onEnd() {
        state = BTFState.FINISHED;
    }


    public Team getPlayerTeam(int playerId) {
        for (Team team : teams) {
            if (team.isMyTeam(playerId)) {
                return team;
            }
        }
        return null;
    }

    public void destroyTower(int towerId) {
        for (Team team : teams) {
            if (team.getTowerId() == towerId) {
                team.destroy();
                break;
            }
        }
    }

    public void onPlayerDie(PlayerEntity player) {
        Team team;
        if ((team = getPlayerTeam(player.getId())) != null) {
            Timer.countdown(TimeUnit.SECONDS.toMillis(10), tick -> NetworkService.gI().sendToast(player, String.format("Cooldown %d second", tick)), () -> {
                player.respawn();
                player.teleport(team.getLocation());
            });
        }
    }

}
