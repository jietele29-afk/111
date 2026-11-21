package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Level {

    public static HashMap<Integer, Level> levels = new HashMap<>();

    public Integer value;
    public List<String> lore;
    public HashMap<String, List<String>> attribute;
    public ProtectRune protectRune;
    public SuitEffect suitEffect;

    public Level(Integer value, List<String> lore, HashMap<String, List<String>> attribute,
                 ProtectRune protectRune, SuitEffect suitEffect) {
        this.value = value;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.attribute = attribute != null ? attribute : new HashMap<>();
        this.protectRune = protectRune;
        this.suitEffect = suitEffect;
        if (protectRune != null) {
            protectRune.level = this;
        }
    }

    public static int getMaxLevel() {
        int max = 0;
        for (Integer key : levels.keySet()) {
            if (key != null && key > max) {
                max = key;
            }
        }
        return max;
    }

    public String getFirstLore() {
        if (lore != null && !lore.isEmpty()) {
            return lore.get(0);
        }
        return "§e+" + value + "星";
    }

    public static Level byItemStack(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> rawLore = meta.getLore();
        if (rawLore == null || rawLore.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>(rawLore.size());
        for (String line : rawLore) {
            String s = line;
            if (NewCustomCuiLianPro.LEVEL_JUDGE != null && !NewCustomCuiLianPro.LEVEL_JUDGE.isEmpty()) {
                s = s.replace(NewCustomCuiLianPro.LEVEL_JUDGE, "");
            }
            if (NewCustomCuiLianPro.PROTECT_RUNE_JUDGE != null && !NewCustomCuiLianPro.PROTECT_RUNE_JUDGE.isEmpty()) {
                s = s.replace(NewCustomCuiLianPro.PROTECT_RUNE_JUDGE, "");
            }
            cleaned.add(s);
        }
        for (Level level : levels.values()) {
            if (level.lore != null && !level.lore.isEmpty() && cleaned.containsAll(level.lore)) {
                return level;
            }
        }
        return null;
    }

    public static Level byProtectRune(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> rawLore = meta.getLore();
        if (rawLore == null || rawLore.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>(rawLore.size());
        for (String line : rawLore) {
            String s = line;
            if (NewCustomCuiLianPro.LEVEL_JUDGE != null && !NewCustomCuiLianPro.LEVEL_JUDGE.isEmpty()) {
                s = s.replace(NewCustomCuiLianPro.LEVEL_JUDGE, "");
            }
            if (NewCustomCuiLianPro.PROTECT_RUNE_JUDGE != null && !NewCustomCuiLianPro.PROTECT_RUNE_JUDGE.isEmpty()) {
                s = s.replace(NewCustomCuiLianPro.PROTECT_RUNE_JUDGE, "");
            }
            cleaned.add(s);
        }
        for (Level level : levels.values()) {
            if (level.protectRune != null && level.protectRune.lore != null
                    && !level.protectRune.lore.isEmpty()
                    && cleaned.contains(level.protectRune.lore)) {
                return level;
            }
        }
        return null;
    }

    public static Level deserialize(YamlConfiguration config, String path) {
        HashMap<String, List<String>> map = new HashMap<>();
        map.put("Hand", new ArrayList<>());
        map.put("Helmet", new ArrayList<>());
        map.put("Chestplate", new ArrayList<>());
        map.put("Leggings", new ArrayList<>());
        map.put("Boots", new ArrayList<>());
        ConfigurationSection cs = config.getConfigurationSection(path + ".Attribute");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                map.put(key, ItemUtil.colorList(config.getStringList(path + ".Attribute." + key)));
            }
        }
        return new Level(
                Integer.parseInt(path),
                ItemUtil.colorList(config.getStringList(path + ".Lore")),
                map,
                ProtectRune.deserialize(config, path + ".ProtectRune"),
                SuitEffect.deserialize(config, path + ".SuitEffect")
        );
    }
}
