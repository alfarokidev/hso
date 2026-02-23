package game.guild;

import database.SQL;
import game.entity.player.PlayerEntity;
import game.inventory.GuildInventory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.item.BaseItem;
import model.item.EquipmentItem;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Guild {
    private int id;
    private String name;
    private String shortName;
    private int leader;
    private String leaderName;
    private List<GuildMember> members = new CopyOnWriteArrayList<>();
    private short icon;
    private long gold;
    private int gem;
    private String rules;
    private String slogan;
    private String notification;
    private int level = 1;
    private int experience = 0;
    private int maxMembers = 10;
    private long createdAt;
    private transient GuildInventory inventory;

    public Guild(String name, String shortName, PlayerEntity leader) {
        this.name = name;
        this.shortName = shortName;
        this.leader = leader.getId();
        this.leaderName = leader.getName();
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public boolean isMember(PlayerEntity player) {
        return members.stream()
                .anyMatch(m -> m.getPlayerId() == player.getId());
    }

    public boolean isLeader(PlayerEntity player) {
        return leader == player.getId();
    }


    public GuildMember getMember(int playerId) {
        return members.stream()
                .filter(m -> m.getPlayerId() == playerId)
                .findFirst()
                .orElse(null);
    }

    public GuildMember getMember(String playerName) {
        return members.stream()
                .filter(m -> m.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);
    }


    public void addExperience(int exp) {
        this.experience += exp;
        checkLevelUp();
    }

    public int getLevelPercent() {
        int requiredExp = level * 1000;
        if (requiredExp <= 0) return 0;

        int percent = (int) ((experience * 100L) / requiredExp);
        return Math.min(percent, 100);
    }

    public int donateGold(GuildMember m, long gold) {
        if (gold <= 0) return 0;

        int points = (int) (gold / 10_000);   // 10k gold = 1 point
        if (points <= 0) return 0;

        m.addContribution(points);
        m.setGoldContribution(gold);
        addGold(gold);
        addExperience(points * 2);
        return points;
    }

    public int donateGem(GuildMember m, int gem) {
        if (gem <= 0) return 0;

        int points = gem;   // 1 gem = 1 point

        m.addContribution(points);
        m.setGemContribution(gem);
        addGem(gem);
        addExperience(points * 2);

        return points;
    }


    public int donateItem(GuildMember m, BaseItem item, int amount) {
        if (item == null || amount <= 0) return 0;

        int basePoint = getItemContribution(item);
        int points = basePoint * amount;

        if (points <= 0) return 0;

        m.addContribution(points);
        addExperience(points * 2);

        return points;
    }

    public int getItemContribution(BaseItem item) {
        return switch (item.getCategory()) {
            case EQUIPMENT -> {
                EquipmentItem eq = (EquipmentItem) item;
                yield calcEquipmentDonation(eq);
            }
            case MATERIAL -> 3;
            case POTION -> 2;
        };
    }

    public int getItemCost(BaseItem item) {
        return switch (item.getCategory()) {
            case EQUIPMENT -> {
                EquipmentItem eq = (EquipmentItem) item;
                yield calcEquipmentCost(eq);
            }
            case MATERIAL -> 3 * 2;
            case POTION -> 2 * 2;
        };
    }

    public int calcEquipmentDonation(EquipmentItem item) {
        int getCost = calcEquipmentCost(item);
        return Math.max(1, getCost * 30 / 100);
    }

    public int calcEquipmentCost(EquipmentItem item) {
        int base = colorBaseCost(item.getColor());
        int lvCost = levelCost(item.getLevel());
        int pCost = plusCost(item.getPlus());

        return base + lvCost + pCost;
    }

    private int colorBaseCost(int color) {
        return switch (color) {
            case 0 -> 5;
            case 1 -> 15;
            case 2 -> 40;
            case 3 -> 120;
            case 4 -> 400;
            case 5 -> 3000;
            default -> 20;
        };
    }

    private int plusCost(int plus) {
        return plus * 150;  // +150 CP per +1
    }

    private int levelCost(int level) {
        return level * 10;   // +10 CP per level
    }

    public void addGold(long gold) {
        this.gold += gold;
    }

    public void addGem(int gem) {
        this.gem += gem;
    }

    public boolean spendGold(long amount) {
        if (gold >= amount) {
            gold -= amount;
            return true;
        }
        return false;
    }

    public boolean spendGem(int amount) {
        if (gem >= amount) {
            gem -= amount;
            return true;
        }
        return false;
    }

    private void checkLevelUp() {
        int requiredExp = level * 1000;
        while (experience >= requiredExp && level < 100) {
            experience -= requiredExp;
            level++;
            maxMembers += 5;
            requiredExp = level * 1000;
        }
    }

    public void createInventory() {
        inventory = new GuildInventory();
        inventory.setGuildId(id);
        try {

            SQL.save(inventory);

        } catch (SQLException e) {
            log.error("Failed create guild inventory for guild id {}", id, e);
        }
    }

    public void saveInventory() {
        try {

            SQL.update(inventory)
                    .where("guildId", id)
                    .execute();

            log.debug("Inventory used {}", inventory.used());
        } catch (SQLException e) {
            log.error("Failed save guild inventory for guild id {}", id, e);
        }
    }

    public void loadInventory() {
        try {
            inventory = SQL.from(GuildInventory.class)
                    .where("guildId", id)
                    .first();
        } catch (SQLException e) {
            log.error("Failed load guild inventory for guild id {}", id, e);
        }
    }
}