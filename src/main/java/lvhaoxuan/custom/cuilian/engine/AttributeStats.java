package lvhaoxuan.custom.cuilian.engine;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.api.GemAPI;
import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aggregated real combat & entity stats computed from an entity's equipped/held
 * tempered items, socketed gems, and active suit effects.
 */
public class AttributeStats {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)");

    public double attackDamage = 0.0;       // 物理伤害
    public double physicalDefense = 0.0;    // 物理防御 (maps to armor + minor damage reduction)
    public double maxHealth = 0.0;          // 生命上限
    public double armorToughness = 0.0;     // 盔甲韧性
    public double moveSpeedPercent = 0.0;   // 移动速度 (%)
    public double attackSpeedPercent = 0.0; // 攻击速度 (%)
    public double critChance = 0.0;         // 暴击几率 (%)
    public double critDamagePercent = 0.0;  // 暴击伤害额外加成 (%)
    public double lifestealPercent = 0.0;   // 吸血 (%)
    public double dodgeChance = 0.0;        // 闪避几率 (%)
    public double reflectPercent = 0.0;     // 伤害反弹 (%)
    public double knockbackResist = 0.0;    // 抗击退 (%)

    public static AttributeStats compute(LivingEntity entity) {
        AttributeStats stats = new AttributeStats();
        if (entity == null) {
            return stats;
        }
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) {
            return stats;
        }

        // Armor slots: Helmet, Chestplate, Leggings, Boots
        applyItem(stats, eq.getHelmet(), "Helmet");
        applyItem(stats, eq.getChestplate(), "Chestplate");
        applyItem(stats, eq.getLeggings(), "Leggings");
        applyItem(stats, eq.getBoots(), "Boots");

        // Main hand (weapons / tools)
        applyItem(stats, eq.getItemInMainHand(), "Hand");

        // Off hand if enabled
        if (NewCustomCuiLianPro.judgeOffHand) {
            applyItem(stats, eq.getItemInOffHand(), "Hand");
        }

        // Active SuitEffect attributes
        Level minLevel = CuiLianAPI.getMinLevel(entity, eq);
        if (minLevel != null && minLevel.suitEffect != null && minLevel.suitEffect.attribute != null) {
            for (String line : minLevel.suitEffect.attribute) {
                stats.parseLine(line);
            }
        }

        // Clamp percentages to sane gameplay caps
        stats.dodgeChance = Math.min(60.0, Math.max(0.0, stats.dodgeChance));
        stats.critChance = Math.min(100.0, Math.max(0.0, stats.critChance));
        stats.lifestealPercent = Math.min(50.0, Math.max(0.0, stats.lifestealPercent));
        stats.reflectPercent = Math.min(60.0, Math.max(0.0, stats.reflectPercent));
        stats.moveSpeedPercent = Math.min(100.0, Math.max(-50.0, stats.moveSpeedPercent));
        stats.attackSpeedPercent = Math.min(150.0, Math.max(-50.0, stats.attackSpeedPercent));
        stats.knockbackResist = Math.min(100.0, Math.max(0.0, stats.knockbackResist));

        return stats;
    }

    private static void applyItem(AttributeStats stats, ItemStack item, String expectedSlot) {
        if (!ItemUtil.isNotEmpty(item) || !CuiLianAPI.canCuiLian(item)) {
            return;
        }
        String itemSlot = NewCustomCuiLianPro.typesInBag.get(item.getType());
        // Only apply stats when the item is in its proper equipment slot (e.g. sword in Hand, helmet on Helmet)
        if (itemSlot == null || !itemSlot.equalsIgnoreCase(expectedSlot)) {
            return;
        }

        // 1) Tempering level attributes
        Level level = Level.byItemStack(item);
        if (level != null && level.attribute != null) {
            List<String> attrs = level.attribute.get(itemSlot);
            if (attrs != null) {
                for (String line : attrs) {
                    stats.parseLine(line);
                }
            }
        }

        // 2) Socketed gem attributes
        if (NewCustomCuiLianPro.gemEnable) {
            for (Gem gem : GemAPI.getSocketedGems(item)) {
                if (gem.attribute != null) {
                    for (String line : gem.attribute) {
                        stats.parseLine(line);
                    }
                }
            }
        }
    }

    public void parseLine(String rawLine) {
        if (rawLine == null || rawLine.isEmpty()) {
            return;
        }
        String plain = ChatColor.stripColor(rawLine);
        if (plain == null || plain.isEmpty()) {
            return;
        }
        Matcher m = NUMBER_PATTERN.matcher(plain);
        if (!m.find()) {
            return;
        }
        double val;
        try {
            val = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }

        if (plain.contains("物理伤害") || plain.contains("攻击伤害") || plain.contains("近战伤害")) {
            attackDamage += val;
        } else if (plain.contains("物理防御") || plain.contains("护甲值") || plain.contains("防御")) {
            physicalDefense += val;
        } else if (plain.contains("生命上限") || plain.contains("最大血量") || plain.contains("最大生命")) {
            maxHealth += val;
        } else if (plain.contains("盔甲韧性") || plain.contains("护甲韧性")) {
            armorToughness += val;
        } else if (plain.contains("移动速度")) {
            moveSpeedPercent += val;
        } else if (plain.contains("攻击速度")) {
            attackSpeedPercent += val;
        } else if (plain.contains("暴击几率")) {
            critChance += val;
        } else if (plain.contains("暴击伤害")) {
            critDamagePercent += val;
        } else if (plain.contains("吸血") || plain.contains("生命吸取")) {
            lifestealPercent += val;
        } else if (plain.contains("闪避") || plain.contains("躲闪")) {
            dodgeChance += val;
        } else if (plain.contains("伤害反弹") || plain.contains("反伤")) {
            reflectPercent += val;
        } else if (plain.contains("抗击退") || plain.contains("击退抗性")) {
            knockbackResist += val;
        }
    }
}
