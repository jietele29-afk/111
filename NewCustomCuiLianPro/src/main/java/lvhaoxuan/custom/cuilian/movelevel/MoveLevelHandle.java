package lvhaoxuan.custom.cuilian.movelevel;

import lvhaoxuan.custom.cuilian.gui.MoveLevelGuiHolder;
import lvhaoxuan.custom.cuilian.loader.Loader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.script.ScriptEngine;
import java.util.List;

public class MoveLevelHandle {

    public static final int SLOT_LEFT = 2;
    public static final int SLOT_RIGHT = 4;
    public static final int SLOT_CONFIRM = 6;

    public static ScriptEngine engine;
    public static String moveLevelInvTitle = "§8淬炼移星";
    public static boolean enableMoveLevel = true;

    public static void init() {
        engine = Loader.loadMoveLevelScript();
        enableMoveLevel = true;
    }

    public static void open(Player p) {
        if (!enableMoveLevel || p == null) {
            return;
        }
        MoveLevelGuiHolder holder = new MoveLevelGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 9,
                moveLevelInvTitle != null ? moveLevelInvTitle : "§8淬炼移星");
        holder.setInventory(inv);

        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, background);
        }

        inv.setItem(SLOT_LEFT, null);
        inv.setItem(SLOT_RIGHT, null);

        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta btnMeta = button.getItemMeta();
        if (btnMeta != null) {
            btnMeta.setDisplayName("§a淬炼移星");
            btnMeta.setLore(List.of(
                    "§7把左边装备的淬炼等级移给右边装备。",
                    "§7(宝石槽位与已镶嵌宝石将各自保留)"
            ));
            button.setItemMeta(btnMeta);
        }
        inv.setItem(SLOT_CONFIRM, button);

        p.openInventory(inv);
    }
}
