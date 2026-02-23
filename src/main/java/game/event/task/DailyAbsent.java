package game.event.task;

import game.entity.player.PlayerEntity;
import game.event.BaseEvent;
import game.event.GameEvent;
import lombok.Getter;
import lombok.Setter;
import model.item.BaseItem;
import model.item.ItemCategory;
import model.item.PotionItem;
import model.reward.Reward;
import service.NetworkService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Getter
@Setter
public class DailyAbsent extends BaseEvent {
    private final Map<Integer, LocalDateTime> claims = new ConcurrentHashMap<>();

    public DailyAbsent() {
        super(1, "DAILY ABSENT");
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onUpdate(long currentTime) {

    }

    @Override
    public void onEnd() {
    }

    public boolean claim(PlayerEntity p) {
        if (claims.containsKey(p.getId())) return false;

        List<Reward> rewards = new ArrayList<>();
        rewards.add(Reward.create(-2, 1000, ItemCategory.POTION));
        rewards.add(Reward.create(-1, 100000, ItemCategory.POTION));

        rewards.forEach(reward -> {
            p.getInventoryManager().addToBag(reward.getItem(), reward.getQuantity());
        });

        NetworkService.gI().sendRewardDialog(p, "Absensi Harian", rewards);
        p.getInventoryManager().updateInventory();
        claims.put(p.getId(), LocalDateTime.now());
        return true;
    }

    public void reset() { claims.clear();}
}
