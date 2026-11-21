package lvhaoxuan.custom.cuilian.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static boolean isNotEmpty(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorList(List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                result.add(color(line));
            }
        }
        return result;
    }

    /**
     * Adds a glowing enchantment glint without showing the enchantment name in lore.
     */
    public static ItemStack addGlow(ItemStack item) {
        if (!isNotEmpty(item)) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack readItemStack(YamlConfiguration config, String path) {
        return readItemStack(config, path, true);
    }

    public static ItemStack readItemStack(YamlConfiguration config, String path, boolean defaultGlow) {
        if (config == null || !config.contains(path)) {
            return null;
        }
        String typeName = config.getString(path + ".Type", "PAPER");
        Material mat = Material.matchMaterial(typeName != null ? typeName.trim() : "PAPER");
        if (mat == null || mat == Material.AIR) {
            mat = Material.PAPER;
        }
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = config.getString(path + ".Name");
            if (name != null) {
                meta.setDisplayName(color(name));
            }
            List<String> lore = config.getStringList(path + ".Lore");
            if (!lore.isEmpty()) {
                meta.setLore(colorList(lore));
            }
            int cmd = config.getInt(path + ".CustomModelData", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            boolean glow = config.getBoolean(path + ".Glow", defaultGlow);
            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getItemInMainHand(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        EntityEquipment eq = entity.getEquipment();
        return eq != null ? eq.getItemInMainHand() : null;
    }

    public static void setItemInMainHand(LivingEntity entity, ItemStack item) {
        if (entity == null) {
            return;
        }
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            eq.setItemInMainHand(item);
        }
    }

    public static ItemStack getItemInOffHand(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        EntityEquipment eq = entity.getEquipment();
        return eq != null ? eq.getItemInOffHand() : null;
    }

    public static int countSimilar(Inventory inv, ItemStack target) {
        if (inv == null || !isNotEmpty(target)) {
            return 0;
        }
        int found = 0;
        for (ItemStack slot : inv.getContents()) {
            if (isNotEmpty(slot) && slot.isSimilar(target)) {
                found += slot.getAmount();
            }
        }
        return found;
    }

    public static boolean inventoryHasSimilar(Inventory inv, ItemStack target, int requiredAmount) {
        return countSimilar(inv, target) >= requiredAmount;
    }

    public static boolean inventoryRemoveSimilar(Inventory inv, ItemStack target, int amountToRemove) {
        if (!inventoryHasSimilar(inv, target, amountToRemove)) {
            return false;
        }
        int remaining = amountToRemove;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (isNotEmpty(slot) && slot.isSimilar(target)) {
                int take = Math.min(remaining, slot.getAmount());
                if (slot.getAmount() - take <= 0) {
                    inv.setItem(i, null);
                } else {
                    slot.setAmount(slot.getAmount() - take);
                    inv.setItem(i, slot);
                }
                remaining -= take;
            }
        }
        return remaining == 0;
    }

    public static int randomInt(int min, int max) {
        if (min >= max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static boolean rollChance(double chancePercent) {
        if (chancePercent <= 0.0) {
            return false;
        }
        if (chancePercent >= 100.0) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble(100.0) < chancePercent;
    }

    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i == 0 && c == '-' && str.length() > 1) {
                continue;
            }
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
