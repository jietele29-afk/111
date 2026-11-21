package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Gem {

    private static final String[] ROMAN_TIERS = {"", "I", "II", "III", "IV", "V"};
    private static final String[] CN_TIERS = {"", "一阶", "二阶", "三阶", "四阶", "五阶"};

    public static Map<String, Gem> gems = new LinkedHashMap<>();
    public static ItemStack binderItem = null;
    public static double binderChance = 100.0;
    public static int binderNeedAmount = 5;
    public static ItemStack removeItem = null;

    public static NamespacedKey KEY_GEM_ITEM_ID;
    public static NamespacedKey KEY_TOOL_TYPE;

    public final String id;          // e.g. qingling_1
    public final String family;      // e.g. qingling
    public final int tier;           // 1..5
    public final String name;        // e.g. §a§l青灵石 I
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

    public Gem(String id, String family, int tier, String name, ItemStack item, double chance,
               boolean failDestroyGem, boolean failDestroyEquip,
               int maxSameGem, int minCuiLianLevel, double removeChance,
               List<String> allowTypes, List<String> attribute, List<String> potionEffect) {
        this.id = id;
        this.family = family;
        this.tier = tier;
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

    public Gem getNextTier() {
        if (family == null || tier >= 5) {
            return null;
        }
        return gems.get(family + "_" + (tier + 1));
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

    public static ItemStack createBinderItem(int amount) {
        if (binderItem == null) {
            return null;
        }
        ItemStack clone = binderItem.clone();
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

    public static boolean isBinderItem(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && KEY_TOOL_TYPE != null) {
            String tool = meta.getPersistentDataContainer().get(KEY_TOOL_TYPE, PersistentDataType.STRING);
            if ("BINDER".equals(tool)) {
                return true;
            }
        }
        return binderItem != null && binderItem.isSimilar(item);
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

    public static void loadTools(YamlConfiguration config) {
        binderChance = config.getDouble("Tools.BinderItem.Chance", 100.0);
        binderNeedAmount = Math.max(2, config.getInt("Tools.BinderItem.NeedAmount", 5));
        binderItem = ItemUtil.readItemStack(config, "Tools.BinderItem", true);
        if (binderItem != null && KEY_TOOL_TYPE != null) {
            ItemMeta meta = binderItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_TOOL_TYPE, PersistentDataType.STRING, "BINDER");
                binderItem.setItemMeta(meta);
            }
        }

        removeItem = ItemUtil.readItemStack(config, "Tools.RemoveItem", true);
        if (removeItem != null && KEY_TOOL_TYPE != null) {
            ItemMeta meta = removeItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_TOOL_TYPE, PersistentDataType.STRING, "REMOVE");
                removeItem.setItemMeta(meta);
            }
        }
    }

    /**
     * Loads all 5-tier gem families from gem.yml -> GemFamilies.
     */
    public static void loadFamilies(YamlConfiguration config) {
        ConfigurationSection families = config.getConfigurationSection("GemFamilies");
        if (families == null) {
            return;
        }
        for (String familyKey : families.getKeys(false)) {
            String base = "GemFamilies." + familyKey;
            String baseDisplay = ItemUtil.color(config.getString(base + ".DisplayName", familyKey));
            String typeName = config.getString(base + ".Type", "EMERALD");
            Material mat = Material.matchMaterial(typeName != null ? typeName.trim() : "EMERALD");
            if (mat == null || mat == Material.AIR || mat.isBlock()) {
                mat = Material.EMERALD;
            }
            int cmd = config.getInt(base + ".CustomModelData", 0);
            double chance = config.getDouble(base + ".Chance", 100.0);
            boolean failDestroyGem = config.getBoolean(base + ".FailDestroyGem", false);
            boolean failDestroyEquip = config.getBoolean(base + ".FailDestroyEquip", false);
            int maxSameGem = config.getInt(base + ".MaxSameGem", 2);
            int minCuiLianLevel = config.getInt(base + ".MinCuiLianLevel", 0);
            double removeChance = config.getDouble(base + ".RemoveChance", 100.0);
            List<String> allowTypes = config.getStringList(base + ".AllowTypes");
            String desc = ItemUtil.color(config.getString(base + ".Description", ""));

            ConfigurationSection levelsSec = config.getConfigurationSection(base + ".Levels");
            if (levelsSec == null) {
                continue;
            }
            for (String tierStr : levelsSec.getKeys(false)) {
                int tier;
                try {
                    tier = Integer.parseInt(tierStr);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (tier < 1 || tier > 5) {
                    continue;
                }
                String tierPath = base + ".Levels." + tierStr;
                List<String> attrs = ItemUtil.colorList(config.getStringList(tierPath + ".Attribute"));
                List<String> potions = config.getStringList(tierPath + ".PotionEffect");

                String gemId = familyKey + "_" + tier;
                String roman = tier < ROMAN_TIERS.length ? ROMAN_TIERS[tier] : String.valueOf(tier);
                String cnTier = tier < CN_TIERS.length ? CN_TIERS[tier] : (tier + "阶");
                String fullName = baseDisplay + " §e" + roman + " §7(" + cnTier + ")";

                ItemStack stack = new ItemStack(mat, 1);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(fullName);
                    if (cmd > 0) {
                        meta.setCustomModelData(cmd);
                    }
                    List<String> lore = new ArrayList<>();
                    if (!desc.isEmpty()) {
                        lore.add(desc);
                    }
                    lore.add("§e品质等级：§b" + cnTier + " (" + tier + "/5)");
                    lore.add("§e适用部位：§f" + formatAllowTypes(allowTypes));
                    lore.add("§e属性加成：");
                    for (String a : attrs) {
                        lore.add("§7  - " + a);
                    }
                    lore.add("§e用法：§a左键拿起，直接右键点击背包内装备即可镶嵌");
                    if (tier < 5) {
                        lore.add("§d进阶：§7使用「宝石粘合剂」可将 5 颗同级宝石合成为 " + CN_TIERS[tier + 1]);
                    } else {
                        lore.add("§6✦ 已达最高品阶（五阶圆满）");
                    }
                    meta.setLore(lore);
                    if (KEY_GEM_ITEM_ID != null) {
                        meta.getPersistentDataContainer().set(KEY_GEM_ITEM_ID, PersistentDataType.STRING, gemId);
                    }
                    stack.setItemMeta(meta);
                }
                ItemUtil.addGlow(stack);

                Gem gem = new Gem(gemId, familyKey, tier, fullName, stack, chance,
                        failDestroyGem, failDestroyEquip, maxSameGem, minCuiLianLevel,
                        removeChance, allowTypes, attrs, potions);
                gems.put(gemId, gem);
            }
        }
    }

    private static String formatAllowTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return "全部装备";
        }
        List<String> names = new ArrayList<>();
        for (String t : types) {
            switch (t.toLowerCase()) {
                case "hand" -> names.add("武器");
                case "helmet" -> names.add("头盔");
                case "chestplate" -> names.add("胸甲");
                case "leggings" -> names.add("护腿");
                case "boots" -> names.add("靴子");
                case "all" -> names.add("全部装备");
                default -> names.add(t);
            }
        }
        return String.join("/", names);
    }
}
