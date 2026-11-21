package lvhaoxuan.custom.cuilian.engine;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies native Minecraft AttributeModifiers directly to LivingEntities so all tempering
 * and gem stats take real effect in vanilla Paper/Purpur 1.20.4 without external plugins.
 */
public final class NativeAttributeEngine {

    private static final UUID MOD_HEALTH = UUID.fromString("c1a10001-7c3e-4d91-9b11-100000000001");
    private static final UUID MOD_ARMOR = UUID.fromString("c1a10002-7c3e-4d91-9b11-100000000002");
    private static final UUID MOD_TOUGHNESS = UUID.fromString("c1a10003-7c3e-4d91-9b11-100000000003");
    private static final UUID MOD_MOVE_SPEED = UUID.fromString("c1a10004-7c3e-4d91-9b11-100000000004");
    private static final UUID MOD_ATTACK_SPEED = UUID.fromString("c1a10005-7c3e-4d91-9b11-100000000005");
    private static final UUID MOD_KNOCKBACK = UUID.fromString("c1a10006-7c3e-4d91-9b11-100000000006");

    private static final Map<UUID, AttributeStats> cachedStats = new HashMap<>();

    private NativeAttributeEngine() {
    }

    public static AttributeStats getStats(LivingEntity entity) {
        if (entity == null) {
            return new AttributeStats();
        }
        return cachedStats.computeIfAbsent(entity.getUniqueId(), k -> AttributeStats.compute(entity));
    }

    public static void syncEntity(LivingEntity entity) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (!NewCustomCuiLianPro.nativeAttributeEngine) {
            clearEntity(entity);
            return;
        }
        AttributeStats stats = AttributeStats.compute(entity);
        cachedStats.put(entity.getUniqueId(), stats);

        applyModifier(entity, Attribute.GENERIC_MAX_HEALTH, MOD_HEALTH, "cuilian_health",
                stats.maxHealth, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(entity, Attribute.GENERIC_ARMOR, MOD_ARMOR, "cuilian_armor",
                stats.physicalDefense, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(entity, Attribute.GENERIC_ARMOR_TOUGHNESS, MOD_TOUGHNESS, "cuilian_toughness",
                stats.armorToughness, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(entity, Attribute.GENERIC_MOVEMENT_SPEED, MOD_MOVE_SPEED, "cuilian_move_speed",
                stats.moveSpeedPercent / 100.0, AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(entity, Attribute.GENERIC_ATTACK_SPEED, MOD_ATTACK_SPEED, "cuilian_attack_speed",
                stats.attackSpeedPercent / 100.0, AttributeModifier.Operation.ADD_SCALAR);
        applyModifier(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, MOD_KNOCKBACK, "cuilian_kb_resist",
                stats.knockbackResist / 100.0, AttributeModifier.Operation.ADD_NUMBER);

        // Clamp current health if max health decreased
        AttributeInstance hpAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttr != null && entity.getHealth() > hpAttr.getValue()) {
            entity.setHealth(Math.max(1.0, hpAttr.getValue()));
        }
    }

    public static void clearEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        cachedStats.remove(entity.getUniqueId());
        removeModifier(entity, Attribute.GENERIC_MAX_HEALTH, MOD_HEALTH);
        removeModifier(entity, Attribute.GENERIC_ARMOR, MOD_ARMOR);
        removeModifier(entity, Attribute.GENERIC_ARMOR_TOUGHNESS, MOD_TOUGHNESS);
        removeModifier(entity, Attribute.GENERIC_MOVEMENT_SPEED, MOD_MOVE_SPEED);
        removeModifier(entity, Attribute.GENERIC_ATTACK_SPEED, MOD_ATTACK_SPEED);
        removeModifier(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, MOD_KNOCKBACK);
    }

    private static void applyModifier(LivingEntity entity, Attribute attr, UUID modId, String name,
                                      double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier mod : inst.getModifiers()) {
            if (mod.getUniqueId().equals(modId)) {
                existing = mod;
                break;
            }
        }
        if (Math.abs(amount) < 1e-6) {
            if (existing != null) {
                inst.removeModifier(existing);
            }
            return;
        }
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 1e-6 && existing.getOperation() == op) {
                return; // Unchanged, avoid redundant attribute packet
            }
            inst.removeModifier(existing);
        }
        inst.addModifier(new AttributeModifier(modId, name, amount, op));
    }

    private static void removeModifier(LivingEntity entity, Attribute attr, UUID modId) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null) {
            return;
        }
        for (AttributeModifier mod : inst.getModifiers()) {
            if (mod.getUniqueId().equals(modId)) {
                inst.removeModifier(mod);
                break;
            }
        }
    }
}
