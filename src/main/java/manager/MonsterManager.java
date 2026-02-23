package manager;

import model.monster.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MonsterManager {
    private final ConcurrentHashMap<Integer, Monster> monsters = new ConcurrentHashMap<>();

    private MonsterManager() {
    }

    private static class Holder {
        private static final MonsterManager INSTANCE = new MonsterManager();
    }

    public static MonsterManager getInstance() {
        return MonsterManager.Holder.INSTANCE;
    }

    public void addMonster(Monster monster) {
        monsters.put(monster.getMid(), monster);
    }

    public Monster getMonster(int id) {
        return monsters.get(id);
    }

    public List<Monster> getTemplates() {
        return new ArrayList<>(monsters.values());
    }

    public void clear() {
        monsters.clear();
    }
}
