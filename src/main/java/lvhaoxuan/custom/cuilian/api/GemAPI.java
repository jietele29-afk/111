package lvhaoxuan.custom.cuilian.api;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.engine.NativeAttributeEngine;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Core API for in-bag 1-click Gem Socketing, 5-to-1 Gem Binding, and Gem Extraction.
 * All feedback is chat-only (no Title popups). Equipment has gemMaxSlots open by default (no punching).
 */
public final class GemAPI {

    public static NamespacedKey KEY_GEMS;

    private GemAPI() {
    }

    public static void initKeys(NewCustomCuiLianPro plugin) {
        KEY_GEMS = new NamespacedKey(plugin, "gem_list");
    }

    public static int getSlotCount(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item) || !CuiLianAPI.canCuiLian(item)) {
            return 0;
        }
        return NewCustomCuiLianPro.gemMaxSlots;
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

        List<String> gemIds = getSocketedGemIds(item);
        int totalSlots = getSlotCount(item);

        if (!gemIds.isEmpty()) {
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
        if (getEmptySlotCount(equip) <= 0) {
            return Message.GEM_NO_EMPTY_SLOT;
        }
        List<String> currentGems = getSocketedGemIds(equip);
        if (gem.maxSameGem > 0) {
            int sameFamilyCount = 0;
            for (String id : currentGems) {
                Gem existing = Gem.gems.get(id);
                if (existing != null && existing.family.equals(gem.family)) {
                    sameFamilyCount++;
                }
            }
            if (sameFamilyCount >= gem.maxSameGem) {
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
     * Sockets a gem directly onto an equipment item (chat feedback only, no Title popup).
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
            List<String> ids = getSocketedGemIds(updated);
            ids.add(gem.id);
            saveSocketedGemIds(updated, ids);
            refreshGemLore(updated);

            String msg = Message.GEM_INLAY_SUCCESS.replace("%s", gem.name);
            if (player != null) {
                player.sendMessage(msg);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.9f, 1.3f);
                NativeAttributeEngine.syncEntity(player);
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
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.9f, 1.0f);
            }
            return new InlayResult(false, gem.failDestroyGem, false, equip, msg);
        }
    }

    /**
     * Merges 5 identical-tier gems + 1 BinderItem into 1 next-tier gem (Lv.1 -> Lv.2 ... -> Lv.5).
     * Returns true if 1 BinderItem should be consumed from the cursor.
     */
    public static boolean bindGem(Player player, Gem sourceGem) {
        if (!NewCustomCuiLianPro.gemEnable || player == null || sourceGem == null) {
            return false;
        }
        Gem nextGem = sourceGem.getNextTier();
        if (nextGem == null) {
            player.sendMessage(Message.GEM_BIND_MAX_LEVEL);
            return false;
        }
        int need = Gem.binderNeedAmount;
        if (!ItemUtil.inventoryHasSimilar(player.getInventory(), sourceGem.item, need)) {
            player.sendMessage(Message.GEM_BIND_NOT_ENOUGH);
            return false;
        }
        if (ItemUtil.rollChance(Gem.binderChance)) {
            ItemUtil.inventoryRemoveSimilar(player.getInventory(), sourceGem.item, need);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(nextGem.createItemStack(1));
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(Message.GEM_BIND_SUCCESS.replace("%s", nextGem.name));
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.35f);
        } else {
            player.sendMessage(Message.GEM_BIND_FAIL);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.9f, 0.9f);
        }
        return true;
    }

    /**
     * Extracts the gem at the specified index from the equipment.
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
                NativeAttributeEngine.syncEntity(player);
            }
            return gem;
        } else {
            if (player != null) {
                player.sendMessage(Message.GEM_REMOVE_FAIL);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.9f, 0.9f);
            }
            return null;
        }
    }

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
