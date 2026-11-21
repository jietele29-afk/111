package lvhaoxuan.custom.cuilian.loader;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.movelevel.MoveLevelHandle;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.Stone;
import lvhaoxuan.custom.cuilian.object.SuitEffect;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Loader {

    public static void loadLevels() {
        Level.levels.clear();
        File file = ensureResource("cuilian.yml");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            for (String key : config.getKeys(false)) {
                try {
                    Level level = Level.deserialize(config, key);
                    Level.levels.put(level.value, level);
                } catch (Exception ex) {
                    NewCustomCuiLianPro.ins.getLogger().warning("加载 cuilian.yml 等级 '" + key + "' 失败: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.SEVERE, "加载 cuilian.yml 失败", ex);
        }
    }

    public static void loadStones() {
        Stone.stones.clear();
        File file = ensureResource("stone.yml");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            for (String key : config.getKeys(false)) {
                try {
                    Stone stone = Stone.deserialize(config, key);
                    Stone.stones.put(key, stone);
                } catch (Exception ex) {
                    NewCustomCuiLianPro.ins.getLogger().warning("加载 stone.yml 淬炼石 '" + key + "' 失败: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.SEVERE, "加载 stone.yml 失败", ex);
        }
    }

    public static void loadGems() {
        Gem.gems.clear();
        File file = ensureResource("gem.yml");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            Gem.loadTools(config);
            Gem.loadFamilies(config);
            NewCustomCuiLianPro.ins.getLogger().info("已加载 " + Gem.gems.size() + " 颗镶嵌宝石（1~5阶）。");
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.SEVERE, "加载 gem.yml 失败", ex);
        }
    }

    public static void loadItems() {
        NewCustomCuiLianPro.types.clear();
        NewCustomCuiLianPro.typesInBag.clear();
        File file = ensureResource("items.yml");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            for (String key : config.getKeys(false)) {
                for (String strType : config.getStringList(key)) {
                    try {
                        NewCustomCuiLianPro.ItemType type = new NewCustomCuiLianPro.ItemType(key, strType);
                        NewCustomCuiLianPro.types.add(type);
                        NewCustomCuiLianPro.typesInBag.put(type.type, type.typeInBag);
                    } catch (IllegalArgumentException ex) {
                        NewCustomCuiLianPro.ins.getLogger().warning("跳过 items.yml 无效物品配置 '" + strType + "': " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.SEVERE, "加载 items.yml 失败", ex);
        }
    }

    public static void loadConfig() {
        File file = ensureResource("config.yml");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            NewCustomCuiLianPro.nativeAttributeEngine = config.getBoolean("NativeAttributeEngine", true);
            NewCustomCuiLianPro.otherEntitySuitEffect = config.getBoolean("OtherEntitySuitEffect", false);
            NewCustomCuiLianPro.PROTECT_RUNE_JUDGE = ItemUtil.color(config.getString("PROTECT_RUNE_JUDGE", "§a§l保护符: "));
            NewCustomCuiLianPro.LEVEL_JUDGE = ItemUtil.color(config.getString("LEVEL_JUDGE", "§e§l淬炼属性: "));
            MoveLevelHandle.moveLevelInvTitle = ItemUtil.color(config.getString("MoveLevelInvTitle", "§8淬炼移星"));
            NewCustomCuiLianPro.judgeOffHand = config.getBoolean("JudgeOffHand", false);
            NewCustomCuiLianPro.displayNameFormat = config.getInt("DisplayNameFormat", 1);
            NewCustomCuiLianPro.replaceLore = config.getStringList("ReplaceLore");

            // High-star particle settings
            NewCustomCuiLianPro.particleEnable = config.getBoolean("ParticleEffect.Enable", true);
            NewCustomCuiLianPro.particleMinLevel = Math.max(1, config.getInt("ParticleEffect.MinLevel", 6));

            // Gem settings
            NewCustomCuiLianPro.gemEnable = config.getBoolean("Gem.Enable", true);
            NewCustomCuiLianPro.gemMaxSlots = Math.max(1, config.getInt("Gem.MaxSlots", 3));
            NewCustomCuiLianPro.GEM_JUDGE = ItemUtil.color(config.getString("Gem.GEM_JUDGE", "§b§l宝石属性: "));
            NewCustomCuiLianPro.GEM_UNDER_LINE = ItemUtil.color(config.getString("Gem.UNDER_LINE",
                    "§b§l§m  §3§l§m  §9§l§m  §e§l宝石镶嵌§9§l§m  §3§l§m  §b§l§m  "));
            NewCustomCuiLianPro.GEM_SLOT_EMPTY_LORE = ItemUtil.color(config.getString("Gem.SLOT_EMPTY_LORE", "§7◇ 空宝石槽位"));
            NewCustomCuiLianPro.GEM_SLOT_FILLED_PREFIX = ItemUtil.color(config.getString("Gem.SLOT_FILLED_PREFIX", "§b◆ "));
            NewCustomCuiLianPro.gemRemoveInvTitle = ItemUtil.color(config.getString("Gem.RemoveInvTitle", "§8装备宝石拆卸"));
            NewCustomCuiLianPro.opItemsInvTitle = ItemUtil.color(config.getString("Gem.OpItemsInvTitle", "§8[OP] 淬炼与宝石全物品库"));
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.SEVERE, "加载 config.yml 失败", ex);
        }
    }

    public static String loadSuitEffectScriptStr(String name) {
        File dataFolder = NewCustomCuiLianPro.ins.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File folder = new File(dataFolder, "script");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, name);
        try {
            if (!file.exists()) {
                Files.writeString(file.toPath(), SuitEffect.defaultScript, StandardCharsets.UTF_8);
            }
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().warning("读取套装粒子脚本 '" + name + "' 失败: " + ex.getMessage());
            return "";
        }
    }

    public static ScriptEngine loadMoveLevelScript() {
        File file = ensureResource("movelevelscript.js");
        return loadScript(file);
    }

    public static ScriptEngine loadScript(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        ScriptEngine engine = createNashornEngine();
        if (engine == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            engine.eval(reader);
            return engine;
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.WARNING, "加载脚本失败: " + file.getName(), ex);
            return null;
        }
    }

    public static ScriptEngine createNashornEngine() {
        try {
            Class<?> factoryClass = Class.forName("de.eldoria.nashornjs.js.api.scripting.NashornScriptEngineFactory");
            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            return (ScriptEngine) factoryClass.getMethod("getScriptEngine").invoke(factory);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> factoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            return (ScriptEngine) factoryClass.getMethod("getScriptEngine").invoke(factory);
        } catch (Throwable ignored) {
        }
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        if (engine == null) {
            engine = manager.getEngineByName("javascript");
        }
        return engine;
    }

    private static File ensureResource(String fileName) {
        File dataFolder = NewCustomCuiLianPro.ins.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            NewCustomCuiLianPro.ins.saveResource(fileName, true);
        }
        return file;
    }
}
