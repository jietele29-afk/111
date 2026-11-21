package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.gui.GemRemoveGuiHolder;
import lvhaoxuan.custom.cuilian.gui.OpItemsGuiHolder;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Handles:
 * 1) OpItemsGuiHolder (/cuilian op) paginated OP item browser
 * 2) GemRemoveGuiHolder (/cuilian gem remove) selective extraction GUI
 * 3) Direct 1-click in-bag interactions (ZERO second confirmation):
 *    - Pick up Gem on cursor + Right-click equipment -> instant socketing
 *    - Pick up Binder on cursor + Right-click Lv.1-4 Gem -> instant 5-to-1 synthesis
 *    - Pick up RemoveItem on cursor + Right-click socketed equipment -> instant gem extraction
 */
public class GemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory topInv = e.getInventory();

        // 1) OP Item Browser GUI (/cuilian op)
        if (topInv.getHolder() instanceof OpItemsGuiHolder holder) {
            e.setCancelled(true);
            int rawSlot = e.getRawSlot();
            if (rawSlot < 0 || rawSlot >= 54) {
                return;
            }
            if (rawSlot < 45) {
                ItemStack template = holder.getOriginalItemAt(rawSlot);
                if (ItemUtil.isNotEmpty(template)) {
                    int amount = e.isShiftClick() ? 64 : (e.isRightClick() ? 16 : 1);
                    template.setAmount(Math.min(amount, template.getMaxStackSize()));
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(template);
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
                }
                return;
            }
            if (rawSlot == OpItemsGuiHolder.SLOT_TAB_STONES) {
                OpItemsGuiHolder.open(player, OpItemsGuiHolder.Category.STONES, 0);
            } else if (rawSlot == OpItemsGuiHolder.SLOT_TAB_PROTECTS) {
                OpItemsGuiHolder.open(player, OpItemsGuiHolder.Category.PROTECTS, 0);
            } else if (rawSlot == OpItemsGuiHolder.SLOT_TAB_GEMS) {
                OpItemsGuiHolder.open(player, OpItemsGuiHolder.Category.GEMS, 0);
            } else if (rawSlot == OpItemsGuiHolder.SLOT_TAB_TOOLS) {
                OpItemsGuiHolder.open(player, OpItemsGuiHolder.Category.TOOLS, 0);
            } else if (rawSlot == OpItemsGuiHolder.SLOT_PREV && holder.getPage() > 0) {
                OpItemsGuiHolder.open(player, holder.getCategory(), holder.getPage() - 1);
            } else if (rawSlot == OpItemsGuiHolder.SLOT_NEXT && holder.getPage() < holder.getMaxPage()) {
                OpItemsGuiHolder.open(player, holder.getCategory(), holder.getPage() + 1);
            }
            return;
        }

        // 2) Gem Remove GUI (/cuilian gem remove)
        if (topInv.getHolder() instanceof GemRemoveGuiHolder holder) {
            e.setCancelled(true);
            int rawSlot = e.getRawSlot();
            if (rawSlot >= 10 && rawSlot <= 16) {
                handleRemoveGuiClick(player, holder, rawSlot - 10);
            }
            return;
        }

        // 3) Direct 1-click in-bag Gem Socketing, 5-to-1 Binding, and Extraction
        if (!NewCustomCuiLianPro.gemEnable || e.isCancelled()) {
            return;
        }
        if (e.getRawSlot() < 0 || !e.isRightClick()) {
            return;
        }
        InventoryType topType = topInv.getType();
        if (topType != InventoryType.CRAFTING && topType != InventoryType.PLAYER) {
            return;
        }

        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();

        // A) Cursor holds a Gem + Right-click an equipment item -> 1-click direct inlay!
        if (ItemUtil.isNotEmpty(cursor) && ItemUtil.isNotEmpty(target)) {
            Gem cursorGem = Gem.byItemStack(cursor);
            if (cursorGem != null && CuiLianAPI.canCuiLian(target)) {
                e.setCancelled(true);
                if (target.getAmount() > 1) {
                    player.sendMessage("§c一次只能对单件装备镶嵌宝石！");
                    return;
                }
                GemAPI.InlayResult result = GemAPI.inlayGem(target, cursorGem, player);
                if (result.consumeGem) {
                    consumeOneFromCursor(player, cursor);
                }
                if (result.destroyEquip) {
                    e.setCurrentItem(null);
                } else if (result.success) {
                    e.setCurrentItem(result.resultEquip);
                }
                return;
            }

            // B) Cursor holds BinderItem (宝石粘合剂) + Right-click a Gem -> 5-to-1 merge!
            if (Gem.isBinderItem(cursor)) {
                Gem targetGem = Gem.byItemStack(target);
                if (targetGem != null) {
                    e.setCancelled(true);
                    if (GemAPI.bindGem(player, targetGem)) {
                        consumeOneFromCursor(player, cursor);
                    }
                    return;
                }
            }

            // C) Cursor holds RemoveItem (宝石拆卸符) + Right-click a socketed equipment -> 1-click extract!
            if (Gem.isRemoveItem(cursor) && CuiLianAPI.canCuiLian(target)) {
                e.setCancelled(true);
                List<String> ids = GemAPI.getSocketedGemIds(target);
                if (ids.isEmpty()) {
                    player.sendMessage(Message.GEM_REMOVE_NO_GEMS);
                    return;
                }
                consumeOneFromCursor(player, cursor);
                ItemStack copy = target.clone();
                Gem extracted = GemAPI.removeGemAt(copy, ids.size() - 1, player);
                e.setCurrentItem(copy);
                if (extracted != null) {
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(extracted.createItemStack(1));
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                return;
            }
        }

        // Friendly tip if player right-clicks a gem/binder with an empty cursor
        if (!ItemUtil.isNotEmpty(cursor) && ItemUtil.isNotEmpty(target)) {
            if (Gem.byItemStack(target) != null) {
                player.sendMessage(Message.GEM_USAGE_TIP);
            } else if (Gem.isBinderItem(target)) {
                player.sendMessage(Message.GEM_BIND_USAGE_TIP);
            }
        }
    }

    private void handleRemoveGuiClick(Player player, GemRemoveGuiHolder holder, int gemIndex) {
        ItemStack equip = player.getInventory().getItem(holder.getHeldSlot());
        if (!ItemUtil.isNotEmpty(equip)) {
            player.closeInventory();
            return;
        }
        List<String> ids = GemAPI.getSocketedGemIds(equip);
        if (gemIndex < 0 || gemIndex >= ids.size()) {
            return;
        }
        if (Gem.removeItem == null || !ItemUtil.inventoryHasSimilar(player.getInventory(), Gem.removeItem, 1)) {
            player.sendMessage(Message.GEM_REMOVE_NO_TOOL);
            return;
        }
        ItemUtil.inventoryRemoveSimilar(player.getInventory(), Gem.removeItem, 1);

        ItemStack copy = equip.clone();
        Gem extracted = GemAPI.removeGemAt(copy, gemIndex, player);
        player.getInventory().setItem(holder.getHeldSlot(), copy);

        if (extracted != null) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(extracted.createItemStack(1));
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        if (GemAPI.getSocketedGemIds(copy).isEmpty()) {
            player.closeInventory();
        } else {
            holder.populate(copy);
        }
    }

    private void consumeOneFromCursor(Player player, ItemStack cursor) {
        if (cursor.getAmount() <= 1) {
            player.setItemOnCursor(null);
        } else {
            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor);
        }
    }
}
