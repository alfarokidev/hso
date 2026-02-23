package game.entity.monster;

import game.entity.Position;
import game.event.EventManager;
import game.event.btf.BTF;
import game.event.btf.BTFState;
import game.guild.GuildManager;
import game.party.Party;
import game.skill.DamageContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import manager.ItemManager;
import manager.MonsterManager;
import manager.WorldManager;
import model.item.EquipmentItem;
import model.item.MaterialItem;
import model.item.PotionItem;
import model.monster.GuildMine;
import model.monster.Monster;
import service.NetworkService;
import game.entity.base.GameObjectType;
import game.entity.base.LivingEntity;
import game.entity.player.PlayerEntity;
import utils.NumberUtils;

import java.util.List;

import static game.entity.base.GameObjectType.PLAYER;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MonsterEntity extends LivingEntity {

    private int templateId;
    private MonsterType monsterType = MonsterType.NORMAL;
    private Position spawnPosition;
    private byte typeMove;
    private long respawnTime;
    private int refreshTime = 8;
    private long spawnTime;

    // MONSTER STATS
    private static final int BASE_DAMAGE = 50;
    private static final int DAMAGE_PER_LEVEL = 5;
    private static final double DAMAGE_EXPONENT = 1.2;
    private static final int BASE_DEFENSE = 10;
    private static final int DEFENSE_PER_LEVEL = 2;
    private static final double DEFENSE_EXPONENT = 1.1;

    public MonsterEntity() {
        super(GameObjectType.MONSTER);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void onSpawn() {
        isActive = true;
        if (isTower()) {
            refreshTime = Integer.MAX_VALUE;
        }

        respawnTime = System.currentTimeMillis();
        spawnTime = System.currentTimeMillis();
        recalculateStats();
        hp = maxHp;
        mp = maxMp;
        attackCooldown = 3000;
        position = new Position(spawnPosition.getX(), spawnPosition.getY());
        if (templateId == 64) {
            GuildMine guildMine;
            if ((guildMine = GuildManager.getInstance().getByMap(getMap().getId())) != null) {
                guild = GuildManager.getInstance().getGuildById(guildMine.getGuildId());
            }
        }
    }

    @Override
    public void onUpdate(long deltaTime) {
        super.onUpdate(deltaTime);
        if (isDead()) {
            handleRespawn();
        }

        long now = System.currentTimeMillis();
        if (target != null) {
            if (distanceTo(target) < 100) {
                if ((now - lastAttackTime) >= attackCooldown) {
                    attack(target);
                }
            } else {
                target = null;
            }
        }

    }

    @Override
    public void onDestroy() {
        target = null;
        isActive = false;
    }


    // ==================== COMBAT ====================

    @Override
    public void attack(LivingEntity target) {
        if (target == null || target.isDead() || !canAttack()) return;

        dealDamageTo(target);
    }

    @Override
    public void die(LivingEntity attacker) {
        hp = 0;
        respawnTime = System.currentTimeMillis() + (refreshTime * 1000L);

        if (templateId == 64) {


            if (attacker instanceof PlayerEntity p) {

                List<PlayerEntity> clones = getZone().getPlayers().stream().filter(LivingEntity::isClone).toList();
                for (PlayerEntity entity : clones) {
                    entity.setBot(null);
                    WorldManager.getInstance().leaveMap(entity);
                }

                if (p.getGuild() != null) {
                    guild = p.getGuild();
                    getZone().broadcast(player -> NetworkService.gI().sendMonsterInfo(player, this));

                    WorldManager.getInstance().createClone(p.getId(), new Position(getMapId(), getX(), getY()));
                    GuildMine guildMine = GuildManager.getInstance().getByMap(getMapId());
                    if (guildMine != null) {
                        guildMine.getGuards().clear();
                        guildMine.getGuards().add(attacker.getId());
                        guildMine.setGuildId(attacker.getGuild().getId());

                        WorldManager.getInstance().worldBroadcast(player -> NetworkService.gI().sendChatWorld(player, String.format("Guild %s has destroyed crystal mine at %s", attacker.getGuild().getShortName(), getMap().getName())));

                    }
                }
            }
        } else {

            if (getMap().isBattleMap()) {

                if (isTower()) {
                    BTF btf = EventManager.getInstance().getEvent(BTF.class);
                    if (btf != null && btf.getState() == BTFState.FIGHTING) {
                        btf.destroyTower(templateId);
                        WorldManager.getInstance().worldBroadcast(player -> NetworkService.gI().sendChatWorld(player, String.format("%s Telah menghancurkan %s", attacker.getName(), getName())));
                    }
                }

            } else {

                if (target != null) {
                    giveRewards(target);
                }

            }
        }

        target = null;
        //zone.broadcast(player -> NetworkService.gI().sendRemoveActor(player, this));
    }

    public long getLivingTimeInSecond() {
        if (spawnTime <= 0) return 0;
        return (System.currentTimeMillis() - spawnTime) / 1000;
    }

    @Override
    public void respawn() {
        spawnTime = System.currentTimeMillis();
        recalculateStats();
        hp = maxHp;
        mp = maxMp;
        position = new Position(spawnPosition.getX(), spawnPosition.getY());
        target = null;
        broadcastMovement();

    }

    @Override
    public void recalculateStats() {
        int baseStr = this.level;
        int baseDex = this.level;
        int baseVit = this.level;
        int baseInt = this.level;

        double multiplier = 1.0;
        switch (monsterType) {
            case ELITE -> multiplier = 2.5;
            case BOSS -> multiplier = 5.0;
            case RAID_BOSS -> multiplier = 15.0;
        }

        int baseDamage = BASE_DAMAGE + (this.level * DAMAGE_PER_LEVEL) + (int) Math.pow(this.level, DAMAGE_EXPONENT);
        baseDamage = (int) (baseDamage * multiplier);

        int baseDefense = BASE_DEFENSE + (this.level * DEFENSE_PER_LEVEL) + (int) Math.pow(this.level, DEFENSE_EXPONENT);
        baseDefense = (int) (baseDefense * multiplier);

        stats.reset();
        stats.setBasicDmg(baseDamage);
        stats.setDefense(baseDefense);
        stats.applyBaseAttributes(baseStr, baseDex, baseVit, baseInt, 0);
        stats.setReflectDamagePercent(0);
        stats.setEvadePercent(0);
        stats.setPenetrationPercent(0);

        updateMaxHp();
        updateMaxMp();
    }

    @Override
    protected int calculateBaseHp() {
        Monster temp = MonsterManager.getInstance().getMonster(templateId);
        int base = temp.getHp() + (level * 100);
        return (int) (base * getStatMultiplier());
    }

    @Override
    protected int calculateBaseMp() {
        int base = 50 + (level * 25);
        return (int) (base * getStatMultiplier());
    }

    private double getStatMultiplier() {
        return switch (monsterType) {
            case NORMAL -> 0.5;
            case ELITE -> 2.5;
            case BOSS -> 5.0;
            case RAID_BOSS -> 15.0;
        };
    }

    private void handleRespawn() {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= respawnTime) {
            List<Integer> btfMap = List.of(104, 105, 106, 107, 108);
            if (!btfMap.contains(getMap().getId())) {
                respawn();
            }
        }
    }

    // ==================== REWARDS ====================

    private void giveRewards(LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            long goldReward = (long) (level * 10L * getTypeGoldMultiplier());
            player.addGold(goldReward);

            if (NumberUtils.randomInt(1, 100) < 10) {
                EquipmentItem item = ItemManager.getInstance().randomEquipment(level);
                if (item != null) {
                    int chance = switch (item.getColor()) {
                        case 4 -> 15;
                        case 3 -> 25;
                        case 2 -> 50;
                        default -> 100;
                    };

                    if (NumberUtils.randomInt(1, 100) >= chance) return;

                    zone.dropItem(item, id, position, player.getId());
                }
            }

            if (NumberUtils.randomInt(1, 100) <= 5) {
                MaterialItem item = ItemManager.getInstance().getMaterial(NumberUtils.randomInt(0, 4));
                if (item != null) {
                    zone.dropItem(item, id, position, player.getId());
                }
            }

            if (NumberUtils.randomInt(1, 100) <= 7) {
                PotionItem item = ItemManager.getInstance().getPotion(NumberUtils.randomInt(0, 5));
                if (item != null) {
                    zone.dropItem(item, id, position, player.getId());
                }
            }
        }
    }


    private double getTypeGoldMultiplier() {
        return switch (monsterType) {
            case NORMAL -> 1.0;
            case ELITE -> 3.0;
            case BOSS -> 10.0;
            case RAID_BOSS -> 50.0;
        };
    }

    // ==================== EVENT HOOKS ====================

    @Override
    protected void onDamageTaken(DamageContext context) {
        if (context.getAttacker() instanceof PlayerEntity player) {

            target = player;

            // Calculate EXP for this hit
            long expReward = expCalculator.calculateHitExpReward(
                    getLevel(),
                    player.getLevel(),
                    context.getFinalDamage(),
                    getMaxHp(),
                    context.getDefenderHpBeforeHit(),
                    (player.getParty() != null && player.getParty().size() > 1),
                    isDead()
            );


            player.addExperience(expReward);

            long sharedExp = expReward * 30 / 100;
            Party party;
            if ((party = player.getParty()) != null && party.size() > 1) {
                // GIVE 10% EXP TO OTHER PARTY
                for (PlayerEntity member : party.getMembers().values()) {
                    if (member == null) continue;
                    if (member.getId() == player.getId()) continue;
                    if (!member.isOnline()) continue;
                    if (member.getZone() != player.getZone()) continue;

                    member.addExperience(sharedExp);
                }
            }
            zone.broadcast(p -> NetworkService.gI().sendFireMonster(p, context));

        }
    }

//    private long calculateExpReward(LivingEntity attacker, long damageDealt) {
//        // Base EXP from monster level (smooth scaling)
//        long baseExp = (long) (50 * Math.pow(level, 2.2));
//
//        // Level difference multiplier
//        int levelDiff = level - attacker.getLevel();
//
//        if (levelDiff >= 5) {
//            baseExp = (long) (baseExp * 1.3);
//        } else if (levelDiff >= 3) {
//            baseExp = (long) (baseExp * 1.15);
//        } else if (levelDiff >= 1) {
//            baseExp = (long) (baseExp * 1.05);
//        } else if (levelDiff <= -10) {
//            baseExp = (long) (baseExp * 0.05);
//        } else if (levelDiff <= -5) {
//            baseExp = (long) (baseExp * 0.15);
//        } else if (levelDiff <= -3) {
//            baseExp = (long) (baseExp * 0.4);
//        }
//
//        // Calculate EXP based on damage dealt (percentage of total EXP)
//        double damagePercent = (double) damageDealt / maxHp;
//        long expForThisHit = (long) (baseExp * damagePercent);
//
//        return Math.max(1, expForThisHit);
//    }

    @Override
    protected void onEvade(DamageContext context) {
        if (context.getAttacker().getType() == PLAYER) {
            zone.broadcast(player -> NetworkService.gI().sendFireMonster(player, context));
        }
    }

    public enum MonsterType {
        NORMAL(0), ELITE(1), BOSS(2), RAID_BOSS(3);
        @Getter
        private final int value;

        MonsterType(int value) {
            this.value = value;
        }

        public static MonsterType fromValue(int typeId) {
            for (MonsterType type : values()) {
                if (type.value == typeId) return type;
            }
            return null;
        }
    }

    public boolean isTower() {
        return templateId == 89 || templateId == 90 || templateId == 91 || templateId == 92;
    }

    public boolean isMyGuild(int guildId) {
        return guild != null && guild.getId() == guildId;
    }

}
