package lvhaoxuan.custom.cuilian.runnable;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.hook.AttributeHook;
import lvhaoxuan.custom.cuilian.message.Message;
import lvhaoxuan.custom.cuilian.object.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs synchronously on the main thread every 10 ticks to synchronize:
 * - Tempering SuitEffect potion effects & AttributePlus/SX-Attribute bonuses
 * - Socketed Gem passive potion effects
 */
public class SyncEffectRunnable implements Runnable {

    public static final Map<UUID, Level> activeSuitMap = new HashMap<>();

    public static void cleanupEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        UUID uuid = entity.getUniqueId();
        if (activeSuitMap.remove(uuid) != null) {
            AttributeHook.clearSuitAttributes(entity);
        }
        ScriptRunnable.cleanupEntity(uuid);
    }

    @Override
    public void run() {
        for (LivingEntity le : getEntities()) {
            if (le != null && le.isValid() && !le.isDead()) {
                sync(le);
            }
        }
    }

    public void sync(LivingEntity le) {
        EntityEquipment eq = le.getEquipment();
        if (eq == null) {
            return;
        }

        // 1) Apply passive potion effects from socketed gems on equipped armor/weapons
        List<String> gemPotions = GemAPI.collectEquippedGemPotions(
                eq.getArmorContents(), eq.getItemInMainHand(), eq.getItemInOffHand());
        applyPotionStrings(le, gemPotions);

        // 2) Apply tempering suit effect
        UUID uuid = le.getUniqueId();
        Level minLevel = CuiLianAPI.getMinLevel(le, eq);
        Level previousLevel = activeSuitMap.get(uuid);

        if (minLevel != null && minLevel.suitEffect != null) {
            applyPotionStrings(le, minLevel.suitEffect.potionEffect);
            AttributeHook.applySuitAttributes(le, minLevel.suitEffect.attribute);

            if (previousLevel == null) {
                le.sendMessage(Message.ENABLE_SUIT_EFFECT.replace("%s", minLevel.getFirstLore()));
                activeSuitMap.put(uuid, minLevel);
            } else if (!previousLevel.value.equals(minLevel.value)) {
                le.sendMessage(Message.DISENABLE_SUIT_EFFECT.replace("%s", previousLevel.getFirstLore()));
                le.sendMessage(Message.ENABLE_SUIT_EFFECT.replace("%s", minLevel.getFirstLore()));
                activeSuitMap.put(uuid, minLevel);
            }
        } else if (previousLevel != null) {
            le.sendMessage(Message.DISENABLE_SUIT_EFFECT.replace("%s", previousLevel.getFirstLore()));
            activeSuitMap.remove(uuid);
            AttributeHook.clearSuitAttributes(le);
        }
    }

    private void applyPotionStrings(LivingEntity le, List<String> potionList) {
        if (potionList == null || potionList.isEmpty()) {
            return;
        }
        for (String potionStr : potionList) {
            if (potionStr == null || potionStr.trim().isEmpty()) {
                continue;
            }
            String[] args = potionStr.trim().split("\\s+");
            if (args.length < 2) {
                continue;
            }
            PotionEffectType type = PotionEffectType.getByName(args[0].toUpperCase());
            if (type == null) {
                continue;
            }
            try {
                int amplifier = Integer.parseInt(args[1]);
                le.addPotionEffect(new PotionEffect(type, 30, amplifier, true, false));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static List<LivingEntity> getEntities() {
        List<LivingEntity> entities = new ArrayList<>();
        if (!NewCustomCuiLianPro.otherEntitySuitEffect) {
            entities.addAll(Bukkit.getOnlinePlayers());
        } else {
            for (World world : Bukkit.getWorlds()) {
                entities.addAll(world.getLivingEntities());
            }
        }
        return entities;
    }
}
