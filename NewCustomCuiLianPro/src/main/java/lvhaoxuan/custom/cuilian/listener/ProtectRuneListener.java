package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.ProtectRune;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectRuneListener implements Listener {

    public static final Map<UUID, ProtectRune> pendingRunes = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (e.getRawSlot() < 0 || !e.isRightClick()) {
            return;
        }

        UUID uuid = p.getUniqueId();
        ItemStack item = e.getCurrentItem();
        InventoryType topType = e.getInventory().getType();
        boolean inPersonalBag = (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER);

        // Step 2: Player already selected a protect rune and is now right-clicking a target item
        if (pendingRunes.containsKey(uuid)) {
            if (!inPersonalBag) {
                p.sendMessage(Message.MUST_ADD_IN_BAG);
                return;
            }
            e.setCancelled(true);

            // Right-clicking an empty slot cancels cleanly (no NPE!)
            if (!ItemUtil.isNotEmpty(item)) {
                cancelWithMessage(p, Message.CANCEL_ADD);
                return;
            }

            ProtectRune protectRune = pendingRunes.get(uuid);
            if (protectRune == null || protectRune.item == null || protectRune.level == null) {
                cancelWithMessage(p, Message.CAN_NOT_ADD);
                return;
            }

            Level equipLevel = Level.byItemStack(item);
            int basicLevel = equipLevel != null ? equipLevel.value : 0;

            if (!CuiLianAPI.canCuiLian(item)
                    || !ItemUtil.inventoryHasSimilar(p.getInventory(), protectRune.item, 1)
                    || basicLevel < protectRune.level.value) {
                cancelWithMessage(p, Message.CAN_NOT_ADD);
                return;
            }

            ItemStack updated = CuiLianAPI.addProtectRune(item.clone(), protectRune);
            e.setCurrentItem(updated);
            ItemUtil.inventoryRemoveSimilar(p.getInventory(), protectRune.item, 1);

            pendingRunes.remove(uuid);
            p.sendMessage(Message.ADD_SUCCESS.replace("%s", protectRune.lore));
            p.closeInventory();
            return;
        }

        // Step 1: Player right-clicks a ProtectRune inside their inventory
        if (ItemUtil.isNotEmpty(item)) {
            ProtectRune protectRune = ProtectRune.byItemStack(item);
            if (protectRune != null) {
                if (!inPersonalBag || !ItemUtil.inventoryHasSimilar(p.getInventory(), protectRune.item, 1)) {
                    cancelWithMessage(p, Message.MUST_ADD_IN_BAG);
                    e.setCancelled(true);
                    return;
                }
                for (String line : Message.ADD_MESSAGE) {
                    p.sendMessage(line.replace("%s", protectRune.lore));
                }
                pendingRunes.put(uuid, protectRune);
                e.setCancelled(true);
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        pendingRunes.remove(e.getPlayer().getUniqueId());
    }

    private void cancelWithMessage(Player p, String msg) {
        pendingRunes.remove(p.getUniqueId());
        p.closeInventory();
        if (msg != null && !msg.isEmpty()) {
            p.sendMessage(msg);
        }
    }
}
