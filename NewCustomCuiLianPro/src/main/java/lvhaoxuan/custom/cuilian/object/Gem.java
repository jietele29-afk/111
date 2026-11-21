package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Gem {

    public static Map<String, Gem> gems = new LinkedHashMap<>();
    public static ItemStack punchItem = null;
    public static double punchChance = 80.0;
    public static ItemStack removeItem = null;

    public static NamespacedKey KEY_GEM_ITEM_ID;
    public static NamespacedKey KEY_TOOL_TYPE;

    public final String id;
    public final String name;
    public final ItemStack item;
    public final double chance;
    public final boolean failDestroyGem;
    public final boolean failDestroyEquip;
    public final int maxSameGem;
    public final int minCuiLianLevel;
    public final double removeChance;
    public final List<String> allowTypes;
    public final List<String> attribute;
    public final List<String> potionEffect;

    public Gem(String id, String name, ItemStack item, double chance,
               boolean failDestroyGem, boolean failDestroyEquip,
               int maxSameGem, int minCuiLianLevel, double removeChance,
               List<String> allowTypes, List<String> attribute, List<String> potionEffect) {
        this.id = id;
        this.name = name;
        this.item = item;
        this.chance = chance;
        this.failDestroyGem = failDestroyGem;
        this.failDestroyEquip = failDestroyEquip;
        this.maxSameGem = maxSameGem;
        this.minCuiLianLevel = minCuiLianLevel;
        this.removeChance = removeChance;
        this.allowTypes = allowTypes != null ? allowTypes : new ArrayList<>();
        this.attribute = attribute != null ? attribute : new ArrayList<>();
        this.potionEffect = potionEffect != null ? potionEffect : new ArrayList<>();
    }

    public static void initKeys(NewCustomCuiLianPro plugin) {
        KEY_GEM_ITEM_ID = new NamespacedKey(plugin, "gem_item_id");
        KEY_TOOL_TYPE = new NamespacedKey(plugin, "gem_tool_type");
    }

    public boolean allowsSlotType(String slotType) {
        if (allowTypes == null || allowTypes.isEmpty()) {
            return true;
        }
        for (String allowed : allowTypes) {
            if ("ALL".equalsIgnoreCase(allowed) || allowed.equalsIgnoreCase(slotType)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack createItemStack(int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(Math.max(1, amount));
        return clone;
    }

    public static ItemStack createPunchItem(int amount) {
        if (punchItem == null) {
            return null;
        }
        ItemStack clone = punchItem.clone();
        clone.setAmount(Math.max(1, amount));
        return clone;
    }

    public static ItemStack createRemoveItem(int amount) {
        if (removeItem == null) {
            return null;
        }
        ItemStack clone = removeItem.clone();
        clone.setAmount(Math.max(1, amount));
        return clone;
    }

    public static Gem byItemStack(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && KEY_GEM_ITEM_ID != null) {
            String id = meta.getPersistentDataContainer().get(KEY_GEM_ITEM_ID, PersistentDataType.STRING);
            if (id != null && gems.containsKey(id)) {
                return gems.get(id);
            }
        }
        for (Gem gem : gems.values()) {
            if (gem.item != null && gem.item.isSimilar(item)) {
                return gem;
            }
        }
        return null;
    }

    public static boolean isPunchItem(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && KEY_TOOL_TYPE != null) {
            String tool = meta.getPersistentDataContainer().get(KEY_TOOL_TYPE, PersistentDataType.STRING);
            if ("PUNCH".equals(tool)) {
                return true;
            }
        }
        return punchItem != null && punchItem.isSimilar(item);
    }

    public static boolean isRemoveItem(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && KEY_TOOL_TYPE != null) {
            String tool = meta.getPersistentDataContainer().get(KEY_TOOL_TYPE, PersistentDataType.STRING);
            if ("REMOVE".equals(tool)) {
                return true;
            }
        }
        return removeItem != null && removeItem.isSimilar(item);
    }

    public static Gem deserialize(YamlConfiguration config, String id) {
        String path = "Gems." + id;
        ItemStack stack = ItemUtil.readItemStack(config, path);
        if (stack != null && KEY_GEM_ITEM_ID != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_GEM_ITEM_ID, PersistentDataType.STRING, id);
                stack.setItemMeta(meta);
            }
        }
        String displayName = ItemUtil.color(config.getString(path + ".Name", id));
        double chance = config.getDouble(path + ".Chance", 80.0);
        boolean failDestroyGem = config.getBoolean(path + ".FailDestroyGem", true);
        boolean failDestroyEquip = config.getBoolean(path + ".FailDestroyEquip", false);
        int maxSameGem = config.getInt(path + ".MaxSameGem", 1);
        int minCuiLianLevel = config.getInt(path + ".MinCuiLianLevel", 0);
        double removeChance = config.getDouble(path + ".RemoveChance", 100.0);
        List<String> allowTypes = config.getStringList(path + ".AllowTypes");
        List<String> attribute = ItemUtil.colorList(config.getStringList(path + ".Attribute"));
        List<String> potionEffect = config.getStringList(path + ".PotionEffect");

        return new Gem(id, displayName, stack, chance, failDestroyGem, failDestroyEquip,
                maxSameGem, minCuiLianLevel, removeChance, allowTypes, attribute, potionEffect);
    }

    public static void loadTools(YamlConfiguration config) {
        punchChance = config.getDouble("Tools.PunchItem.Chance", 80.0);
        punchItem = ItemUtil.readItemStack(config, "Tools.PunchItem");
        if (punchItem != null && KEY_TOOL_TYPE != null) {
            ItemMeta meta = punchItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_TOOL_TYPE, PersistentDataType.STRING, "PUNCH");
                punchItem.setItemMeta(meta);
            }
        }

        removeItem = ItemUtil.readItemStack(config, "Tools.RemoveItem");
        if (removeItem != null && KEY_TOOL_TYPE != null) {
            ItemMeta meta = removeItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_TOOL_TYPE, PersistentDataType.STRING, "REMOVE");
                removeItem.setItemMeta(meta);
            }
        }
    }
}
