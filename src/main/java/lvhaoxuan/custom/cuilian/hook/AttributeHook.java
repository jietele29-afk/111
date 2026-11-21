package lvhaoxuan.custom.cuilian.hook;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Safe reflection-based hook supporting AttributePlus (v2 & v3) and SX-Attribute (v2, v3 & v4).
 * Never throws NoClassDefFoundError when soft-dependencies are missing or upgraded.
 */
public final class AttributeHook {

    private static final String SOURCE_KEY = "NewCustomCuiLianPro";

    private static boolean apV2Enabled = false;
    private static boolean apV3Enabled = false;
    private static boolean sxV2Enabled = false;
    private static boolean sxV3OrV4Enabled = false;

    private AttributeHook() {
    }

    public static void init(NewCustomCuiLianPro plugin) {
        apV2Enabled = false;
        apV3Enabled = false;
        sxV2Enabled = false;
        sxV3OrV4Enabled = false;

        Plugin ap = plugin.getServer().getPluginManager().getPlugin("AttributePlus");
        if (ap != null && ap.isEnabled()) {
            try {
                Class.forName("org.serverct.ersha.api.AttributeAPI");
                apV3Enabled = true;
                plugin.getLogger().info("已挂钩 AttributePlus (v3 API)");
            } catch (ClassNotFoundException ignored) {
                try {
                    Class.forName("org.serverct.ersha.jd.AttributeAPI");
                    apV2Enabled = true;
                    plugin.getLogger().info("已挂钩 AttributePlus (v2 API)");
                } catch (ClassNotFoundException ignored2) {
                    plugin.getLogger().warning("检测到 AttributePlus 但未找到兼容的 API 类");
                }
            }
        }

        Plugin sx = plugin.getServer().getPluginManager().getPlugin("SX-Attribute");
        if (sx != null && sx.isEnabled()) {
            String ver = sx.getDescription().getVersion();
            if (ver != null && ver.startsWith("2")) {
                sxV2Enabled = true;
                plugin.getLogger().info("已挂钩 SX-Attribute v2 (" + ver + ")");
            } else {
                // Covers SX-Attribute 3.x and 4.x
                sxV3OrV4Enabled = true;
                plugin.getLogger().info("已挂钩 SX-Attribute v3/v4 (" + ver + ")");
            }
        }
    }

    public static void applySuitAttributes(LivingEntity entity, List<String> attributes) {
        if (!(entity instanceof Player player) || attributes == null || attributes.isEmpty()) {
            return;
        }
        if (apV3Enabled) {
            applyApV3(player, attributes);
        } else if (apV2Enabled) {
            applyApV2(player, attributes);
        }

        if (sxV2Enabled) {
            applySxV2(player, attributes);
        } else if (sxV3OrV4Enabled) {
            applySxV3OrV4(player, attributes);
        }
    }

    public static void clearSuitAttributes(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (apV3Enabled) {
            clearApV3(player);
        } else if (apV2Enabled) {
            clearApV2(player);
        }
        if (sxV2Enabled || sxV3OrV4Enabled) {
            clearSx(player);
        }
    }

    private static void applyApV3(Player player, List<String> attributes) {
        try {
            Class<?> apiCls = Class.forName("org.serverct.ersha.api.AttributeAPI");
            Method getAttrData = apiCls.getMethod("getAttrData", LivingEntity.class);
            Object attrData = getAttrData.invoke(null, player);
            if (attrData == null) {
                return;
            }
            for (Method m : apiCls.getMethods()) {
                if (m.getName().equals("addPersistentSourceAttribute") && m.getParameterCount() == 3) {
                    m.invoke(null, attrData, SOURCE_KEY, attributes);
                    return;
                }
            }
            for (Method m : apiCls.getMethods()) {
                if (m.getName().equals("addSourceAttribute") && m.getParameterCount() == 3) {
                    m.invoke(null, attrData, SOURCE_KEY, attributes);
                    return;
                }
            }
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "AttributePlus v3 属性同步失败", e);
        }
    }

    private static void clearApV3(Player player) {
        try {
            Class<?> apiCls = Class.forName("org.serverct.ersha.api.AttributeAPI");
            Method getAttrData = apiCls.getMethod("getAttrData", LivingEntity.class);
            Object attrData = getAttrData.invoke(null, player);
            if (attrData == null) {
                return;
            }
            for (Method m : apiCls.getMethods()) {
                if (m.getName().equals("takeSourceAttribute") && m.getParameterCount() == 2) {
                    m.invoke(null, attrData, SOURCE_KEY);
                    return;
                }
            }
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "AttributePlus v3 属性清理失败", e);
        }
    }

    private static void applyApV2(Player player, List<String> attributes) {
        try {
            Class<?> apiCls = Class.forName("org.serverct.ersha.jd.AttributeAPI");
            Method addAttr = apiCls.getMethod("addAttribute", Player.class, String.class, List.class, boolean.class);
            addAttr.invoke(null, player, SOURCE_KEY, attributes, false);
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "AttributePlus v2 属性同步失败", e);
        }
    }

    private static void clearApV2(Player player) {
        try {
            Class<?> apiCls = Class.forName("org.serverct.ersha.jd.AttributeAPI");
            Method delAttr = apiCls.getMethod("deleteAttribute", Player.class, String.class);
            delAttr.invoke(null, player, SOURCE_KEY);
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "AttributePlus v2 属性清理失败", e);
        }
    }

    private static void applySxV2(Player player, List<String> attributes) {
        try {
            Class<?> sxCls = Class.forName("github.saukiya.sxattribute.SXAttribute");
            Object api = sxCls.getMethod("getApi").invoke(null);
            Class<?> condCls = Class.forName("github.saukiya.sxattribute.data.condition.SXConditionType");
            Method getLoreData = api.getClass().getMethod("getLoreData", LivingEntity.class, condCls, List.class);
            Object data = getLoreData.invoke(api, null, null, attributes);
            Class<?> dataCls = Class.forName("github.saukiya.sxattribute.data.attribute.SXAttributeData");
            Method setData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, dataCls);
            setData.invoke(api, AttributeHook.class, player.getUniqueId(), data);
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "SX-Attribute v2 属性同步失败", e);
        }
    }

    private static void applySxV3OrV4(Player player, List<String> attributes) {
        try {
            Class<?> sxCls = Class.forName("github.saukiya.sxattribute.SXAttribute");
            Object api = sxCls.getMethod("getApi").invoke(null);
            Method loadListData = api.getClass().getMethod("loadListData", List.class);
            Object data = loadListData.invoke(api, attributes);
            Class<?> dataCls = Class.forName("github.saukiya.sxattribute.data.attribute.SXAttributeData");
            Method setData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, dataCls);
            setData.invoke(api, AttributeHook.class, player.getUniqueId(), data);
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "SX-Attribute v3/v4 属性同步失败", e);
        }
    }

    private static void clearSx(Player player) {
        try {
            Class<?> sxCls = Class.forName("github.saukiya.sxattribute.SXAttribute");
            Object api = sxCls.getMethod("getApi").invoke(null);
            Method removeData = api.getClass().getMethod("removeEntityAPIData", Class.class, UUID.class);
            removeData.invoke(api, AttributeHook.class, player.getUniqueId());
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.WARNING, "SX-Attribute 属性清理失败", e);
        }
    }
}
