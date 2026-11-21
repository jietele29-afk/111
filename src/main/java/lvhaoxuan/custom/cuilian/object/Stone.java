package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Stone {

    public static HashMap<String, Stone> stones = new HashMap<>();

    public Map<Level, Double> chance;
    public ItemStack item;
    public String id;
    public LevelDrop dropLevel;
    public int riseLevel;

    public Stone(ItemStack item, String id, LevelDrop dropLevel, int riseLevel, Map<Level, Double> chance) {
        this.item = item;
        this.id = id;
        this.dropLevel = dropLevel;
        this.riseLevel = riseLevel;
        this.chance = chance;
    }

    public static Stone byItemStack(ItemStack item) {
        if (ItemUtil.isNotEmpty(item)) {
            for (Stone stone : stones.values()) {
                if (stone.item != null && stone.item.isSimilar(item)) {
                    return stone;
                }
            }
        }
        return null;
    }

    public static Stone deserialize(YamlConfiguration config, String path) {
        Map<Level, Double> map = new HashMap<>();
        ConfigurationSection cs = config.getConfigurationSection(path + ".Chance");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                try {
                    Level level = Level.levels.get(Integer.parseInt(key));
                    if (level != null) {
                        map.put(level, config.getDouble(path + ".Chance." + key));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ItemStack stack = ItemUtil.readItemStack(config, path);
        String dropStr = config.getString(path + ".dropLevel", "0");
        int rise = config.getInt(path + ".riseLevel", 1);
        return new Stone(stack, path, new LevelDrop(dropStr), rise, map);
    }

    public static class LevelDrop {
        public int min;
        public int max;

        public LevelDrop(String dropStr) {
            if (dropStr == null || dropStr.trim().isEmpty()) {
                init(0);
                return;
            }
            String trimmed = dropStr.trim();
            if (trimmed.contains(" ")) {
                String[] args = trimmed.split("\\s+");
                try {
                    init(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    init(0);
                }
            } else {
                try {
                    init(Integer.parseInt(trimmed));
                } catch (NumberFormatException e) {
                    init(0);
                }
            }
        }

        public void init(int drop) {
            this.min = Math.max(0, drop);
            this.max = Math.max(0, drop);
        }

        public void init(int a, int b) {
            this.min = Math.max(0, Math.min(a, b));
            this.max = Math.max(0, Math.max(a, b));
        }

        public int toInteger() {
            return min == max ? min : ItemUtil.randomInt(min, max);
        }
    }
}
