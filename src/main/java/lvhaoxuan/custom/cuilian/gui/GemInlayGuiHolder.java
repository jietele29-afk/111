package lvhaoxuan.custom.cuilian.gui;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
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
 * 27-slot Gem Socketing & Punching GUI:
 * - Slot 11: Equipment slot (装备槽)
 * - Slot 13: Gem or Punch Item slot (宝石/打孔符槽)
 * - Slot 15: Confirm / Status button (确认镶嵌/打孔按钮)
 */
public class GemInlayGuiHolder implements InventoryHolder {

    public static final int SLOT_EQUIP = 11;
    public static final int SLOT_MATERIAL = 13;
    public static final int SLOT_CONFIRM = 15;

    private Inventory inventory;

    public static void open(Player player) {
        GemInlayGuiHolder holder = new GemInlayGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, NewCustomCuiLianPro.gemInlayInvTitle);
        holder.inventory = inv;

        ItemStack border = makePane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        inv.setItem(2, makeIcon(Material.IRON_CHESTPLATE, "§e§l装备放入槽", List.of("§7在下方放入需要镶嵌或打孔的装备")));
        inv.setItem(4, makeIcon(Material.AMETHYST_SHARD, "§b§l宝石 / 打孔符槽", List.of("§7在下方放入宝石或装备打孔符")));
        inv.setItem(6, makeIcon(Material.ANVIL, "§a§l操作确认", List.of("§7放入装备与宝石/打孔符后点击下方按钮")));

        inv.setItem(SLOT_EQUIP, null);
        inv.setItem(SLOT_MATERIAL, null);
        holder.refreshConfirmButton();

        player.openInventory(inv);
    }

    public void refreshConfirmButton() {
        if (inventory == null) {
            return;
        }
        ItemStack equip = inventory.getItem(SLOT_EQUIP);
        ItemStack mat = inventory.getItem(SLOT_MATERIAL);

        if (!ItemUtil.isNotEmpty(equip) || !ItemUtil.isNotEmpty(mat)) {
            inventory.setItem(SLOT_CONFIRM, makeIcon(Material.YELLOW_STAINED_GLASS_PANE,
                    "§e等待放入物品",
                    List.of("§7左侧放入可淬炼装备", "§7中间放入宝石或打孔符")));
            return;
        }

        if (!CuiLianAPI.canCuiLian(equip)) {
            inventory.setItem(SLOT_CONFIRM, makeIcon(Material.RED_STAINED_GLASS_PANE,
                    "§c装备不可镶嵌",
                    List.of("§7该物品不在 items.yml 可淬炼列表中")));
            return;
        }

        int totalSlots = GemAPI.getSlotCount(equip);
        int usedSlots = GemAPI.getSocketedGemIds(equip).size();

        if (Gem.isPunchItem(mat)) {
            if (totalSlots >= NewCustomCuiLianPro.gemMaxSlots) {
                inventory.setItem(SLOT_CONFIRM, makeIcon(Material.RED_STAINED_GLASS_PANE,
                        "§c槽位已达上限",
                        List.of("§7当前槽位: §e" + totalSlots + "/" + NewCustomCuiLianPro.gemMaxSlots)));
            } else {
                inventory.setItem(SLOT_CONFIRM, makeIcon(Material.LIME_STAINED_GLASS_PANE,
                        "§a§l点击执行装备打孔",
                        List.of("§7当前槽位: §e" + totalSlots + "/" + NewCustomCuiLianPro.gemMaxSlots,
                                "§7打孔成功率: §b" + Gem.punchChance + "%",
                                "§e点击消耗 1 个打孔符增加 1 个槽位")));
            }
            return;
        }

        Gem gem = Gem.byItemStack(mat);
        if (gem == null) {
            inventory.setItem(SLOT_CONFIRM, makeIcon(Material.RED_STAINED_GLASS_PANE,
                    "§c无效的材料",
                    List.of("§7中间槽位请放入有效的宝石或打孔符")));
            return;
        }

        String error = GemAPI.validateInlay(equip, gem);
        if (error != null) {
            inventory.setItem(SLOT_CONFIRM, makeIcon(Material.RED_STAINED_GLASS_PANE,
                    "§c无法镶嵌",
                    List.of(error)));
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("§7目标宝石: " + gem.name);
            lore.add("§7装备槽位: §b" + usedSlots + "/" + totalSlots + " §7(空余 " + (totalSlots - usedSlots) + ")");
            lore.add("§7镶嵌成功率: §a" + gem.chance + "%");
            lore.add("§7失败损毁宝石: " + (gem.failDestroyGem ? "§c是" : "§a否"));
            if (gem.failDestroyEquip) {
                lore.add("§4警告: 失败时将损毁装备！");
            }
            lore.add("§e点击确认镶嵌！");
            inventory.setItem(SLOT_CONFIRM, makeIcon(Material.EMERALD_BLOCK, "§a§l点击确认镶嵌", lore));
        }
    }

    private static ItemStack makePane(Material mat, String name) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack makeIcon(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
