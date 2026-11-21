package lvhaoxuan.custom.cuilian.api;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Core API for Gem Socketing (宝石镶嵌), Socket Punching (装备打孔), and Gem Extraction (宝石拆卸).
 * Stores socket count and socketed gem IDs in Paper 1.20.4 PersistentDataContainer (PDC)
 * while rendering clean, AttributePlus/SX-Attribute-compatible Lore lines.
 */
public final class GemAPI {

    public static NamespacedKey KEY_SLOTS;
    public static NamespacedKey KEY_GEMS;

    private GemAPI() {
    }

    public static void initKeys(NewCustomCuiLianPro plugin) {
        KEY_SLOTS = new NamespacedKey(plugin, "gem_slots");
        KEY_GEMS = new NamespacedKey(plugin, "gem_list");
    }

    public static int getSlotCount(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item) || !CuiLianAPI.canCuiLian(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || KEY_SLOTS == null) {
            return NewCustomCuiLianPro.gemDefaultSlots;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_SLOTS, PersistentDataType.INTEGER)) {
            Integer val = pdc.get(KEY_SLOTS, PersistentDataType.INTEGER);
            return val != null ? Math.max(0, Math.min(val, NewCustomCuiLianPro.gemMaxSlots)) : 0;
        }
        return NewCustomCuiLianPro.gemDefaultSlots;
    }

    public static void setSlotCount(ItemStack item, int slots) {
        if (!ItemUtil.isNotEmpty(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || KEY_SLOTS == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(slots, NewCustomCuiLianPro.gemMaxSlots));
        meta.getPersistentDataContainer().set(KEY_SLOTS, PersistentDataType.INTEGER, clamped);
        item.setItemMeta(meta);
        refreshGemLore(item);
    }

    public static List<String> getSocketedGemIds(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (!ItemUtil.isNotEmpty(item)) {
            return result;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || KEY_GEMS == null) {
            return result;
        }
        String raw = meta.getPersistentDataContainer().get(KEY_GEMS, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static List<Gem> getSocketedGems(ItemStack item) {
        List<Gem> list = new ArrayList<>();
        for (String id : getSocketedGemIds(item)) {
            Gem gem = Gem.gems.get(id);
            if (gem != null) {
                list.add(gem);
            }
        }
        return list;
    }

    private static void saveSocketedGemIds(ItemStack item, List<String> ids) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || KEY_GEMS == null) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            meta.getPersistentDataContainer().remove(KEY_GEMS);
        } else {
            meta.getPersistentDataContainer().set(KEY_GEMS, PersistentDataType.STRING, String.join(",", ids));
        }
        item.setItemMeta(meta);
    }

    public static int getEmptySlotCount(ItemStack item) {
        return Math.max(0, getSlotCount(item) - getSocketedGemIds(item).size());
    }

    public static List<String> cleanGemLore(List<String> lore) {
        if (lore == null) {
            return new ArrayList<>();
        }
        Iterator<String> it = lore.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line == null) {
                continue;
            }
            if ((NewCustomCuiLianPro.GEM_JUDGE != null && !NewCustomCuiLianPro.GEM_JUDGE.isEmpty()
                    && line.contains(NewCustomCuiLianPro.GEM_JUDGE))
                    || (NewCustomCuiLianPro.GEM_UNDER_LINE != null && !NewCustomCuiLianPro.GEM_UNDER_LINE.isEmpty()
                    && line.equals(NewCustomCuiLianPro.GEM_UNDER_LINE))
                    || (NewCustomCuiLianPro.GEM_SLOT_EMPTY_LORE != null && !NewCustomCuiLianPro.GEM_SLOT_EMPTY_LORE.isEmpty()
                    && line.equals(NewCustomCuiLianPro.GEM_SLOT_EMPTY_LORE))
                    || (NewCustomCuiLianPro.GEM_SLOT_FILLED_PREFIX != null && !NewCustomCuiLianPro.GEM_SLOT_FILLED_PREFIX.isEmpty()
                    && line.startsWith(NewCustomCuiLianPro.GEM_SLOT_FILLED_PREFIX))) {
                it.remove();
            }
        }
        return lore;
    }

    /**
     * Re-renders the Gem Lore section at the bottom of the item's lore without disturbing
     * original lore, tempering lore, or protect rune lore.
     */
    public static ItemStack refreshGemLore(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item) || !CuiLianAPI.canCuiLian(item)) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        cleanGemLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hasExplicitSlots = KEY_SLOTS != null && pdc.has(KEY_SLOTS, PersistentDataType.INTEGER);
        List<String> gemIds = getSocketedGemIds(item);
        int totalSlots = getSlotCount(item);

        // Render gem section if the item has explicit slots punched OR has socketed gems
        if (hasExplicitSlots || !gemIds.isEmpty()) {
            if (NewCustomCuiLianPro.GEM_UNDER_LINE != null && !NewCustomCuiLianPro.GEM_UNDER_LINE.isEmpty()) {
                lore.add(NewCustomCuiLianPro.GEM_UNDER_LINE);
            }
            for (String gemId : gemIds) {
                Gem gem = Gem.gems.get(gemId);
                if (gem != null) {
                    lore.add(NewCustomCuiLianPro.GEM_SLOT_FILLED_PREFIX + gem.name);
                    for (String attr : gem.attribute) {
                        lore.add(NewCustomCuiLianPro.GEM_JUDGE + attr);
                    }
                }
            }
            int empty = Math.max(0, totalSlots - gemIds.size());
            for (int i = 0; i < empty; i++) {
                lore.add(NewCustomCuiLianPro.GEM_SLOT_EMPTY_LORE);
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Validates whether a gem can be socketed onto an equipment item.
     * Returns null if valid, or an error message string if invalid.
     */
    public static String validateInlay(ItemStack equip, Gem gem) {
        if (!NewCustomCuiLianPro.gemEnable) {
            return Message.GEM_DISABLED;
        }
        if (!ItemUtil.isNotEmpty(equip) || !CuiLianAPI.canCuiLian(equip) || gem == null) {
            return Message.GEM_CAN_NOT_INLAY;
        }
        String slotType = NewCustomCuiLianPro.typesInBag.get(equip.getType());
        if (slotType == null || !gem.allowsSlotType(slotType)) {
            return Message.GEM_TYPE_NOT_ALLOWED;
        }
        Level lv = Level.byItemStack(equip);
        int currentLv = lv != null ? lv.value : 0;
        if (currentLv < gem.minCuiLianLevel) {
            return Message.GEM_LEVEL_TOO_LOW.replace("%d", String.valueOf(gem.minCuiLianLevel));
        }
        int emptySlots = getEmptySlotCount(equip);
        if (emptySlots <= 0) {
            return Message.GEM_NO_EMPTY_SLOT;
        }
        List<String> currentGems = getSocketedGemIds(equip);
        if (gem.maxSameGem > 0) {
            int sameCount = 0;
            for (String id : currentGems) {
                if (id.equals(gem.id)) {
                    sameCount++;
                }
            }
            if (sameCount >= gem.maxSameGem) {
                return Message.GEM_MAX_SAME_REACHED.replace("%d", String.valueOf(gem.maxSameGem));
            }
        }
        return null;
    }

    public static class InlayResult {
        public final boolean success;
        public final boolean consumeGem;
        public final boolean destroyEquip;
        public final ItemStack resultEquip;
        public final String message;

        public InlayResult(boolean success, boolean consumeGem, boolean destroyEquip,
                           ItemStack resultEquip, String message) {
            this.success = success;
            this.consumeGem = consumeGem;
            this.destroyEquip = destroyEquip;
            this.resultEquip = resultEquip;
            this.message = message;
        }
    }

    /**
     * Attempts to socket a gem into an equipment item.
     */
    public static InlayResult inlayGem(ItemStack equip, Gem gem, Player player) {
        String err = validateInlay(equip, gem);
        if (err != null) {
            if (player != null) {
                player.sendMessage(err);
            }
            return new InlayResult(false, false, false, equip, err);
        }

        boolean success = ItemUtil.rollChance(gem.chance);
        if (success) {
            ItemStack updated = equip.clone();
            // Ensure slot count is explicitly persisted once a gem is added
            int slots = getSlotCount(updated);
            ItemMeta meta = updated.getItemMeta();
            if (meta != null && KEY_SLOTS != null && !meta.getPersistentDataContainer().has(KEY_SLOTS, PersistentDataType.INTEGER)) {
                meta.getPersistentDataContainer().set(KEY_SLOTS, PersistentDataType.INTEGER, slots);
                updated.setItemMeta(meta);
            }
            List<String> ids = getSocketedGemIds(updated);
            ids.add(gem.id);
            saveSocketedGemIds(updated, ids);
            refreshGemLore(updated);

            String msg = Message.GEM_INLAY_SUCCESS.replace("%s", gem.name);
            if (player != null) {
                player.sendMessage(msg);
                player.sendTitlePart(TitlePart.TITLE, Component.text("镶嵌成功", NamedTextColor.AQUA));
                player.sendTitlePart(TitlePart.SUBTITLE, Component.text(gem.name));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.3f);
            }
            return new InlayResult(true, true, false, updated, msg);
        } else {
            if (gem.failDestroyEquip) {
                String msg = Message.GEM_INLAY_FAIL_DESTROY_EQUIP;
                if (player != null) {
                    player.sendMessage(msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                }
                return new InlayResult(false, gem.failDestroyGem, true, null, msg);
            }
            String msg = gem.failDestroyGem ? Message.GEM_INLAY_FAIL : Message.GEM_INLAY_FAIL_KEEP;
            if (player != null) {
                player.sendMessage(msg);
                player.sendTitlePart(TitlePart.TITLE, Component.text("镶嵌失败", NamedTextColor.RED));
                player.sendTitlePart(TitlePart.SUBTITLE, Component.text(gem.name));
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
            return new InlayResult(false, gem.failDestroyGem, false, equip, msg);
        }
    }

    /**
     * Attempts to punch (+1) a new gem slot into an equipment item.
     */
    public static boolean punchSlot(ItemStack equip, Player player, boolean guaranteed) {
        if (!NewCustomCuiLianPro.gemEnable) {
            if (player != null) {
                player.sendMessage(Message.GEM_DISABLED);
            }
            return false;
        }
        if (!ItemUtil.isNotEmpty(equip) || !CuiLianAPI.canCuiLian(equip)) {
            if (player != null) {
                player.sendMessage(Message.GEM_CAN_NOT_INLAY);
            }
            return false;
        }
        int current = getSlotCount(equip);
        if (current >= NewCustomCuiLianPro.gemMaxSlots) {
            if (player != null) {
                player.sendMessage(Message.GEM_PUNCH_MAX.replace("%d", String.valueOf(NewCustomCuiLianPro.gemMaxSlots)));
            }
            return false;
        }
        if (guaranteed || ItemUtil.rollChance(Gem.punchChance)) {
            int next = current + 1;
            setSlotCount(equip, next);
            if (player != null) {
                String msg = Message.GEM_PUNCH_SUCCESS
                        .replaceFirst("%d", String.valueOf(next))
                        .replaceFirst("%d", String.valueOf(NewCustomCuiLianPro.gemMaxSlots));
                player.sendMessage(msg);
                player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
            }
            return true;
        } else {
            if (player != null) {
                player.sendMessage(Message.GEM_PUNCH_FAIL);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f);
            }
            return false;
        }
    }

    /**
     * Attempts to extract a gem at the given index from the equipment.
     * Returns the extracted Gem if successful, or null if failed/invalid.
     */
    public static Gem removeGemAt(ItemStack equip, int index, Player player) {
        List<String> ids = getSocketedGemIds(equip);
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        String gemId = ids.get(index);
        Gem gem = Gem.gems.get(gemId);
        double chance = gem != null ? gem.removeChance : 100.0;

        if (ItemUtil.rollChance(chance)) {
            ids.remove(index);
            saveSocketedGemIds(equip, ids);
            refreshGemLore(equip);
            if (player != null) {
                String name = gem != null ? gem.name : gemId;
                player.sendMessage(Message.GEM_REMOVE_SUCCESS.replace("%s", name));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
            return gem;
        } else {
            if (player != null) {
                player.sendMessage(Message.GEM_REMOVE_FAIL);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f);
            }
            return null;
        }
    }

    /**
     * Collects all potion effect strings ("POTION_NAME AMPLIFIER") from gems socketed
     * across an entity's armor and held items.
     */
    public static List<String> collectEquippedGemPotions(ItemStack[] armor, ItemStack mainHand, ItemStack offHand) {
        List<String> potions = new ArrayList<>();
        if (!NewCustomCuiLianPro.gemEnable) {
            return potions;
        }
        List<ItemStack> items = new ArrayList<>();
        if (armor != null) {
            items.addAll(Arrays.asList(armor));
        }
        if (ItemUtil.isNotEmpty(mainHand)) {
            items.add(mainHand);
        }
        if (NewCustomCuiLianPro.judgeOffHand && ItemUtil.isNotEmpty(offHand)) {
            items.add(offHand);
        }
        for (ItemStack item : items) {
            for (Gem gem : getSocketedGems(item)) {
                if (gem.potionEffect != null && !gem.potionEffect.isEmpty()) {
                    potions.addAll(gem.potionEffect);
                }
            }
        }
        return potions;
    }
}
