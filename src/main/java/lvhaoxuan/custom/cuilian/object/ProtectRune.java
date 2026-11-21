package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class ProtectRune {

    public ItemStack item;
    public String lore;
    public Level level;

    public ProtectRune(ItemStack item, String lore) {
        this.item = item;
        this.lore = lore;
    }

    public static ProtectRune byItemStack(ItemStack item) {
        if (ItemUtil.isNotEmpty(item)) {
            for (Level level : Level.levels.values()) {
                if (level.protectRune != null && level.protectRune.item != null
                        && level.protectRune.item.isSimilar(item)) {
                    return level.protectRune;
                }
            }
        }
        return null;
    }

    public static ProtectRune deserialize(YamlConfiguration config, String path) {
        if (config.get(path) == null) {
            return null;
        }
        ItemStack stack = ItemUtil.readItemStack(config, path);
        String addLore = ItemUtil.color(config.getString(path + ".AddLore", ""));
        return new ProtectRune(stack, addLore);
    }
}
