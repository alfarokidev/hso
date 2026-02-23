package manager;

import model.npc.NpcData;

import java.util.HashMap;
import java.util.Map;

public class NpcManager {

    private final Map<Integer, NpcData> npcData = new HashMap<>();

    private NpcManager() {
    }

    private static class Holder {
        private static final NpcManager INSTANCE = new NpcManager();
    }

    public static NpcManager getInstance() {
        return NpcManager.Holder.INSTANCE;
    }

    public void addNpc(NpcData npc) {
        if (npc == null) return;

        npcData.putIfAbsent(npc.getId(), npc);
    }

    public NpcData getNpc(int id) {
        return npcData.get(id);
    }

    public void clear() {
        npcData.clear();
    }
}
