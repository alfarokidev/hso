package game.party;

import game.entity.player.PlayerEntity;
import service.NetworkService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final ConcurrentHashMap<String, Party> parties = new ConcurrentHashMap<>();

    private PartyManager() {
    }

    private static class Holder {
        private static final PartyManager INSTANCE = new PartyManager();
    }

    public static PartyManager getInstance() {
        return Holder.INSTANCE;
    }

    // -----------------
    // CREATE PARTY
    // -----------------
    public Party createParty(PlayerEntity leader) {

        // already has party?
        if (leader.getParty() != null) {
            return leader.getParty();
        }

        String partyId = UUID.randomUUID().toString();   // UNIQUE
        Party party = new Party(partyId, leader);

        parties.put(partyId, party);
        leader.setParty(party);   // IMPORTANT

        return party;
    }

    // -----------------
    // JOIN PARTY
    // -----------------
    public boolean joinParty(String partyId, PlayerEntity player) {
        Party party = parties.get(partyId);
        if (party == null) return false;

        if (player.getParty() != null) return false; // already in party

        boolean added = party.addMember(player);
        if (!added) return false;

        player.setParty(party);  // IMPORTANT
        return true;
    }

    // -----------------
    // LEAVE PARTY
    // -----------------
    public void leaveParty(String partyId, PlayerEntity player) {
        Party party = parties.get(partyId);
        if (party == null) return;

        party.removeMember(player);
        player.setParty(null);

        if (party.isEmpty()) {
            parties.remove(partyId);
        }
    }

    // -----------------
    // KICK MEMBER
    // -----------------
    public boolean kick(String partyId, PlayerEntity leader, PlayerEntity target) {
        Party party = parties.get(partyId);
        if (party == null) return false;

        if (!party.isLeader(leader)) return false;

        boolean removed = party.removeMember(target);
        if (!removed) return false;

        target.setParty(null);   // IMPORTANT
        return true;
    }

    // -----------------
    // GET PARTY
    // -----------------
    public Party getParty(String partyId) {
        return parties.get(partyId);
    }

    public void destroyParty(String partyId) {
        Party party = parties.remove(partyId);
        if (party == null) return;

        // remove party from all members
        for (PlayerEntity p : party.getMembers().values()) {
            p.setParty(null);
            NetworkService.gI().leaveParty(p);
        }

        // clear members
        party.getMembers().clear();
    }
}
