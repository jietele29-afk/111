package lvhaoxuan.custom.cuilian.gui;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 27-slot Gem Extraction GUI for the player's currently held equipment.
 * Displays each socketed gem in slots 10..16; clicking a gem consumes 1 RemoveItem to extract it.
 */
public class GemRemoveGuiHolder implements InventoryHolder {

    private Inventory inventory;
    private final int heldSlot;

    public GemRemoveGuiHolder(int heldSlot) {
        this.heldSlot = heldSlot;
    }

    public int getHeldSlot() {
        return heldSlot;
    }

    public static void open(Player player) {
        if (!NewCustomCuiLianPro.gemEnable) {
            player.sendMessage(Message.GEM_DISABLED);
            return;
        }
        ItemStack equip = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isNotEmpty(equip)) {
            player.sendMessage(Message.GEM_REMOVE_NO_GEMS);
            return;
        }
        List<String> ids = GemAPI.getSocketedGemIds(equip);
        if (ids.isEmpty()) {
            player.sendMessage(Message.GEM_REMOVE_NO_GEMS);
            return;
        }
        int heldSlot = player.getInventory().getHeldItemSlot();
        GemRemoveGuiHolder holder = new GemRemoveGuiHolder(heldSlot);
        Inventory inv = Bukkit.createInventory(holder, 27, NewCustomCuiLianPro.gemRemoveInvTitle);
        holder.inventory = inv;
        holder.populate(equip);
        player.openInventory(inv);
    }

    public void populate(ItemStack equip) {
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            bg.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // Display equipment preview in slot 4
        if (ItemUtil.isNotEmpty(equip)) {
            inventory.setItem(4, equip.clone());
        }

        List<String> ids = GemAPI.getSocketedGemIds(equip);
        int slot = 10;
        for (int i = 0; i < ids.size() && slot <= 16; i++, slot++) {
            String id = ids.get(i);
            Gem gem = Gem.gems.get(id);
            ItemStack icon;
            if (gem != null && gem.item != null) {
                icon = gem.item.clone();
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(" ");
                    lore.add("§e拆卸成功率: §b" + gem.removeChance + "%");
                    lore.add("§a点击消耗 1 个宝石拆卸符取下此宝石");
                    meta.setLore(lore);
                    icon.setItemMeta(meta);
                }
            } else {
                icon = new ItemStack(Material.BARRIER);
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c未知宝石 (" + id + ")");
                    meta.setLore(List.of("§7点击清理此无效宝石记录"));
                    icon.setItemMeta(meta);
                }
            }
            inventory.setItem(slot, icon);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
