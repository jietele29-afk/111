package lvhaoxuan.custom.cuilian.runnable;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * Pure-Java high-star particle effects (wings & foot magic circles) triggered whenever
 * a player holds OR wears ANY item tempered to >= ParticleEffect.MinLevel (default 6 stars).
 * Works natively on Paper/Purpur 1.20.4 without NashornJs or LLib.
 */
public class ParticleEffectRunnable implements Runnable {

    private int tickStep = 0;

    // Stylized 2D wing silhouette (half-wing mirrored left & right)
    private static final double[][] WING_POINTS = {
            {0.35, 1.45}, {0.60, 1.60}, {0.90, 1.72}, {1.20, 1.78}, {1.48, 1.70},
            {0.45, 1.25}, {0.78, 1.38}, {1.12, 1.46}, {1.38, 1.42},
            {0.40, 1.05}, {0.72, 1.15}, {1.02, 1.18},
            {0.35, 0.85}, {0.62, 0.92}, {0.82, 0.86},
            {0.30, 0.68}, {0.50, 0.68}
    };

    @Override
    public void run() {
        if (!NewCustomCuiLianPro.particleEnable) {
            return;
        }
        tickStep = (tickStep + 1) % 360;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || player.isDead() || player.isInvisible()) {
                continue;
            }
            int maxStar = getHighestEquippedStar(player);
            if (maxStar < NewCustomCuiLianPro.particleMinLevel) {
                continue;
            }
            renderForTier(player, maxStar);
        }
    }

    private int getHighestEquippedStar(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) {
            return 0;
        }
        int highest = 0;
        for (ItemStack armor : eq.getArmorContents()) {
            highest = Math.max(highest, getStar(armor));
        }
        highest = Math.max(highest, getStar(eq.getItemInMainHand()));
        highest = Math.max(highest, getStar(eq.getItemInOffHand()));
        return highest;
    }

    private int getStar(ItemStack item) {
        if (!ItemUtil.isNotEmpty(item)) {
            return 0;
        }
        Level lv = Level.byItemStack(item);
        return lv != null && lv.value != null ? lv.value : 0;
    }

    private void renderForTier(Player player, int star) {
        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        double angleOffset = Math.toRadians(tickStep * 6.0);

        if (star >= 15) {
            // 15~18★ Supreme Tier: Dual-color Starlight Wings + Hexagram Magic Circle + Crown Halo
            drawWings(world, base,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.9f),
                    new Particle.DustOptions(Color.fromRGB(185, 80, 255), 0.9f),
                    true);
            drawMagicCircle(world, base, 1.35, angleOffset,
                    new Particle.DustOptions(Color.fromRGB(255, 200, 50), 0.85f), true);
            drawHalo(world, base, angleOffset);
        } else if (star >= 12) {
            // 12~14★ Angelic/Flame Wings + Rotating Foot Magic Circle
            drawWings(world, base,
                    new Particle.DustOptions(Color.fromRGB(255, 95, 35), 0.85f),
                    new Particle.DustOptions(Color.fromRGB(255, 220, 110), 0.85f),
                    false);
            drawMagicCircle(world, base, 1.20, angleOffset,
                    new Particle.DustOptions(Color.fromRGB(255, 110, 40), 0.8f), false);
        } else if (star >= 9) {
            // 9~11★ Flame Foot Magic Circle + Rising Twin Helix Ribbons
            drawMagicCircle(world, base, 1.15, angleOffset,
                    new Particle.DustOptions(Color.fromRGB(190, 90, 255), 0.8f), false);
            drawHelix(world, base, angleOffset);
        } else {
            // 6~8★ Starlight Foot Magic Circle (Cyan/Gold rotating rune ring)
            drawMagicCircle(world, base, 1.05, angleOffset,
                    new Particle.DustOptions(Color.fromRGB(70, 220, 255), 0.75f), false);
        }
    }

    private void drawMagicCircle(World world, Location base, double radius, double angleOffset,
                                 Particle.DustOptions dust, boolean hexagram) {
        int points = 16;
        for (int i = 0; i < points; i++) {
            double theta = angleOffset + (2.0 * Math.PI * i / points);
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            world.spawnParticle(Particle.REDSTONE, base.getX() + x, base.getY() + 0.08, base.getZ() + z, 1, 0, 0, 0, 0, dust);
        }
        // Rotating inner triangle / hexagram vertices
        int vertices = hexagram ? 6 : 3;
        for (int i = 0; i < vertices; i++) {
            double theta1 = -angleOffset + (2.0 * Math.PI * i / vertices);
            double theta2 = -angleOffset + (2.0 * Math.PI * ((i + 2) % vertices) / vertices);
            double x1 = Math.cos(theta1) * (radius * 0.85);
            double z1 = Math.sin(theta1) * (radius * 0.85);
            double x2 = Math.cos(theta2) * (radius * 0.85);
            double z2 = Math.sin(theta2) * (radius * 0.85);
            for (int step = 0; step <= 3; step++) {
                double t = step / 3.0;
                double px = base.getX() + x1 + (x2 - x1) * t;
                double pz = base.getZ() + z1 + (z2 - z1) * t;
                world.spawnParticle(Particle.END_ROD, px, base.getY() + 0.10, pz, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawHelix(World world, Location base, double angleOffset) {
        double h = (tickStep % 20) / 20.0 * 1.8;
        for (int strand = 0; strand < 2; strand++) {
            double theta = angleOffset * 2.0 + strand * Math.PI;
            double x = Math.cos(theta) * 0.75;
            double z = Math.sin(theta) * 0.75;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.getX() + x, base.getY() + 0.15 + h, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    private void drawWings(World world, Location base, Particle.DustOptions primary,
                           Particle.DustOptions secondary, boolean sparkle) {
        double yawRad = Math.toRadians(base.getYaw());
        // Player's right vector and back vector in world XZ plane
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);

        // Gentle wing flap animation
        double flap = Math.sin(tickStep * 0.25) * 0.18;

        for (int i = 0; i < WING_POINTS.length; i++) {
            double lateral = WING_POINTS[i][0];
            double height = WING_POINTS[i][1];
            double backOffset = 0.28 + lateral * (0.22 + flap);
            Particle.DustOptions color = (i % 2 == 0) ? primary : secondary;

            // Right wing
            double rx = base.getX() + rightX * lateral + backX * backOffset;
            double rz = base.getZ() + rightZ * lateral + backZ * backOffset;
            world.spawnParticle(Particle.REDSTONE, rx, base.getY() + height, rz, 1, 0, 0, 0, 0, color);

            // Left wing (mirrored)
            double lx = base.getX() - rightX * lateral + backX * backOffset;
            double lz = base.getZ() - rightZ * lateral + backZ * backOffset;
            world.spawnParticle(Particle.REDSTONE, lx, base.getY() + height, lz, 1, 0, 0, 0, 0, color);

            if (sparkle && i == 4 && tickStep % 3 == 0) {
                world.spawnParticle(Particle.END_ROD, rx, base.getY() + height, rz, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, lx, base.getY() + height, lz, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawHalo(World world, Location base, double angleOffset) {
        double r = 0.42;
        for (int i = 0; i < 8; i++) {
            double theta = angleOffset + (2.0 * Math.PI * i / 8.0);
            double x = Math.cos(theta) * r;
            double z = Math.sin(theta) * r;
            world.spawnParticle(Particle.END_ROD, base.getX() + x, base.getY() + 2.25, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }
}
