package lvhaoxuan.custom.cuilian.listener;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.engine.AttributeStats;
import lvhaoxuan.custom.cuilian.engine.NativeAttributeEngine;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Executes real combat math for tempering & gem stats:
 * - Attack Damage (物理伤害, melee & projectiles)
 * - Critical Hit Chance & Critical Damage (暴击几率 & 暴击伤害)
 * - Lifesteal (吸血)
 * - Dodge Chance (闪避几率)
 * - Thorns / Damage Reflect (伤害反弹)
 */
public class CombatAttributeListener implements Listener {

    // Guard against infinite thorns reflection loops
    private static final Set<UUID> reflectingGuard = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!NewCustomCuiLianPro.nativeAttributeEngine) {
            return;
        }
        if (!(e.getEntity() instanceof LivingEntity defender)) {
            return;
        }

        LivingEntity attacker = resolveAttacker(e);
        if (attacker == null || reflectingGuard.contains(attacker.getUniqueId())) {
            return;
        }

        // Compute fresh stats for attacker & defender
        AttributeStats atkStats = AttributeStats.compute(attacker);
        AttributeStats defStats = AttributeStats.compute(defender);

        // 1) Defender Dodge check (水夕石)
        if (defStats.dodgeChance > 0.0 && ItemUtil.rollChance(defStats.dodgeChance)) {
            e.setCancelled(true);
            defender.getWorld().spawnParticle(Particle.CLOUD, defender.getLocation().add(0, 1.0, 0), 8, 0.3, 0.4, 0.3, 0.04);
            defender.getWorld().playSound(defender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
            if (defender instanceof Player dp) {
                dp.sendActionBar("§b✦ 闪避成功！");
            }
            return;
        }

        double damage = e.getDamage();

        // 2) Attacker Physical Damage bonus (淬炼武器 / 赤炎石)
        if (atkStats.attackDamage > 0.0) {
            damage += atkStats.attackDamage;
        }

        // 3) Attacker Crit Chance & Crit Damage (破军石 / 紫霄石)
        boolean isCrit = false;
        if (atkStats.critChance > 0.0 && ItemUtil.rollChance(atkStats.critChance)) {
            isCrit = true;
            double critMultiplier = 1.5 + (atkStats.critDamagePercent / 100.0);
            damage *= critMultiplier;
            defender.getWorld().spawnParticle(Particle.CRIT_MAGIC, defender.getLocation().add(0, 1.1, 0), 12, 0.35, 0.4, 0.35, 0.1);
            defender.getWorld().playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        }

        e.setDamage(Math.max(0.0, damage));

        // 4) Attacker Lifesteal (血珀石)
        if (atkStats.lifestealPercent > 0.0 && damage > 0.0) {
            double heal = damage * (atkStats.lifestealPercent / 100.0);
            if (heal > 0.05) {
                AttributeInstance maxHp = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double cap = maxHp != null ? maxHp.getValue() : 20.0;
                attacker.setHealth(Math.min(cap, attacker.getHealth() + heal));
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 2, 0.25, 0.25, 0.25, 0.0);
            }
        }

        // 5) Defender Damage Reflection (沧澜石)
        if (defStats.reflectPercent > 0.0 && damage > 0.0 && attacker != defender) {
            double reflectDmg = damage * (defStats.reflectPercent / 100.0);
            if (reflectDmg >= 0.2) {
                UUID defId = defender.getUniqueId();
                reflectingGuard.add(defId);
                try {
                    attacker.damage(reflectDmg, defender);
                } finally {
                    reflectingGuard.remove(defId);
                }
            }
        }

        if (isCrit && attacker instanceof Player ap) {
            ap.sendActionBar("§e✦ 暴击! §c-" + String.format("%.1f", damage));
        }

        // Keep native attributes synced when fighting
        if (attacker instanceof Player) {
            NativeAttributeEngine.syncEntity(attacker);
        }
    }

    private LivingEntity resolveAttacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof LivingEntity le) {
            return le;
        }
        if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }
}
