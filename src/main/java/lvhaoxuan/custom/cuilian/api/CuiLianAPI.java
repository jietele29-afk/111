package lvhaoxuan.custom.cuilian.api;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.ProtectRune;
import lvhaoxuan.custom.cuilian.object.Stone;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CuiLianAPI {

    public static boolean canCuiLian(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return false;
        }
        return NewCustomCuiLianPro.typesInBag.containsKey(item.getType());
    }

    /**
     * Tempers an equipment item using a CuiLian Stone. Chat feedback only (no Title popup).
     */
    public static ItemStack cuilian(Stone stone, ItemStack item, Player p) {
        if (stone == null || !canCuiLian(item)) {
            return item;
        }
        Level basicLevelObj = Level.byItemStack(item);
        int basicLevel = basicLevelObj != null ? basicLevelObj.value : 0;
        Level targetLevel = Level.levels.get(basicLevel + stone.riseLevel);
        if (targetLevel == null) {
            return item;
        }

        Double chanceVal = stone.chance != null ? stone.chance.get(targetLevel) : null;
        double chance = chanceVal != null ? chanceVal : 0.0;
        boolean success = ItemUtil.rollChance(chance);
        String sendMessage;

        if (success) {
            item = setItemLevel(item, targetLevel);
            String lvText = targetLevel.getFirstLore();
            sendMessage = Message.SUCCESS.replace("%s", lvText);
            if (targetLevel.value >= 5) {
                String playerName = p != null ? p.getDisplayName() : "未知玩家";
                String stoneName = stone.item != null && stone.item.hasItemMeta() && stone.item.getItemMeta().hasDisplayName()
                        ? stone.item.getItemMeta().getDisplayName() : stone.id;
                Bukkit.broadcastMessage(Message.SERVER_SUCCESS
                        .replace("%p", playerName)
                        .replace("%d", stoneName)
                        .replace("%s", lvText));
            }
        } else {
            int dropLevel = stone.dropLevel != null ? stone.dropLevel.toInteger() : 0;
            Level protectRune = Level.byProtectRune(item);

            if (protectRune != null && protectRune.value <= basicLevel) {
                int maxAllowedDrop = basicLevel - protectRune.value;
                if (maxAllowedDrop <= dropLevel) {
                    dropLevel = maxAllowedDrop > 0 ? ItemUtil.randomInt(0, maxAllowedDrop) : 0;
                }
                int finalLevelNum = Math.max(0, basicLevel - dropLevel);
                Level toLevel = Level.levels.get(finalLevelNum);
                item = setItemLevel(item, toLevel);
                String lvText = toLevel != null ? toLevel.getFirstLore() : "§c§l淬炼消失";
                sendMessage = Message.CUILIAN_FAIL_PROTECT_RUNE
                        .replace("%s", lvText)
                        .replace("%d", String.valueOf(dropLevel));
            } else {
                int finalLevelNum = Math.max(0, basicLevel - dropLevel);
                Level toLevel = Level.levels.get(finalLevelNum);
                item = setItemLevel(item, toLevel);
                String lvText = toLevel != null ? toLevel.getFirstLore() : "§c§l淬炼消失";
                sendMessage = Message.CUILIAN_FAIL
                        .replace("%s", lvText)
                        .replace("%d", String.valueOf(dropLevel));
            }
        }

        if (p != null && sendMessage != null) {
            p.sendMessage(sendMessage);
        }
        return item;
    }

    public static ItemStack setItemLevel(ItemStack item, Level level) {
        if (!canCuiLian(item)) {
            return item;
        }
        int basicLevel = level != null ? level.value : 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Level protectRuneLevel = Level.byProtectRune(item);

        setDisplayName(item.getType(), meta, basicLevel);

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore = replaceLore(lore);
        lore = cleanLevel(lore);
        lore = cleanProtectRune(lore);
        lore = GemAPI.cleanGemLore(lore);

        if (level != null) {
            if (Message.UNDER_LINE != null && !Message.UNDER_LINE.isEmpty()) {
                lore.add(Message.UNDER_LINE);
            }
            for (String line : level.lore) {
                lore.add(NewCustomCuiLianPro.LEVEL_JUDGE + line);
            }
            String slotType = NewCustomCuiLianPro.typesInBag.get(item.getType());
            if (slotType != null && level.attribute != null) {
                List<String> attrs = level.attribute.get(slotType);
                if (attrs != null) {
                    for (String line : attrs) {
                        lore.add(NewCustomCuiLianPro.LEVEL_JUDGE + line);
                    }
                }
            }
        }

        if (protectRuneLevel != null && protectRuneLevel.protectRune != null) {
            lore.add(NewCustomCuiLianPro.PROTECT_RUNE_JUDGE + protectRuneLevel.protectRune.lore);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        GemAPI.refreshGemLore(item);
        return item;
    }

    public static void setDisplayName(Material type, ItemMeta meta, int basicLevel) {
        if (NewCustomCuiLianPro.displayNameFormat == 1) {
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : chineseDisplayName(type);
            displayName = displayName.replaceAll("^(§[0-9a-fk-orA-FK-OR])*\\+[0-9]+ ", "");
            if (basicLevel > 0) {
                displayName = "§e+" + basicLevel + " " + displayName;
            }
            meta.setDisplayName(displayName);
        } else if (NewCustomCuiLianPro.displayNameFormat == 2) {
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : chineseDisplayName(type);
            displayName = displayName.replaceAll(" (§[0-9a-fk-orA-FK-OR])*\\+[0-9]+$", "");
            if (basicLevel > 0) {
                displayName = displayName + " §e+" + basicLevel;
            }
            meta.setDisplayName(displayName);
        }
    }

    public static ItemStack addProtectRune(ItemStack item, ProtectRune protectRune) {
        if (!ItemUtil.isNotEmpty(item) || protectRune == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore = cleanProtectRune(lore);
        lore = GemAPI.cleanGemLore(lore);
        lore.add(NewCustomCuiLianPro.PROTECT_RUNE_JUDGE + protectRune.lore);
        meta.setLore(lore);
        item.setItemMeta(meta);
        GemAPI.refreshGemLore(item);
        return item;
    }

    public static List<String> cleanLevel(List<String> lore) {
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line == null) {
                continue;
            }
            if ((NewCustomCuiLianPro.LEVEL_JUDGE != null && !NewCustomCuiLianPro.LEVEL_JUDGE.isEmpty()
                    && line.contains(NewCustomCuiLianPro.LEVEL_JUDGE))
                    || (Message.UNDER_LINE != null && !Message.UNDER_LINE.isEmpty()
                    && line.equals(Message.UNDER_LINE))) {
                iterator.remove();
            }
        }
        return lore;
    }

    public static List<String> cleanProtectRune(List<String> lore) {
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line != null && NewCustomCuiLianPro.PROTECT_RUNE_JUDGE != null
                    && !NewCustomCuiLianPro.PROTECT_RUNE_JUDGE.isEmpty()
                    && line.contains(NewCustomCuiLianPro.PROTECT_RUNE_JUDGE)) {
                iterator.remove();
            }
        }
        return lore;
    }

    public static List<String> replaceLore(List<String> lore) {
        if (NewCustomCuiLianPro.replaceLore == null || NewCustomCuiLianPro.replaceLore.isEmpty()) {
            return lore;
        }
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line == null) {
                continue;
            }
            for (String replace : NewCustomCuiLianPro.replaceLore) {
                if (replace != null && !replace.isEmpty() && line.contains(replace)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return lore;
    }

    public static Level getMinLevel(LivingEntity entity, EntityEquipment equipment) {
        if (entity == null || equipment == null) {
            return null;
        }
        int ret = -1;
        for (ItemStack item : equipment.getArmorContents()) {
            Level level = Level.byItemStack(item);
            int basicLevel = level != null ? level.value : 0;
            ret = (ret == -1) ? basicLevel : Math.min(ret, basicLevel);
        }
        ItemStack mainHand = ItemUtil.getItemInMainHand(entity);
        Level mainLevel = Level.byItemStack(mainHand);
        int mainVal = mainLevel != null ? mainLevel.value : 0;
        ret = (ret == -1) ? mainVal : Math.min(ret, mainVal);

        if (NewCustomCuiLianPro.judgeOffHand) {
            ItemStack offHand = ItemUtil.getItemInOffHand(entity);
            Level offLevel = Level.byItemStack(offHand);
            int offVal = offLevel != null ? offLevel.value : 0;
            ret = (ret == -1) ? offVal : Math.min(ret, offVal);
        }
        return Level.levels.get(ret);
    }

    public static String chineseDisplayName(Material type) {
        if (type == null) {
            return "§f未知物品";
        }
        return switch (type) {
            case BOW -> "§f弓";
            case CROSSBOW -> "§f弩";
            case TRIDENT -> "§f三叉戟";
            case WOODEN_SWORD -> "§f木剑";
            case STONE_SWORD -> "§f石剑";
            case IRON_SWORD -> "§f铁剑";
            case GOLDEN_SWORD -> "§f金剑";
            case DIAMOND_SWORD -> "§f钻石剑";
            case NETHERITE_SWORD -> "§f下界合金剑";
            case WOODEN_AXE -> "§f木斧";
            case STONE_AXE -> "§f石斧";
            case IRON_AXE -> "§f铁斧";
            case GOLDEN_AXE -> "§f金斧";
            case DIAMOND_AXE -> "§f钻石斧";
            case NETHERITE_AXE -> "§f下界合金斧";
            case LEATHER_HELMET -> "§f皮革头盔";
            case LEATHER_CHESTPLATE -> "§f皮革胸甲";
            case LEATHER_LEGGINGS -> "§f皮革护腿";
            case LEATHER_BOOTS -> "§f皮革靴子";
            case CHAINMAIL_HELMET -> "§f锁链头盔";
            case CHAINMAIL_CHESTPLATE -> "§f锁链胸甲";
            case CHAINMAIL_LEGGINGS -> "§f锁链护腿";
            case CHAINMAIL_BOOTS -> "§f锁链靴子";
            case IRON_HELMET -> "§f铁头盔";
            case IRON_CHESTPLATE -> "§f铁胸甲";
            case IRON_LEGGINGS -> "§f铁护腿";
            case IRON_BOOTS -> "§f铁靴子";
            case GOLDEN_HELMET -> "§f金头盔";
            case GOLDEN_CHESTPLATE -> "§f金胸甲";
            case GOLDEN_LEGGINGS -> "§f金护腿";
            case GOLDEN_BOOTS -> "§f金靴子";
            case DIAMOND_HELMET -> "§f钻石头盔";
            case DIAMOND_CHESTPLATE -> "§f钻石胸甲";
            case DIAMOND_LEGGINGS -> "§f钻石护腿";
            case DIAMOND_BOOTS -> "§f钻石靴子";
            case NETHERITE_HELMET -> "§f下界合金头盔";
            case NETHERITE_CHESTPLATE -> "§f下界合金胸甲";
            case NETHERITE_LEGGINGS -> "§f下界合金护腿";
            case NETHERITE_BOOTS -> "§f下界合金靴子";
            case TURTLE_HELMET -> "§f海龟壳";
            default -> "§f" + type.name();
        };
    }
}
