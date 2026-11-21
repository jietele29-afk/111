package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.Stone;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class FurnaceListener implements Listener {

    private static final String META_OWNER = "FurnaceOwner";
    private static final String META_FUEL = "FurnaceFuel";

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !e.hasBlock()) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block != null && block.getType() == Material.FURNACE) {
            Player p = e.getPlayer();
            block.setMetadata(META_OWNER, new FixedMetadataValue(NewCustomCuiLianPro.ins, p.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent e) {
        Block block = e.getBlock();
        if (!(block.getState() instanceof Furnace furnace)) {
            return;
        }
        ItemStack fuel = e.getFuel();
        ItemStack smelt = furnace.getInventory().getSmelting();
        Stone stone = Stone.byItemStack(fuel);

        if (CuiLianAPI.canCuiLian(smelt)) {
            Level level = Level.byItemStack(smelt);
            int currentLv = level != null ? level.value : 0;
            if (stone != null && Level.levels.containsKey(currentLv + stone.riseLevel)) {
                block.setMetadata(META_FUEL, new FixedMetadataValue(NewCustomCuiLianPro.ins, stone));
                e.setBurning(true);
                e.setBurnTime(200);
            } else {
                // Prevent burning non-stone fuels (e.g. normal coal) or max-level items
                block.removeMetadata(META_FUEL, NewCustomCuiLianPro.ins);
                e.setCancelled(true);
            }
        } else if (stone != null) {
            // Prevent wasting a CuiLian Stone on non-temperable items (like raw iron or food)
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        ItemStack source = e.getSource();
        Block block = e.getBlock();

        if (block.hasMetadata(META_FUEL)) {
            List<MetadataValue> fuelMeta = block.getMetadata(META_FUEL);
            block.removeMetadata(META_FUEL, NewCustomCuiLianPro.ins);

            if (!fuelMeta.isEmpty() && fuelMeta.get(0).value() instanceof Stone stone
                    && CuiLianAPI.canCuiLian(source)) {
                Player owner = null;
                if (block.hasMetadata(META_OWNER)) {
                    List<MetadataValue> ownerMeta = block.getMetadata(META_OWNER);
                    if (!ownerMeta.isEmpty()) {
                        owner = Bukkit.getPlayerExact(ownerMeta.get(0).asString());
                    }
                }
                ItemStack smeltCopy = source.clone();
                smeltCopy.setAmount(1);
                ItemStack result = CuiLianAPI.cuilian(stone, smeltCopy, owner);
                e.setResult(result);
                return;
            }
        }

        // Fallback: never smelt a temperable item into iron/gold nugget if no CuiLian stone was used
        if (CuiLianAPI.canCuiLian(source)) {
            ItemStack unchanged = source.clone();
            unchanged.setAmount(1);
            e.setResult(unchanged);
        }
    }

    /**
     * Allows non-vanilla-fuel CuiLian Stones (e.g. EMERALD, AMETHYST_SHARD, PAPER)
     * to be placed into the furnace fuel slot via normal click or shift-click.
     * Vanilla fuel materials (like COAL) are handled natively by Minecraft without interference.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getType() != InventoryType.FURNACE) {
            return;
        }
        if (!(e.getInventory() instanceof FurnaceInventory furnaceInv)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Case 1: Clicking directly on the FUEL slot with a non-fuel CuiLian Stone on cursor
        if (e.getSlotType() == InventoryType.SlotType.FUEL) {
            ItemStack cursor = e.getCursor();
            if (ItemUtil.isNotEmpty(cursor) && !cursor.getType().isFuel() && Stone.byItemStack(cursor) != null) {
                ItemStack current = e.getCurrentItem();
                e.setCancelled(true);
                if (!ItemUtil.isNotEmpty(current)) {
                    if (e.isRightClick() && cursor.getAmount() > 1) {
                        ItemStack single = cursor.clone();
                        single.setAmount(1);
                        furnaceInv.setFuel(single);
                        cursor.setAmount(cursor.getAmount() - 1);
                        player.setItemOnCursor(cursor);
                    } else {
                        furnaceInv.setFuel(cursor.clone());
                        player.setItemOnCursor(null);
                    }
                } else if (current.isSimilar(cursor)) {
                    int maxStack = current.getMaxStackSize();
                    int canAdd = maxStack - current.getAmount();
                    if (canAdd > 0) {
                        int move = e.isRightClick() ? 1 : Math.min(canAdd, cursor.getAmount());
                        current.setAmount(current.getAmount() + move);
                        furnaceInv.setFuel(current);
                        if (cursor.getAmount() - move <= 0) {
                            player.setItemOnCursor(null);
                        } else {
                            cursor.setAmount(cursor.getAmount() - move);
                            player.setItemOnCursor(cursor);
                        }
                    }
                } else {
                    // Swap items
                    ItemStack oldFuel = current.clone();
                    furnaceInv.setFuel(cursor.clone());
                    player.setItemOnCursor(oldFuel);
                }
            }
            return;
        }

        // Case 2: Shift-clicking a non-fuel CuiLian Stone from player inventory into furnace
        if (e.isShiftClick() && e.getClickedInventory() != null
                && e.getClickedInventory().getType() == InventoryType.PLAYER) {
            ItemStack clicked = e.getCurrentItem();
            if (ItemUtil.isNotEmpty(clicked) && !clicked.getType().isFuel() && Stone.byItemStack(clicked) != null) {
                ItemStack fuel = furnaceInv.getFuel();
                if (!ItemUtil.isNotEmpty(fuel)) {
                    e.setCancelled(true);
                    furnaceInv.setFuel(clicked.clone());
                    e.setCurrentItem(null);
                } else if (fuel.isSimilar(clicked)) {
                    int canAdd = fuel.getMaxStackSize() - fuel.getAmount();
                    if (canAdd > 0) {
                        e.setCancelled(true);
                        int move = Math.min(canAdd, clicked.getAmount());
                        fuel.setAmount(fuel.getAmount() + move);
                        furnaceInv.setFuel(fuel);
                        if (clicked.getAmount() - move <= 0) {
                            e.setCurrentItem(null);
                        } else {
                            clicked.setAmount(clicked.getAmount() - move);
                            e.setCurrentItem(clicked);
                        }
                    }
                }
            }
        }
    }
}
