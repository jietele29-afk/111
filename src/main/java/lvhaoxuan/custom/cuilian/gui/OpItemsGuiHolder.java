package lvhaoxuan.custom.cuilian.gui;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.Stone;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 54-slot paginated OP Item Browser GUI (/cuilian op).
 * Categories:
 *  - ALL / STONES (淬炼石) / PROTECTS (保护符) / GEMS (50种1-5级宝石) / TOOLS (粘合剂/拆卸符)
 * Left-Click: get 1 | Right-Click: get 16 | Shift+Left-Click: get 64
 */
public class OpItemsGuiHolder implements InventoryHolder {

    public enum Category {
        STONES("淬炼石"),
        PROTECTS("保护符"),
        GEMS("镶嵌宝石"),
        TOOLS("辅助道具");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public static final int SLOT_TAB_STONES = 45;
    public static final int SLOT_TAB_PROTECTS = 46;
    public static final int SLOT_TAB_GEMS = 47;
    public static final int SLOT_TAB_TOOLS = 48;
    public static final int SLOT_PREV = 51;
    public static final int SLOT_PAGE_INFO = 52;
    public static final int SLOT_NEXT = 53;

    private static final int PAGE_SIZE = 45;

    private Inventory inventory;
    private Category category;
    private int page;
    private List<ItemStack> currentItems = new ArrayList<>();

    public OpItemsGuiHolder(Category category, int page) {
        this.category = category;
        this.page = page;
    }

    public static void open(Player player, Category category, int page) {
        OpItemsGuiHolder holder = new OpItemsGuiHolder(category, Math.max(0, page));
        Inventory inv = Bukkit.createInventory(holder, 54,
                NewCustomCuiLianPro.opItemsInvTitle + " §7[" + category.label + "]");
        holder.inventory = inv;
        holder.render();
        player.openInventory(inv);
    }

    public Category getCategory() {
        return category;
    }

    public int getPage() {
        return page;
    }

    public ItemStack getOriginalItemAt(int slot) {
        int index = page * PAGE_SIZE + slot;
        if (slot < 0 || slot >= PAGE_SIZE || index < 0 || index >= currentItems.size()) {
            return null;
        }
        return currentItems.get(index).clone();
    }

    public int getMaxPage() {
        if (currentItems.isEmpty()) {
            return 0;
        }
        return (currentItems.size() - 1) / PAGE_SIZE;
    }

    public void render() {
        currentItems = collectItems(category);
        int maxPage = getMaxPage();
        if (page > maxPage) {
            page = maxPage;
        }
        inventory.clear();

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentItems.size());
        for (int i = start; i < end; i++) {
            ItemStack display = currentItems.get(i).clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() && meta.getLore() != null
                        ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                lore.add("§a左键拿取 1 个 §7| §e右键拿取 16 个 §7| §bShift+左键 64 个");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(i - start, display);
        }

        // Bottom bar filler
        ItemStack filler = makeButton(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int s = 45; s < 54; s++) {
            inventory.setItem(s, filler);
        }

        inventory.setItem(SLOT_TAB_STONES, makeButton(Material.COAL,
                "§e§l淬炼石", List.of("§7点击查看所有淬炼石"), category == Category.STONES));
        inventory.setItem(SLOT_TAB_PROTECTS, makeButton(Material.PAPER,
                "§a§l淬炼保护符", List.of("§7点击查看所有星级保护符"), category == Category.PROTECTS));
        inventory.setItem(SLOT_TAB_GEMS, makeButton(Material.EMERALD,
                "§b§l镶嵌宝石 (1~5级)", List.of("§7点击查看全部 10 大系列宝石"), category == Category.GEMS));
        inventory.setItem(SLOT_TAB_TOOLS, makeButton(Material.SLIME_BALL,
                "§d§l辅助道具", List.of("§7宝石粘合剂 / 宝石拆卸符"), category == Category.TOOLS));

        if (page > 0) {
            inventory.setItem(SLOT_PREV, makeButton(Material.ARROW,
                    "§e上一页", List.of("§7当前第 " + (page + 1) + "/" + (maxPage + 1) + " 页"), false));
        }
        inventory.setItem(SLOT_PAGE_INFO, makeButton(Material.BOOK,
                "§f第 §e" + (page + 1) + " §f/ §e" + (maxPage + 1) + " §f页",
                List.of("§7当前分类共 §b" + currentItems.size() + " §7种物品"), false));
        if (page < maxPage) {
            inventory.setItem(SLOT_NEXT, makeButton(Material.ARROW,
                    "§e下一页", List.of("§7当前第 " + (page + 1) + "/" + (maxPage + 1) + " 页"), false));
        }
    }

    private List<ItemStack> collectItems(Category cat) {
        List<ItemStack> list = new ArrayList<>();
        switch (cat) {
            case STONES -> {
                for (Stone stone : Stone.stones.values()) {
                    if (stone != null && ItemUtil.isNotEmpty(stone.item)) {
                        list.add(stone.item.clone());
                    }
                }
            }
            case PROTECTS -> {
                List<Level> sorted = new ArrayList<>(Level.levels.values());
                sorted.sort(Comparator.comparingInt(l -> l.value));
                for (Level lv : sorted) {
                    if (lv.protectRune != null && ItemUtil.isNotEmpty(lv.protectRune.item)) {
                        list.add(lv.protectRune.item.clone());
                    }
                }
            }
            case GEMS -> {
                for (Gem gem : Gem.gems.values()) {
                    if (gem != null && ItemUtil.isNotEmpty(gem.item)) {
                        list.add(gem.item.clone());
                    }
                }
            }
            case TOOLS -> {
                if (ItemUtil.isNotEmpty(Gem.binderItem)) {
                    list.add(Gem.binderItem.clone());
                }
                if (ItemUtil.isNotEmpty(Gem.removeItem)) {
                    list.add(Gem.removeItem.clone());
                }
            }
        }
        return list;
    }

    private ItemStack makeButton(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        if (glow) {
            ItemUtil.addGlow(item);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
