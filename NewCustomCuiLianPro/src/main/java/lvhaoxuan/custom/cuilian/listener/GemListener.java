package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.gui.GemInlayGuiHolder;
import lvhaoxuan.custom.cuilian.gui.GemRemoveGuiHolder;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles:
 * 1) GemInlayGuiHolder (27-slot Gem Socketing & Socket Punching GUI)
 * 2) GemRemoveGuiHolder (27-slot Gem Extraction GUI)
 * 3) Quick in-bag right-click socketing & punching (matching ProtectRune UX)
 */
public class GemListener implements Listener {

    private static final Map<UUID, Gem> pendingGems = new HashMap<>();
    private static final Map<UUID, Boolean> pendingPunch = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory topInv = e.getInventory();

        // 1) Gem Inlay / Punch GUI
        if (topInv.getHolder() instanceof GemInlayGuiHolder holder) {
            int rawSlot = e.getRawSlot();
            if (rawSlot < 0) {
                return;
            }
            if (rawSlot < topInv.getSize()) {
                if (rawSlot == GemInlayGuiHolder.SLOT_EQUIP || rawSlot == GemInlayGuiHolder.SLOT_MATERIAL) {
                    // Refresh preview button on next tick after item moves
                    Bukkit.getScheduler().runTask(NewCustomCuiLianPro.ins, holder::refreshConfirmButton);
                    return;
                }
                e.setCancelled(true);
                if (rawSlot == GemInlayGuiHolder.SLOT_CONFIRM) {
                    handleInlayGuiConfirm(player, topInv, holder);
                }
            } else {
                // Player inventory clicked (e.g. Shift-click) -> refresh preview next tick
                Bukkit.getScheduler().runTask(NewCustomCuiLianPro.ins, holder::refreshConfirmButton);
            }
            return;
        }

        // 2) Gem Remove GUI
        if (topInv.getHolder() instanceof GemRemoveGuiHolder holder) {
            e.setCancelled(true);
            int rawSlot = e.getRawSlot();
            if (rawSlot >= 10 && rawSlot <= 16) {
                handleRemoveGuiClick(player, holder, rawSlot - 10);
            }
            return;
        }

        // 3) Quick in-bag right-click socketing / punching
        if (!NewCustomCuiLianPro.gemEnable || e.isCancelled()) {
            return;
        }
        if (e.getRawSlot() < 0 || !e.isRightClick()) {
            return;
        }
        InventoryType topType = topInv.getType();
        boolean inPersonalBag = (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER);
        UUID uuid = player.getUniqueId();
        ItemStack clicked = e.getCurrentItem();

        // Active pending Gem or Punch selection
        if (pendingGems.containsKey(uuid) || pendingPunch.containsKey(uuid)) {
            if (!inPersonalBag) {
                player.sendMessage(Message.MUST_ADD_IN_BAG);
                return;
            }
            e.setCancelled(true);
            if (!ItemUtil.isNotEmpty(clicked)) {
                pendingGems.remove(uuid);
                pendingPunch.remove(uuid);
                player.sendMessage(Message.GEM_SELECT_CANCEL);
                player.closeInventory();
                return;
            }
            if (clicked.getAmount() > 1) {
                player.sendMessage("§c一次只能对单件装备进行操作！");
                return;
            }

            if (pendingPunch.remove(uuid) != null) {
                if (Gem.punchItem == null || !ItemUtil.inventoryHasSimilar(player.getInventory(), Gem.punchItem, 1)) {
                    player.sendMessage(Message.GEM_SELECT_CANCEL);
                    player.closeInventory();
                    return;
                }
                if (!CuiLianAPI.canCuiLian(clicked)) {
                    player.sendMessage(Message.GEM_CAN_NOT_INLAY);
                    player.closeInventory();
                    return;
                }
                int current = GemAPI.getSlotCount(clicked);
                if (current >= NewCustomCuiLianPro.gemMaxSlots) {
                    player.sendMessage(Message.GEM_PUNCH_MAX.replace("%d", String.valueOf(NewCustomCuiLianPro.gemMaxSlots)));
                    player.closeInventory();
                    return;
                }
                ItemStack targetCopy = clicked.clone();
                ItemUtil.inventoryRemoveSimilar(player.getInventory(), Gem.punchItem, 1);
                if (GemAPI.punchSlot(targetCopy, player, false)) {
                    e.setCurrentItem(targetCopy);
                }
                player.closeInventory();
                return;
            }

            Gem gem = pendingGems.remove(uuid);
            if (gem != null) {
                if (!ItemUtil.inventoryHasSimilar(player.getInventory(), gem.item, 1)) {
                    player.sendMessage(Message.GEM_SELECT_CANCEL);
                    player.closeInventory();
                    return;
                }
                GemAPI.InlayResult result = GemAPI.inlayGem(clicked, gem, player);
                if (result.consumeGem) {
                    ItemUtil.inventoryRemoveSimilar(player.getInventory(), gem.item, 1);
                }
                if (result.destroyEquip) {
                    e.setCurrentItem(null);
                } else if (result.success) {
                    e.setCurrentItem(result.resultEquip);
                }
                player.closeInventory();
            }
            return;
        }

        // Starting a quick in-bag selection by right-clicking a Gem or PunchItem
        if (inPersonalBag && ItemUtil.isNotEmpty(clicked)) {
            if (Gem.isPunchItem(clicked)) {
                e.setCancelled(true);
                pendingPunch.put(uuid, Boolean.TRUE);
                String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                        ? clicked.getItemMeta().getDisplayName() : "装备打孔符";
                for (String line : Message.GEM_SELECT_START) {
                    player.sendMessage(line.replace("%s", name));
                }
                player.closeInventory();
                return;
            }
            Gem gem = Gem.byItemStack(clicked);
            if (gem != null) {
                e.setCancelled(true);
                pendingGems.put(uuid, gem);
                for (String line : Message.GEM_SELECT_START) {
                    player.sendMessage(line.replace("%s", gem.name));
                }
                player.closeInventory();
            }
        }
    }

    private void handleInlayGuiConfirm(Player player, Inventory inv, GemInlayGuiHolder holder) {
        ItemStack equip = inv.getItem(GemInlayGuiHolder.SLOT_EQUIP);
        ItemStack mat = inv.getItem(GemInlayGuiHolder.SLOT_MATERIAL);
        if (!ItemUtil.isNotEmpty(equip) || !ItemUtil.isNotEmpty(mat)) {
            return;
        }
        if (equip.getAmount() > 1) {
            player.sendMessage("§c装备槽每次只能放入单件装备！");
            return;
        }

        // Punching mode
        if (Gem.isPunchItem(mat)) {
            if (!CuiLianAPI.canCuiLian(equip)) {
                player.sendMessage(Message.GEM_CAN_NOT_INLAY);
                return;
            }
            int current = GemAPI.getSlotCount(equip);
            if (current >= NewCustomCuiLianPro.gemMaxSlots) {
                player.sendMessage(Message.GEM_PUNCH_MAX.replace("%d", String.valueOf(NewCustomCuiLianPro.gemMaxSlots)));
                return;
            }
            consumeOneFromSlot(inv, GemInlayGuiHolder.SLOT_MATERIAL);
            ItemStack copy = equip.clone();
            if (GemAPI.punchSlot(copy, player, false)) {
                inv.setItem(GemInlayGuiHolder.SLOT_EQUIP, copy);
            }
            holder.refreshConfirmButton();
            return;
        }

        // Gem inlay mode
        Gem gem = Gem.byItemStack(mat);
        if (gem == null) {
            player.sendMessage(Message.GEM_CAN_NOT_INLAY);
            return;
        }
        GemAPI.InlayResult result = GemAPI.inlayGem(equip, gem, player);
        if (result.consumeGem) {
            consumeOneFromSlot(inv, GemInlayGuiHolder.SLOT_MATERIAL);
        }
        if (result.destroyEquip) {
            inv.setItem(GemInlayGuiHolder.SLOT_EQUIP, null);
        } else if (result.success) {
            inv.setItem(GemInlayGuiHolder.SLOT_EQUIP, result.resultEquip);
        }
        holder.refreshConfirmButton();
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

    private void consumeOneFromSlot(Inventory inv, int slot) {
        ItemStack stack = inv.getItem(slot);
        if (!ItemUtil.isNotEmpty(stack)) {
            return;
        }
        if (stack.getAmount() <= 1) {
            inv.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            inv.setItem(slot, stack);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof GemInlayGuiHolder)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        returnSlotItem(player, e.getInventory(), GemInlayGuiHolder.SLOT_EQUIP);
        returnSlotItem(player, e.getInventory(), GemInlayGuiHolder.SLOT_MATERIAL);
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        pendingGems.remove(uuid);
        pendingPunch.remove(uuid);
    }
}
