package lvhaoxuan.custom.cuilian.commander;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.gui.GemInlayGuiHolder;
import lvhaoxuan.custom.cuilian.gui.GemRemoveGuiHolder;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.movelevel.MoveLevelHandle;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.Stone;
import lvhaoxuan.custom.cuilian.runnable.ScriptRunnable;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class Commander implements CommandExecutor {

    private static String getHead() {
        return "§6[" + NewCustomCuiLianPro.ins.getName() + " v"
                + NewCustomCuiLianPro.ins.getDescription().getVersion() + "] ";
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("cuilian.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Help command (accessible to all players)
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Player commands
        if (sub.equals("movelevel")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getHead() + "§c仅玩家可使用此指令。");
                return true;
            }
            MoveLevelHandle.open(player);
            return true;
        }

        if (sub.equals("gem") || sub.equals("inlay")) {
            return handleGemCommand(sender, args);
        }

        // Admin-only commands below
        if (!isAdmin(sender)) {
            sender.sendMessage(getHead() + "§c你没有权限执行此指令。");
            return true;
        }

        switch (sub) {
            case "reload" -> {
                ScriptRunnable.enableScript = false;
                NewCustomCuiLianPro.enableConfig();
                NewCustomCuiLianPro.setRecipe();
                ScriptRunnable.resetCaches();
                sender.sendMessage(getHead() + "§a插件配置与宝石系统重载成功！");
            }
            case "stones" -> listStones(sender);
            case "protects" -> listProtects(sender);
            case "set" -> handleSetLevel(sender, args);
            case "stone" -> handleGiveStone(sender, args);
            case "protect" -> handleGiveProtect(sender, args);
            case "test" -> handleTestZombie(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l淬炼与宝石§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
        sender.sendMessage("§a§l▏   §e/cuilian moveLevel §7- 打开淬炼移星界面");
        sender.sendMessage("§a§l▏   §e/cuilian gem §7- 打开装备宝石镶嵌 / 打孔界面");
        sender.sendMessage("§a§l▏   §e/cuilian gem remove §7- 打开手持装备宝石拆卸界面");

        if (isAdmin(sender)) {
            sender.sendMessage("§c§l▏   §c/cuilian set <等级> §7- 设置手持装备淬炼等级 (0为清除)");
            sendClickableHelp(sender, "§c§l▏   §c/cuilian stone <淬炼石ID> <数量> <玩家>", "/cuilian stones", "§c点击查看所有淬炼石");
            sendClickableHelp(sender, "§c§l▏   §c/cuilian protect <保护符等级> <数量> <玩家>", "/cuilian protects", "§c点击查看所有保护符");
            sendClickableHelp(sender, "§c§l▏   §c/cuilian gem give <宝石ID> <数量> <玩家>", "/cuilian gem list", "§b点击查看所有宝石");
            sender.sendMessage("§c§l▏   §c/cuilian gem punch <数量> <玩家> §7- 给予装备打孔符");
            sender.sendMessage("§c§l▏   §c/cuilian gem extractor <数量> <玩家> §7- 给予宝石拆卸符");
            sender.sendMessage("§c§l▏   §c/cuilian gem addslot [数量] §7- 直接为手持装备增加宝石槽位");
            sender.sendMessage("§c§l▏   §c/cuilian reload §7- 重载全部配置");
        }
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l淬炼与宝石§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
    }

    private void sendClickableHelp(CommandSender sender, String text, String cmd, String hover) {
        if (sender instanceof Player player) {
            TextComponent tc = new TextComponent(text);
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
            player.spigot().sendMessage(tc);
        } else {
            sender.sendMessage(text);
        }
    }

    private boolean handleGemCommand(CommandSender sender, String[] args) {
        if (!NewCustomCuiLianPro.gemEnable) {
            sender.sendMessage(Message.GEM_DISABLED);
            return true;
        }
        // /cuilian gem
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getHead() + "§c仅玩家可打开宝石镶嵌界面。");
                return true;
            }
            GemInlayGuiHolder.open(player);
            return true;
        }

        String gemSub = args[1].toLowerCase();
        // /cuilian gem remove
        if (gemSub.equals("remove") || gemSub.equals("extract")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getHead() + "§c仅玩家可打开宝石拆卸界面。");
                return true;
            }
            GemRemoveGuiHolder.open(player);
            return true;
        }

        // Admin gem subcommands
        if (!isAdmin(sender)) {
            sender.sendMessage(getHead() + "§c你没有权限执行此宝石管理指令。");
            return true;
        }

        switch (gemSub) {
            case "list" -> {
                sender.sendMessage("§b§m§l  §3§m§l  §9§m§l  §e§l宝石列表§9§m§l  §3§m§l  §b§m§l  ");
                for (Map.Entry<String, Gem> entry : Gem.gems.entrySet()) {
                    String line = "§b§l▏   §f" + entry.getKey() + " §7- " + entry.getValue().name
                            + " §7(成功率 " + entry.getValue().chance + "%)";
                    if (sender instanceof Player player) {
                        TextComponent tc = new TextComponent(line);
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/cuilian gem give " + entry.getKey() + " 1 " + player.getName()));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder("§a点击获取 1 颗 " + entry.getValue().name).create()));
                        player.spigot().sendMessage(tc);
                    } else {
                        sender.sendMessage(line);
                    }
                }
                sender.sendMessage("§b§m§l  §3§m§l  §9§m§l  §e§l宝石列表§9§m§l  §3§m§l  §b§m§l  ");
            }
            case "give" -> {
                if (args.length < 5) {
                    sender.sendMessage(getHead() + "§c用法: /cuilian gem give <宝石ID> <数量> <玩家>");
                    return true;
                }
                Gem gem = Gem.gems.get(args[2]);
                if (gem == null) {
                    sender.sendMessage(getHead() + "§c未找到宝石ID: " + args[2]);
                    return true;
                }
                int amount = parsePositiveInt(args[3]);
                if (amount <= 0) {
                    sender.sendMessage(getHead() + "§c数量必须为正整数。");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[4]);
                if (target == null) {
                    sender.sendMessage(getHead() + "§c玩家不在线: " + args[4]);
                    return true;
                }
                giveItemSafely(target, gem.createItemStack(amount));
                sender.sendMessage(getHead() + "§a已给予 §e" + target.getName() + " §a" + amount + " 颗 " + gem.name);
            }
            case "punch" -> {
                if (args.length < 4) {
                    sender.sendMessage(getHead() + "§c用法: /cuilian gem punch <数量> <玩家>");
                    return true;
                }
                int amount = parsePositiveInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(getHead() + "§c数量必须为正整数。");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(getHead() + "§c玩家不在线: " + args[3]);
                    return true;
                }
                ItemStack punch = Gem.createPunchItem(amount);
                if (punch == null) {
                    sender.sendMessage(getHead() + "§c打孔符未在 gem.yml 中配置！");
                    return true;
                }
                giveItemSafely(target, punch);
                sender.sendMessage(getHead() + "§a已给予 §e" + target.getName() + " §a" + amount + " 个装备打孔符。");
            }
            case "extractor", "removetool" -> {
                if (args.length < 4) {
                    sender.sendMessage(getHead() + "§c用法: /cuilian gem extractor <数量> <玩家>");
                    return true;
                }
                int amount = parsePositiveInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(getHead() + "§c数量必须为正整数。");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(getHead() + "§c玩家不在线: " + args[3]);
                    return true;
                }
                ItemStack tool = Gem.createRemoveItem(amount);
                if (tool == null) {
                    sender.sendMessage(getHead() + "§c宝石拆卸符未在 gem.yml 中配置！");
                    return true;
                }
                giveItemSafely(target, tool);
                sender.sendMessage(getHead() + "§a已给予 §e" + target.getName() + " §a" + amount + " 个宝石拆卸符。");
            }
            case "addslot" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getHead() + "§c仅玩家可使用此指令。");
                    return true;
                }
                int add = args.length >= 3 ? parsePositiveInt(args[2]) : 1;
                if (add <= 0) {
                    sender.sendMessage(getHead() + "§c数量必须为正整数。");
                    return true;
                }
                ItemStack hand = ItemUtil.getItemInMainHand(player);
                if (!CuiLianAPI.canCuiLian(hand)) {
                    sender.sendMessage(getHead() + "§c手持物品不可淬炼/打孔。");
                    return true;
                }
                int current = GemAPI.getSlotCount(hand);
                int targetSlots = Math.min(NewCustomCuiLianPro.gemMaxSlots, current + add);
                GemAPI.setSlotCount(hand, targetSlots);
                ItemUtil.setItemInMainHand(player, hand);
                sender.sendMessage(getHead() + "§a已将手持装备宝石槽位设置为 §b" + targetSlots + "/" + NewCustomCuiLianPro.gemMaxSlots);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void listStones(CommandSender sender) {
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l淬炼石§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
        for (Map.Entry<String, Stone> entry : Stone.stones.entrySet()) {
            String display = entry.getValue().item != null && entry.getValue().item.hasItemMeta()
                    && entry.getValue().item.getItemMeta().hasDisplayName()
                    ? entry.getValue().item.getItemMeta().getDisplayName() : entry.getKey();
            String text = "§c§l▏   §f" + entry.getKey() + " §7- " + display;
            if (sender instanceof Player player) {
                TextComponent tc = new TextComponent(text);
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/cuilian stone " + entry.getKey() + " 1 " + player.getName()));
                tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§c点击获取淬炼石").create()));
                player.spigot().sendMessage(tc);
            } else {
                sender.sendMessage(text);
            }
        }
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l淬炼石§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
    }

    private void listProtects(CommandSender sender) {
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l保护符§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
        for (Map.Entry<Integer, Level> entry : Level.levels.entrySet()) {
            if (entry.getValue().protectRune != null) {
                String display = entry.getValue().protectRune.item != null
                        && entry.getValue().protectRune.item.hasItemMeta()
                        && entry.getValue().protectRune.item.getItemMeta().hasDisplayName()
                        ? entry.getValue().protectRune.item.getItemMeta().getDisplayName()
                        : entry.getValue().protectRune.lore;
                String text = "§c§l▏   §f" + entry.getKey() + " §7- " + display;
                if (sender instanceof Player player) {
                    TextComponent tc = new TextComponent(text);
                    tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/cuilian protect " + entry.getKey() + " 1 " + player.getName()));
                    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§c点击获取保护符").create()));
                    player.spigot().sendMessage(tc);
                } else {
                    sender.sendMessage(text);
                }
            }
        }
        sender.sendMessage("§c§m§l  §6§m§l  §e§m§l  §a§m§l  §b§m§l  §e§l保护符§b§m§l  §a§m§l  §e§m§l  §6§m§l  §c§m§l  ");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getHead() + "§c仅玩家可使用此指令。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(getHead() + "§c用法: /cuilian set <等级>");
            return;
        }
        int lv;
        try {
            lv = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getHead() + "§c等级必须为整数！");
            return;
        }
        ItemStack hand = ItemUtil.getItemInMainHand(player);
        if (!CuiLianAPI.canCuiLian(hand)) {
            sender.sendMessage(getHead() + "§c手持物品不可淬炼！");
            return;
        }
        Level level = lv <= 0 ? null : Level.levels.get(lv);
        if (lv > 0 && level == null) {
            sender.sendMessage(getHead() + "§c未找到淬炼等级: " + lv);
            return;
        }
        ItemUtil.setItemInMainHand(player, CuiLianAPI.setItemLevel(hand, level));
        sender.sendMessage(getHead() + "§a已将手持装备淬炼等级设置为: " + (level != null ? level.getFirstLore() : "§70星(已清除)"));
    }

    private void handleGiveStone(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(getHead() + "§c用法: /cuilian stone <淬炼石ID> <数量> <玩家>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(getHead() + "§c玩家不在线: " + args[3]);
            return;
        }
        Stone stone = Stone.stones.get(args[1]);
        if (stone == null || stone.item == null) {
            sender.sendMessage(getHead() + "§c未找到淬炼石: " + args[1]);
            return;
        }
        int amount = parsePositiveInt(args[2]);
        if (amount <= 0) {
            sender.sendMessage(getHead() + "§c数量必须为正整数！");
            return;
        }
        // Always clone so we never mutate Stone.item singleton!
        ItemStack give = stone.item.clone();
        give.setAmount(amount);
        giveItemSafely(target, give);
        sender.sendMessage(getHead() + "§a已成功给予 §e" + target.getName() + " §a" + amount + " 个淬炼石 (" + args[1] + ")。");
    }

    private void handleGiveProtect(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(getHead() + "§c用法: /cuilian protect <保护符等级> <数量> <玩家>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(getHead() + "§c玩家不在线: " + args[3]);
            return;
        }
        int lv;
        try {
            lv = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getHead() + "§c保护符等级必须为整数！");
            return;
        }
        Level level = Level.levels.get(lv);
        if (level == null || level.protectRune == null || level.protectRune.item == null) {
            sender.sendMessage(getHead() + "§c未找到该等级的保护符: " + lv);
            return;
        }
        int amount = parsePositiveInt(args[2]);
        if (amount <= 0) {
            sender.sendMessage(getHead() + "§c数量必须为正整数！");
            return;
        }
        // Always clone so we never mutate ProtectRune.item singleton!
        ItemStack give = level.protectRune.item.clone();
        give.setAmount(amount);
        giveItemSafely(target, give);
        sender.sendMessage(getHead() + "§a已成功给予 §e" + target.getName() + " §a" + amount + " 个 " + lv + " 星保护符。");
    }

    private void handleTestZombie(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(getHead() + "§c仅玩家可使用测试指令。");
            return;
        }
        Level maxLv = Level.levels.get(Level.getMaxLevel());
        if (maxLv == null) {
            sender.sendMessage(getHead() + "§c未配置任何淬炼等级。");
            return;
        }
        Zombie zombie = (Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE);
        EntityEquipment eq = zombie.getEquipment();
        if (eq != null) {
            eq.setItemInMainHand(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_SWORD), maxLv));
            eq.setItemInOffHand(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_SWORD), maxLv));
            eq.setHelmet(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_HELMET), maxLv));
            eq.setChestplate(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_CHESTPLATE), maxLv));
            eq.setLeggings(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_LEGGINGS), maxLv));
            eq.setBoots(CuiLianAPI.setItemLevel(new ItemStack(Material.DIAMOND_BOOTS), maxLv));
        }
        sender.sendMessage(getHead() + "§a已生成满星测试僵尸。");
    }

    private int parsePositiveInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void giveItemSafely(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}
