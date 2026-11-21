package lvhaoxuan.custom.cuilian;

import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.commander.Commander;
import lvhaoxuan.custom.cuilian.commander.CuiLianTabCompleter;
import lvhaoxuan.custom.cuilian.hook.AttributeHook;
import lvhaoxuan.custom.cuilian.listener.FurnaceListener;
import lvhaoxuan.custom.cuilian.listener.GemListener;
import lvhaoxuan.custom.cuilian.listener.MoveLevelListener;
import lvhaoxuan.custom.cuilian.listener.ProtectRuneListener;
import lvhaoxuan.custom.cuilian.loader.Loader;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.movelevel.MoveLevelHandle;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.runnable.ScriptRunnable;
import lvhaoxuan.custom.cuilian.runnable.SyncEffectRunnable;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NewCustomCuiLianPro extends JavaPlugin {

    public static String PROTECT_RUNE_JUDGE = "§a§l保护符: ";
    public static String LEVEL_JUDGE = "§e§l淬炼属性: ";
    public static NewCustomCuiLianPro ins;
    public static HashMap<Material, String> typesInBag = new HashMap<>();
    public static List<ItemType> types = new ArrayList<>();
    public static boolean otherEntitySuitEffect = false;
    public static boolean judgeOffHand = true;
    public static int displayNameFormat = 0;
    public static List<String> replaceLore = new ArrayList<>();

    // Gem socketing config fields
    public static boolean gemEnable = true;
    public static int gemMaxSlots = 3;
    public static int gemDefaultSlots = 1;
    public static String GEM_JUDGE = "§b§l宝石属性: ";
    public static String GEM_UNDER_LINE = "§b§l§m  §3§l§m  §9§l§m  §e§l宝石镶嵌§9§l§m  §3§l§m  §b§l§m  ";
    public static String GEM_SLOT_EMPTY_LORE = "§7◇ 空宝石槽位";
    public static String GEM_SLOT_FILLED_PREFIX = "§b◆ ";
    public static String gemInlayInvTitle = "§8装备宝石镶嵌";
    public static String gemRemoveInvTitle = "§8装备宝石拆卸";

    private static final Set<NamespacedKey> registeredRecipeKeys = new HashSet<>();

    @Override
    public void onEnable() {
        ins = this;
        Gem.initKeys(this);
        GemAPI.initKeys(this);

        getServer().getConsoleSender().sendMessage("§a[" + getName() + "] §e正在加载装备淬炼与宝石镶嵌插件 v" + getDescription().getVersion() + "...");

        AttributeHook.init(this);
        enableConfig();

        PluginCommand cmd = getServer().getPluginCommand("cuilian");
        if (cmd != null) {
            cmd.setExecutor(new Commander());
            cmd.setTabCompleter(new CuiLianTabCompleter());
        }

        getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectRuneListener(), this);
        getServer().getPluginManager().registerEvents(new MoveLevelListener(), this);
        getServer().getPluginManager().registerEvents(new GemListener(), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                SyncEffectRunnable.cleanupEntity(e.getPlayer());
            }
        }, this);

        setRecipe();

        // Run synchronously on the main thread (Paper 1.20.4 thread-safe!)
        Bukkit.getScheduler().runTaskTimer(this, new ScriptRunnable(), 20L, 4L);
        Bukkit.getScheduler().runTaskTimer(this, new SyncEffectRunnable(), 20L, 10L);

        getServer().getConsoleSender().sendMessage("§a[" + getName() + "] §a加载完成！");
    }

    @Override
    public void onDisable() {
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            SyncEffectRunnable.cleanupEntity(p);
        }
        clearRegisteredRecipes();
    }

    public static void enableConfig() {
        Message.loadMessages();
        Loader.loadConfig();
        Loader.loadItems();
        Loader.loadLevels();
        Loader.loadStones();
        Loader.loadGems();
        MoveLevelHandle.init();
    }

    private static void clearRegisteredRecipes() {
        for (NamespacedKey key : registeredRecipeKeys) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {
            }
        }
        registeredRecipeKeys.clear();
    }

    /**
     * Registers one modern NamespacedKey FurnaceRecipe per Material.
     * In Minecraft 1.13–1.20.4+, a single Material recipe matches all durabilities automatically.
     */
    public static void setRecipe() {
        clearRegisteredRecipes();
        Set<Material> seen = new HashSet<>();
        for (ItemType itemType : types) {
            if (itemType.type == null || !seen.add(itemType.type)) {
                continue;
            }
            NamespacedKey key = new NamespacedKey(ins, "cuilian_" + itemType.type.name().toLowerCase(Locale.ROOT));
            FurnaceRecipe recipe = new FurnaceRecipe(key, itemType.toItemStack(), itemType.type, 0.0f, 200);
            try {
                if (Bukkit.addRecipe(recipe)) {
                    registeredRecipeKeys.add(key);
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public static class ItemType {
        public final String typeInBag;
        public final String baseType;
        public final Material type;

        public ItemType(String typeInBag, String baseType) {
            this.typeInBag = typeInBag;
            this.baseType = baseType;
            if (baseType == null || baseType.trim().isEmpty()) {
                throw new IllegalArgumentException("物品材质名不能为空");
            }
            String matName = baseType.contains(":") ? baseType.split(":")[0].trim() : baseType.trim();
            if (ItemUtil.isNumeric(matName)) {
                throw new IllegalArgumentException("1.20+ 不支持数字物品ID: " + baseType);
            }
            this.type = Material.matchMaterial(matName);
            if (this.type == null || this.type == Material.AIR) {
                throw new IllegalArgumentException("未识别的物品材质: " + baseType);
            }
        }

        public ItemStack toItemStack() {
            return new ItemStack(this.type, 1);
        }
    }
}
