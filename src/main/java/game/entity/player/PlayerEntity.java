package game.entity.player;

import game.buff.BuffManager;
import game.entity.ai.Bot;
import game.equipment.EquipType;
import game.event.EventManager;
import game.event.btf.BTF;
import game.friend.FriendList;
import game.friend.FriendRequest;
import game.guild.Guild;
import game.guild.GuildManager;
import game.guild.GuildRequest;
import game.inventory.InventoryManager;
import game.party.Party;
import game.party.PartyRequest;
import game.pet.Pet;
import game.pet.PlayerPet;
import game.skill.DamageContext;
import language.LanguageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import lombok.extern.slf4j.Slf4j;
import manager.ItemManager;
import manager.WorldManager;
import model.item.*;
import model.menu.ConfirmDialog;
import model.menu.InputDialog;
import model.menu.Menu;
import model.npc.Go;
import model.shop.Shop;
import network.Message;
import network.Session;
import service.NetworkService;
import game.skill.SkillEntity;
import game.entity.base.GameObjectType;
import game.entity.base.LivingEntity;
import game.entity.Position;
import service.PlayerService;
import utils.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerEntity extends LivingEntity {
    private int uid;
    private int role;
    private long gold;
    private int gems;

    private short activePoint;
    private short arenaPoint;
    private boolean isOnline;
    private int potentialPoint;
    private int skillPoint;

    private int STR;
    private int DEX;
    private int VIT;
    private int INT;

    private byte[] body;
    private byte[][] rms;
    private byte[] skills;

    private Session session;

    // Get current menu
    private Menu currentMenu;
    private Stack<Menu> menuHistory = new Stack<>();

    private InputDialog inputDialog;
    private ConfirmDialog confirmDialog;
    private Shop currentNpcShop;
    private Bot bot;
    private boolean modeBot;
    private int currentItemUpgrade;
    private long lastUpgradeTime;
    private boolean teleport;

    // USED FOR BTF
    private short transformId = -1;
    private short totalKill;

    private Fashion fashion;
    private PlayerPet playerPet;
    private final Map<String, FriendRequest> friendRequests = new ConcurrentHashMap<>();
    private final Map<String, PartyRequest> partyRequests = new ConcurrentHashMap<>();
    private final Map<String, GuildRequest> guildRequests = new ConcurrentHashMap<>();

    private FriendList friendList;

    private Party party;
    private PartSettings partSettings;


    public PlayerEntity() {
        super(GameObjectType.PLAYER);
        this.rms = new byte[2][0];
        this.position = new Position((short) 50, (short) 50);
        buffManager = new BuffManager(this);
        partSettings = new PartSettings();

    }

    // Session management
    public void bindSession(Session session) {
        this.session = session;
        this.isOnline = true;
    }

    public void unbindSession() {
        this.session = null;
        if (!modeBot) {
            this.isOnline = false;
        }
    }

    public void send(Message m) {
        if (session != null) {
            session.send(m);
        }
    }

    public void initBot() {
        bot = new Bot(this);
        modeBot = true;
    }

    public void initial() {
        inventoryManager = new InventoryManager(this);

        if ((playerPet = PlayerService.gI().findPlayerPetById(id)) == null) {
            playerPet = new PlayerPet();
            playerPet.setPlayerId(id);
        }

        if ((friendList = PlayerService.gI().findFriendList(id)) == null) {
            friendList = new FriendList();
            friendList.setId(id);
        }

        skillData = HashMap.newHashMap(skills.length);
        for (int i = 0; i < skills.length; i++) {
            skillData.put((byte) i, new SkillEntity((byte) i, skills[i], (byte) role));
        }

        Guild pGuild;
        if ((pGuild = GuildManager.getInstance().getPlayerGuild(id)) != null) {
            guild = pGuild;
        }

        recalculateStats();
        typePK = -1;
        hp = maxHp;
        mp = maxMp;
        activePoint = 32000;
    }

    // Lifecycle overrides
    @Override
    public void onSpawn() {
        isActive = true;
        lastUpdateTime = System.currentTimeMillis();
        lastChangeMapTime = System.currentTimeMillis();
        attackCooldown = 500;
        recalculateStats();
        setOnline(true);
    }


    @Override
    public void onUpdate(long deltaTime) {
        super.onUpdate(deltaTime);

        if (modeBot && bot != null) {
            bot.update();
        }

        buffManager.update();
        friendRequests.values().removeIf(FriendRequest::isExpired);
        partyRequests.values().removeIf(PartyRequest::isExpired);
        guildRequests.values().removeIf(GuildRequest::isExpired);
    }


    @Override
    public void onDestroy() {
        unbindSession();
        // clearStatusEffects();
        this.isActive = false;
    }

    // Combat overrides
    @Override
    public void attack(LivingEntity target) {
        if (target == null || target.isDead() || !canAttack()) return;

        // Get basic attack skill
        SkillEntity basicAttack = skillData.get((byte) 0);

        if (basicAttack != null && basicAttack.isOnCooldown()) {
            return; // Basic attack on cooldown
        }

        dealDamageTo(target, basicAttack);

        if (basicAttack != null) {
            basicAttack.onUse();
        }

    }

    @Override
    protected void onDamageTaken(DamageContext context) {
        if (context.getAttacker().getType() == GameObjectType.MONSTER) {
            zone.broadcast(notify ->
                    NetworkService.gI().sendMonsterFire(notify, context.getAttacker(), this, context.getFinalDamage()));

        } else {
            zone.broadcast(notify ->
                    NetworkService.gI().sendFireObject(notify, context));
        }
    }

    @Override
    protected void onEvade(DamageContext context) {
        if (context.getAttacker().getType() == GameObjectType.MONSTER) {
            zone.broadcast(notify ->
                    NetworkService.gI().sendMonsterFire(notify, context.getAttacker(), this, 0));
        }
    }


    @Override
    public void die(LivingEntity attacker) {
        this.hp = 0;
        zone.broadcast(notify -> NetworkService.gI().sendPlayerDie(notify, attacker, this));

        if (getMap().isBattleMap()) {
            BTF btf = EventManager.getInstance().getEvent(BTF.class);
            if (btf != null) {
                if (attacker instanceof PlayerEntity p) {
                    p.setTotalKill((short) (p.getTotalKill() + 1));
                    getZone().broadcast(notify -> NetworkService.gI().sendUpdateMarkKiller(notify, p, 0));
                }

                btf.onPlayerDie(this);
                WorldManager.getInstance().worldBroadcast(player -> NetworkService.gI().sendChatWorld(player, String.format("%s was killed by %s [%s] ", getName(), attacker.getName(), getMap().getName())));
                return;
            }
        }


        if (modeBot) {
            if (!isClone) {
                Timer.schedule(() -> {
                    hp = maxHp;
                    zone.broadcast(notify -> NetworkService.gI().sendCharInfo(notify, this));
                }, 15000);

            } else {
                WorldManager.getInstance().leaveMap(this);
            }
        }
    }

    @Override
    public void respawn() {
        this.hp = maxHp;
        this.mp = maxMp;
    }

    @Override
    public void recalculateStats() {
        stats.calculate(this);
        updateMaxHp();
        updateMaxMp();
    }

    // Player-specific methods
    @Override
    public void addExperience(long exp) {
        super.addExperience(exp);
        int expToSend = (int) Math.min(exp, Integer.MAX_VALUE);
        zone.broadcast(player -> NetworkService.gI().sendUpdateExp(player, this, expToSend));

    }

    @Override
    protected void onLevelUp() {
        potentialPoint += 4;
        skillPoint += 1;
        zone.broadcast(player -> NetworkService.gI().sendLevelUp(player, this));
        if (!modeBot) {
            NetworkService.gI().sendMainCharInfo(this);
        }
    }

    public void addPotentialPoint(int index, int value) {
        if (value <= 0) return;
        if (potentialPoint < value) return;

        switch (index) {
            case 0 -> STR += value;
            case 1 -> DEX += value;
            case 2 -> VIT += value;
            case 3 -> INT += value;
            default -> {
                return;
            }
        }

        potentialPoint -= value;
        recalculateStats();
    }

    public boolean upgradeSkill(byte skillIndex, int value) {
        SkillEntity skill = skillData.get(skillIndex);

        if (skill == null) {
            NetworkService.gI().sendNoticeBox(getSession(), "Skill tidak ditemukan");
            return false;
        }

        if (skill.isMaxLevel()) {
            NetworkService.gI().sendNoticeBox(getSession(), "Level sudah maksimal");
            return false;
        }

        if ((skillPoint - value) < 0) {
            NetworkService.gI().sendNoticeBox(getSession(), "Point tidak mencukupi");
            return false;
        }

        if (!skill.canUpgrade((short) level, value)) {
            NetworkService.gI().sendNoticeBox(getSession(), "Level tidak mencukupi");
            return false;
        }

        if (!skill.upgrade((short) level, value)) {
            NetworkService.gI().sendNoticeBox(getSession(), "Level tidak mencukupi");
            return false;
        }

        skillPoint -= value;
        // Optional: recalc stats if passive
        if (skill.getType() == 2) {
            recalculateStats();
        }

        return true;
    }

    public void resetSkill() {
        for (SkillEntity skill : skillData.values()) {
            if (skill.getCurrentLevel() > 0) {
                skill.setCurrentLevel((byte) (skill.getSkillId() == 0 ? 1 : 0));
            }
        }
        skillPoint = level;
        recalculateStats();
    }

    public void setLevelTo(int newLevel) {
        this.level = newLevel;
        experience = 0;
        resetSkill();
        resetPotentialPoints();
        hp = getMaxHp();
        mp = getMaxMp();
        zone.broadcast(player -> NetworkService.gI().sendLevelUp(player, this));
        NetworkService.gI().sendMainCharInfo(this);
    }

    public void addGold(long amount) {
        this.gold += amount;
    }

    public void addGem(int amount) {
        this.gems += amount;
    }

    public boolean spendGold(long amount) {
        if (gold >= amount) {
            gold -= amount;
            return true;
        }
        return false;
    }

    public boolean spendGem(int amount) {
        if (gems >= amount) {
            gems -= amount;
            return true;
        }
        return false;
    }


    public boolean costMana(int amount) {
        if (mp < amount) {
            return false;
        }
        mp -= amount;
        return true;
    }

    public void resetPotentialPoints() {
        int totalFromLevel = (level - 1) * 4;

        STR = 4;
        DEX = 4;
        VIT = 4;
        INT = 4;

        potentialPoint = totalFromLevel;

        recalculateStats();
    }

    public int getHairId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_HAIR)) return -1;

        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_HAIR);
        if (item == null) return -1;
        return partSettings.isShowHair() ? switch (item.getId()) {
            case 4835 -> 166;
            case 4836 -> 167;
            case 4837 -> 168;
            case 4838 -> 169;
            case 4839 -> 170;
            case 4840 -> 171;
            default -> item.getPart() + 41;
        } : -1;
    }

    public Mount getMount() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_MOUNT)) return null;

        int itemId = inventoryManager.getWearing().item(EquipType.FASHION_MOUNT).getId();
        return switch (itemId) {
            case 4869 -> new Mount(183, 22);
            case 4870 -> new Mount(184, 22);
            case 4871 -> new Mount(185, 22);
            case 4872 -> new Mount(186, 22);
            case 4873 -> new Mount(187, 22);
            default -> null;
        };
    }

    public int getWingId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_WING)) return -1;

        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_WING);
        if (item == null) return -1;

        return partSettings.isShowWing() ? item.getPart() + 41 : -1;
    }

    public int getTitleId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_TITLE)) return -1;

        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_TITLE);
        if (item == null) return -1;

        return partSettings.isShowTitle() ? item.getPart() + 41 : -1;
    }

    public int getMaskId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_MASK)) return -1;

        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_MASK);
        if (item == null) return -1;

        return partSettings.isShowMask() ? item.getPart() + 41 : -1;
    }

    public int getCloakId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_CLOAK)) return -1;


        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_CLOAK);
        if (item == null) return -1;

        return partSettings.isShowCloak() ? item.getPart() + 41 : -1;
    }

    public int getWeaponId() {
        if (!inventoryManager.getWearing().hasEquipped(EquipType.FASHION_WEAPON)) return -1;

        EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_WEAPON);
        if (item == null) return -1;

        return partSettings.isShowWeapon() ? item.getPart() + 41 : -1;
    }

    public Pet getPet() {

        Pet pet;
        if ((pet = playerPet.getActivePet()) == null) {
            return null;
        }

        return pet;
    }

    public int[] getFashion() {

        int[] base = inventoryManager.getWearing().getBaseVisual();

        int[] hair = null;
        if (inventoryManager.getWearing().hasEquipped(EquipType.FASHION_HAIR)) {
            EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_HAIR);
            if (item != null) {
                Fashion f = ItemManager.getInstance().getFashion(item.getId());
                if (f != null) hair = f.getPart();
            }
        }

        int[] wing = null;
        if (inventoryManager.getWearing().hasEquipped(EquipType.FASHION_WING)) {
            EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_WING);
            if (item != null) {
                Fashion f = ItemManager.getInstance().getFashion(item.getId());
                if (f != null) wing = f.getPart();
            }
        }
        int[] body = null;
        if (inventoryManager.getWearing().hasEquipped(EquipType.FASHION_BODY)) {
            EquipmentItem item = inventoryManager.getWearing().item(EquipType.FASHION_BODY);
            if (item != null) {
                Fashion f = ItemManager.getInstance().getFashion(item.getId());
                if (f != null) body = f.getPart();
            }
        }


        int[] costume = null;
        if (inventoryManager.getWearing().hasEquipped(EquipType.COSTUME)) {
            EquipmentItem item = inventoryManager.getWearing().item(EquipType.COSTUME);
            if (item != null) {
                Fashion f = ItemManager.getInstance().getFashion(item.getId());
                if (f != null) costume = f.getPart();
            }
        }

        // ✅ COSTUME LAST = HIGHEST PRIORITY
        return mergeVisual(base, hair, body, costume, wing);
    }

    public int[] mergeVisual(int[] base, int[]... layers) {
        int size = 7;
        int[] result = new int[size];

        // start from base
        for (int i = 0; i < size; i++) {
            result[i] = (base != null && i < base.length) ? base[i] : -1;
        }

        // apply each layer (costume, hair, etc.)
        for (int[] layer : layers) {
            if (layer == null) continue;

            for (PartType p : PartType.values()) {
                int idx = p.getId();
                if (idx >= layer.length) continue;

                int v = layer[idx];

                if (v == -1) continue;   // keep

                if (v == -2) {
                    result[idx] = -2;
                    continue;
                }

                // v >= 0 → replace
                result[idx] = v;
            }
        }


        // Apply PART SETTING
        int[] fashionConfig = partSettings.getFashion();
        for (int i = 0; i < fashionConfig.length; i++) {
            if (fashionConfig[i] == -3) continue;

            result[i] = fashionConfig[i];

        }

        return result;
    }


    // Set menu with automatic parent tracking
    public void setCurrentMenu(Menu menu) {
        if (menu != null && currentMenu != null) {
            // Only add to history if moving to a different menu
            if (currentMenu.getId() != menu.getId()) {
                menuHistory.push(currentMenu);
            }
        }
        this.currentMenu = menu;
    }

    // Set menu without tracking (for root menus)
    public void setMenuDialogRoot(Menu menu) {
        clearMenuHistory();
        this.currentMenu = menu;
    }

    // Get parent menu (pops from history)
    public Menu getParentMenu() {
        return menuHistory.isEmpty() ? null : menuHistory.peek();
    }

    // Clear all menu state
    public void clearMenuHistory() {
        menuHistory.clear();
        currentMenu = null;
    }

    // Send menu to client
    public void sendMenuDialog(Menu menu) {
        if (menu == null) return;
        NetworkService.gI().sendMenu(this, menu);
    }

    // Close menu dialog
    public void closeMenuDialog() {
        clearMenuHistory();
    }

    // Check if has parent
    public boolean hasParentMenu() {
        return !menuHistory.isEmpty();
    }

    // Navigate back to parent
    public void navigateToParent() {
        if (menuHistory.isEmpty()) {
            closeMenuDialog();
            return;
        }

        // Pop the parent from history (removes it from stack)
        Menu parent = menuHistory.pop();

        // Set current menu directly without adding to history
        this.currentMenu = parent;
        sendMenuDialog(parent);
    }


    public void openInput(InputDialog input) {
        this.inputDialog = input;
        NetworkService.gI().sendInputDialog(this, input);
    }

    public void openConfirmDialog(ConfirmDialog confirmDialog) {
        this.confirmDialog = confirmDialog;
        NetworkService.gI().sendConfirmDialog(this);
    }


    public void sendMessageDialog(String message) {
        if (session == null) return;

        NetworkService.gI().sendNoticeBox(session, message);
    }

    // ==================== FRIEND REQUEST METHODS ====================

    public void addFriendRequest(String from) {
        friendRequests.put(from, new FriendRequest(from));
    }

    public FriendRequest getFriendRequest(String from) {
        return friendRequests.get(from);
    }

    public void removeFriendRequest(String from) {
        friendRequests.remove(from);
    }

    public void addPartyRequest(String from) {
        partyRequests.put(from, new PartyRequest(from));
    }

    public PartyRequest getPartyRequest(String from) {
        return partyRequests.get(from);
    }

    public void addGuildRequest(String from) {
        guildRequests.put(from, new GuildRequest(from));
    }

    public GuildRequest getGuildRequest(String from) {
        return guildRequests.get(from);
    }

    public void teleport(Go go) {
        setTeleport(true);
        WorldManager.getInstance().changeMap(this, go.getPosition());
    }

    public LanguageType getLanguage() {
        return session.getLanguage() == null ? LanguageType.ENGLISH : session.getLanguage();
    }
}
