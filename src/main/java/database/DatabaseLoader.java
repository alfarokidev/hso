package database;

import game.guild.Guild;
import manager.LanguageManager;
import model.config.AttributeConfig;
import model.monster.GuildMine;
import game.guild.GuildManager;
import game.pet.PetManager;
import model.item.Fashion;
import lombok.extern.slf4j.Slf4j;
import manager.*;
import model.item.*;
import model.map.MapData;
import model.map.MapName;
import model.monster.Monster;
import model.npc.NpcData;
import model.config.SVConfig;
import model.pet.PetData;
import model.shop.Shop;
import model.skill.Skill;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DatabaseLoader {

    private DatabaseLoader() {
    }

    private static class Holder {
        private static final DatabaseLoader INSTANCE = new DatabaseLoader();
    }

    public static DatabaseLoader getInstance() {
        return DatabaseLoader.Holder.INSTANCE;
    }

    public void loadAll() {
        LanguageManager.getInstance().loadLanguages();

        loadEquipment();
        loadPotion();
        loadMaterial();
        loadItemOption();
        loadItemFashion();
        loadSvConfig();
        loadGuild();
        loadPet();
        loadMap(false);
        loadMonster(false);
        loadSkill(false);
        loadNpc(false);
        loadShop(false);

    }

    public void loadEquipment() {
        try {
            List<EquipmentItem> items = SQL.from(EquipmentItem.class).get();
            items.forEach(
                    item -> {
                        ItemManager.getInstance().addEquipment(item);
                    }
            );
            log.debug("{} Equipment loaded", items.size());
        } catch (SQLException e) {
            log.error("loadEquipment() Failed: {}", e.getMessage());
        }
    }

    public void loadPotion() {
        try {
            List<PotionItem> items = SQL.from(PotionItem.class).get();
            items.forEach(
                    item -> ItemManager.getInstance().addPotion(item)
            );
            log.debug("{} Potion loaded", items.size());
        } catch (SQLException e) {
            log.error("loadPotion() Failed: {}", e.getMessage());
        }
    }

    public void loadMaterial() {
        try {
            List<MaterialItem> items = SQL.from(MaterialItem.class).get();
            items.forEach(
                    item -> ItemManager.getInstance().addMaterial(item)
            );

            log.debug("{} Material loaded", items.size());
        } catch (SQLException e) {
            log.error("loadMaterial() Failed: {}", e.getMessage());
        }
    }

    public void loadItemOption() {
        try {
            List<ItemOption> items = SQL.from(ItemOption.class).table("option_item").get();
            items.forEach(
                    item -> ItemManager.getInstance().addItemOption(item)
            );
            log.debug("{} ItemOption loaded", items.size());
        } catch (SQLException e) {
            log.error("loadItemOption() Failed: {}", e.getMessage());
        }
    }

    public void loadItemFashion() {
        try {
            List<Fashion> items = SQL.from(Fashion.class).table("fashion").get();
            items.forEach(
                    item -> ItemManager.getInstance().addFashion(item)
            );
            log.debug("{} ItemFashion loaded", items.size());
        } catch (SQLException e) {
            log.error("loadItemFashion() Failed: {}", e.getMessage());
        }
    }


    public void loadSvConfig() {
        try {
            SVConfig cfg = SQL.from(SVConfig.class).table("sv_config").first();
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.setSvConfig(cfg);
            log.debug("SvConfig loaded && {} pet", cfg.getPetTemplate().size());

            List<AttributeConfig> attributeConfigs = SQL.from(AttributeConfig.class).get();
            attributeConfigs.forEach(configManager::addAttributeConfig);

            log.debug("AttributeConfig loaded {}", attributeConfigs.size());
        } catch (SQLException e) {
            log.error("loadConfig() Failed: {}", e.getMessage());
        }
    }

    public void loadMap(boolean isReload) {
        try {
            List<MapData> items = SQL.from(MapData.class).get();
            if (isReload) {
                WorldManager.getInstance().reloadMapData(items);
            } else {
                items.forEach(
                        map -> WorldManager.getInstance().addMapData(map)
                );
                log.info("{} MapData loaded", items.size());
            }

            List<MapName> names = SQL.from(MapName.class).get();
            if (isReload) {
                WorldManager.getInstance().mapNames.clear();
            }
            names.forEach(name -> WorldManager.getInstance().addMapName(name));


        } catch (SQLException e) {
            log.error("loadMap() Failed: {}", e.getMessage());
        }

    }

    public void loadMonster(boolean isReload) {
        try {
            List<Monster> items = SQL.from(Monster.class).get();

            if (isReload) MonsterManager.getInstance().clear();

            items.forEach(
                    map -> {
                        MonsterManager.getInstance().addMonster(map);
                    }
            );
            log.info("{} Monster loaded", items.size());
        } catch (SQLException e) {
            log.error("loadMonster() Failed: {}", e.getMessage());
        }

    }

    public void loadSkill(boolean isReload) {
        try {
            if (isReload) SkillManager.getInstance().clear();

            List<Skill> items = SQL.from(Skill.class).get();
            Map<Integer, Map<Byte, Skill>> skills = new HashMap<>();
            for (Skill skill : items) {
                skills
                        .computeIfAbsent(skill.role, r -> new HashMap<>())
                        .put(skill.sid, skill);
            }

            skills.forEach(
                    (role, skill) -> SkillManager.getInstance().addSkill(role, skill)
            );

            log.info("{} Skill loaded", items.size());
        } catch (SQLException e) {
            log.error("loadSkill() Failed: {}", e.getMessage());
        }

    }

    public void loadNpc(boolean isReload) {
        try {
            if (isReload) NpcManager.getInstance().clear();

            List<NpcData> items = SQL.from(NpcData.class).table("npc").get();

            items.forEach(
                    npc -> {
                        NpcManager.getInstance().addNpc(npc);
                    }
            );
            log.info("{} NPC loaded", items.size());
        } catch (SQLException e) {
            log.error("loadNPC() Failed: {}", e.getMessage());
        }
    }

    public void loadShop(boolean isReload) {
        try {
            if (isReload) ShopManager.getInstance().clear();

            List<Shop> items = SQL.from(Shop.class).get();

            items.forEach(
                    s -> {
                        ShopManager.getInstance().addShop(s);
                    }
            );
            log.info("{} Shop loaded", items.size());
        } catch (SQLException e) {
            log.error("loadShop() Failed: {}", e.getMessage());
        }
    }

    public void loadPet() {
        try {


            List<PetData> items = SQL.from(PetData.class).get();

            items.forEach(
                    s -> {
                        PetManager.getInstance().addPet(s);
                    }
            );
            log.info("{} Pet loaded", items.size());
        } catch (SQLException e) {
            log.error("loadPet() Failed: {}", e.getMessage());
        }
    }


    public void loadGuild() {
        try {

            List<Guild> items = SQL.from(Guild.class).get();
            GuildManager.getInstance().setGuilds(items);
            log.info("{} Guilds loaded", items.size());

            List<GuildMine> crystals = SQL.from(GuildMine.class).get();
            GuildManager.getInstance().setGuildCrystal(crystals);
            log.info("{} Guild Mines loaded", crystals.size());


        } catch (SQLException e) {
            log.error("loadGuild() Failed: {}", e.getMessage());
        }
    }

    public synchronized void reloadAll(Runnable onComplete) {
        ItemManager.getInstance().clearAll();
        loadEquipment();
        loadPotion();
        loadMaterial();
        loadItemOption();
        loadItemFashion();
        //loadSvConfig();

        loadMap(true);
        loadMonster(true);
        loadSkill(true);
        loadNpc(true);
        loadShop(true);

        if (onComplete != null) {
            onComplete.run();
        }
    }

}
