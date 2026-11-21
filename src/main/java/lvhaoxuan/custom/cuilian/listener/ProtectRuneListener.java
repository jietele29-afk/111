package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.ProtectRune;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * 1-click in-bag ProtectRune attachment:
 * Pick up a ProtectRune on cursor and Right-Click target equipment in inventory to apply immediately.
 */
public class ProtectRuneListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (e.getRawSlot() < 0 || !e.isRightClick()) {
            return;
        }
        InventoryType topType = e.getInventory().getType();
        if (topType != InventoryType.CRAFTING && topType != InventoryType.PLAYER) {
            return;
        }

        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();

        // Direct 1-click: cursor holds ProtectRune + right-click equipment
        if (ItemUtil.isNotEmpty(cursor) && ItemUtil.isNotEmpty(target)) {
            ProtectRune rune = ProtectRune.byItemStack(cursor);
            if (rune != null && rune.level != null && CuiLianAPI.canCuiLian(target)) {
                e.setCancelled(true);
                Level equipLevel = Level.byItemStack(target);
                int basicLevel = equipLevel != null ? equipLevel.value : 0;
                if (basicLevel < rune.level.value) {
                    p.sendMessage(Message.CAN_NOT_ADD);
                    return;
                }
                ItemStack updated = CuiLianAPI.addProtectRune(target.clone(), rune);
                e.setCurrentItem(updated);
                if (cursor.getAmount() <= 1) {
                    p.setItemOnCursor(null);
                } else {
                    cursor.setAmount(cursor.getAmount() - 1);
                    p.setItemOnCursor(cursor);
                }
                p.sendMessage(Message.ADD_SUCCESS.replace("%s", rune.lore));
                p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.9f, 1.2f);
                return;
            }
        }

        // Friendly tip if right-clicking a protect rune with empty cursor
        if (!ItemUtil.isNotEmpty(cursor) && ItemUtil.isNotEmpty(target)) {
            ProtectRune rune = ProtectRune.byItemStack(target);
            if (rune != null) {
                for (String line : Message.ADD_MESSAGE) {
                    p.sendMessage(line.replace("%s", rune.lore));
                }
            }
        }
    }
}
