package game.party;

import game.entity.player.PlayerEntity;
import lombok.Data;
import service.NetworkService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
public class Party {

    public static final int MAX_MEMBERS = 6;

    private final String id;
    private volatile PlayerEntity leader;
    private final ConcurrentHashMap<Integer, PlayerEntity> members =
            new ConcurrentHashMap<>();

    public Party(String id, PlayerEntity leader) {
        this.id = id;
        this.leader = leader;

        // add leader as first member
        members.put(leader.getId(), leader);

        // IMPORTANT: link player to party
        leader.setParty(this);
    }

    // -----------------
    // CHECK LEADER
    // -----------------
    public boolean isLeader(PlayerEntity player) {
        return leader != null && leader.getId() == player.getId();
    }

    // -----------------
    // CAN JOIN?
    // -----------------
    public boolean canJoin() {
        return members.size() < MAX_MEMBERS;
    }

    // -----------------
    // ADD MEMBER
    // -----------------
    public boolean addMember(PlayerEntity player) {
        if (player == null) return false;
        if (!canJoin()) return false;

        boolean added = members.putIfAbsent(player.getId(), player) == null;
        if (added) {
            player.setParty(this);   // IMPORTANT
        }
        return added;
    }

    // -----------------
    // REMOVE MEMBER
    // -----------------
    public boolean removeMember(PlayerEntity player) {
        if (player == null) return false;

        PlayerEntity removed = members.remove(player.getId());
        if (removed == null) return false;

        player.setParty(null);   // IMPORTANT

        // if leader left → promote new leader
        if (leader != null && leader.getId() == player.getId()) {
            promoteNewLeader();
        }
        return true;
    }

    // -----------------
    // PROMOTE NEW LEADER
    // -----------------
    private void promoteNewLeader() {
        if (members.isEmpty()) {
            leader = null;
            return;
        }

        // pick any remaining member as leader
        leader = members.values().iterator().next();
    }

    // -----------------
    // PARTY EMPTY?
    // -----------------
    public boolean isEmpty() {
        return members.isEmpty();
    }

    public int size() {
        return members.size();
    }

    // -----------------
    // BROADCAST
    // -----------------
    public void broadcastPartyInfo() {
        for (PlayerEntity p : members.values()) {
            if (p.isOnline()) {
                NetworkService.gI().sendParty(p, this);
            }
        }
    }

    public void broadcast(Consumer<PlayerEntity> action) {
        for (PlayerEntity p : members.values()) {
            if (p.isOnline()) {
                action.accept(p);
            }
        }
    }


}
