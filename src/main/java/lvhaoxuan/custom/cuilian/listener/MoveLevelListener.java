package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.gui.MoveLevelGuiHolder;
import lvhaoxuan.custom.cuilian.movelevel.MoveLevelHandle;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.script.Invocable;
import java.util.Map;

public class MoveLevelListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MoveLevelGuiHolder)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = e.getRawSlot();
        if (rawSlot < 0) {
            return;
        }

        // Top inventory interaction
        if (rawSlot < e.getInventory().getSize()) {
            if (rawSlot == MoveLevelHandle.SLOT_LEFT || rawSlot == MoveLevelHandle.SLOT_RIGHT) {
                // Allow placing/taking items in the two equipment slots
                return;
            }
            e.setCancelled(true);
            if (rawSlot == MoveLevelHandle.SLOT_CONFIRM) {
                handleMoveLevel(player, e.getInventory());
            }
        }
    }

    private void handleMoveLevel(Player player, Inventory inv) {
        ItemStack left = inv.getItem(MoveLevelHandle.SLOT_LEFT);
        ItemStack right = inv.getItem(MoveLevelHandle.SLOT_RIGHT);
        if (!CuiLianAPI.canCuiLian(left) || !CuiLianAPI.canCuiLian(right)) {
            player.sendMessage("§c请在左右槽位中分别放入可淬炼的装备！");
            return;
        }
        if (left.getAmount() > 1 || right.getAmount() > 1) {
            player.sendMessage("§c移星槽位中每次只能放入单件装备！");
            return;
        }

        Level level1 = Level.byItemStack(left);
        int leftLevel = level1 != null ? level1.value : 0;
        if (leftLevel <= 0) {
            player.sendMessage("§c左侧装备没有淬炼等级，无法移星！");
            return;
        }

        Level level2 = Level.byItemStack(right);
        int rightLevel = level2 != null ? level2.value : 0;

        int targetLevel;
        if (MoveLevelHandle.engine instanceof Invocable invocable) {
            try {
                Object res = invocable.invokeFunction("handle", leftLevel, rightLevel);
                targetLevel = (int) Math.floor(Double.parseDouble(String.valueOf(res)));
            } catch (Exception ex) {
                NewCustomCuiLianPro.ins.getLogger().warning("执行 movelevelscript.js 失败，回退至默认公式: " + ex.getMessage());
                targetLevel = leftLevel + rightLevel - 1;
            }
        } else {
            // Safe fallback formula matching default movelevelscript.js
            targetLevel = leftLevel + rightLevel - 1;
        }

        int maxLevel = Level.getMaxLevel();
        if (targetLevel > maxLevel) {
            targetLevel = maxLevel;
        }
        Level targetLevelObj = targetLevel > 0 ? Level.levels.get(targetLevel) : null;

        inv.setItem(MoveLevelHandle.SLOT_RIGHT, CuiLianAPI.setItemLevel(right, targetLevelObj));
        inv.setItem(MoveLevelHandle.SLOT_LEFT, CuiLianAPI.setItemLevel(left, null));

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.1f);
        player.sendMessage("§a§l淬炼移星成功！§e右侧装备当前为："
                + (targetLevelObj != null ? targetLevelObj.getFirstLore() : "§7无淬炼"));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof MoveLevelGuiHolder)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        returnSlotItem(player, e.getInventory(), MoveLevelHandle.SLOT_LEFT);
        returnSlotItem(player, e.getInventory(), MoveLevelHandle.SLOT_RIGHT);
    }

    private void returnSlotItem(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (ItemUtil.isNotEmpty(item)) {
            inv.setItem(slot, null);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }
}
