package lvhaoxuan.custom.cuilian.runnable;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.api.CuiLianAPI;
import lvhaoxuan.custom.cuilian.loader.Loader;
import lvhaoxuan.custom.cuilian.object.Level;
import org.bukkit.entity.LivingEntity;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs SuitEffect particle scripts synchronously on the main server thread (Paper 1.20.4 safe).
 * Isolates failing scripts individually instead of disabling all scripts globally.
 */
public class ScriptRunnable implements Runnable {

    public static volatile boolean enableScript = true;
    public static final Map<UUID, Map<String, Bindings>> bindingsMap = new HashMap<>();
    public static final Map<String, ScriptEngine> engineMap = new HashMap<>();
    private static final Set<String> failedScripts = new HashSet<>();

    private static final String[] LLIB_PARTICLE_CLASSES = {
            "lvhaoxuan.llib.particle.ParticleModel",
            "lvhaoxuan.llib.particle.ParticleModel2D",
            "lvhaoxuan.llib.particle.ParticleModel3D",
            "lvhaoxuan.llib.particle.ParticleModelLine",
            "lvhaoxuan.llib.particle.ParticleUtil"
    };

    public static void resetCaches() {
        bindingsMap.clear();
        engineMap.clear();
        failedScripts.clear();
        enableScript = true;
    }

    public static void cleanupEntity(UUID uuid) {
        bindingsMap.remove(uuid);
    }

    @Override
    public void run() {
        if (!enableScript) {
            return;
        }
        List<LivingEntity> entities = SyncEffectRunnable.getEntities();
        for (LivingEntity le : entities) {
            if (le != null && le.isValid() && !le.isDead()) {
                sync(le);
            }
        }
    }

    public void sync(LivingEntity le) {
        UUID uuid = le.getUniqueId();
        Level minLevel = CuiLianAPI.getMinLevel(le, le.getEquipment());
        if (minLevel == null || minLevel.suitEffect == null || minLevel.suitEffect.script.isEmpty()) {
            return;
        }
        for (String script : minLevel.suitEffect.script) {
            if (script == null || script.isEmpty() || failedScripts.contains(script)) {
                continue;
            }
            try {
                ScriptEngine engine = engineMap.computeIfAbsent(script, this::getDefaultScriptEngine);
                if (!(engine instanceof Invocable invocable)) {
                    failedScripts.add(script);
                    continue;
                }
                Map<String, Bindings> entityBindings = bindingsMap.computeIfAbsent(uuid, k -> new HashMap<>());
                if (entityBindings.containsKey(script)) {
                    engine.setBindings(entityBindings.get(script), ScriptContext.ENGINE_SCOPE);
                }
                invocable.invokeFunction("onEffectTick", le);
                entityBindings.put(script, engine.getBindings(ScriptContext.ENGINE_SCOPE));
            } catch (Exception ex) {
                failedScripts.add(script);
                NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.WARNING,
                        "套装粒子脚本执行出错，已跳过该脚本（不影响其他功能）", ex);
            }
        }
    }

    public ScriptEngine getDefaultScriptEngine(String script) {
        ScriptEngine engine = Loader.createNashornEngine();
        if (engine == null) {
            return null;
        }
        try {
            for (String className : LLIB_PARTICLE_CLASSES) {
                try {
                    Class<?> cls = Class.forName(className);
                    uploadScriptClass(engine, cls);
                } catch (ClassNotFoundException ignored) {
                    // LLib optional
                }
            }
            engine.eval(script);
        } catch (Exception ex) {
            NewCustomCuiLianPro.ins.getLogger().log(java.util.logging.Level.WARNING, "初始化粒子脚本引擎失败", ex);
            return null;
        }
        return engine;
    }

    public static void uploadScriptClass(ScriptEngine engine, Class<?> clazz) throws Exception {
        String totalName = clazz.getSimpleName() + "_Static";
        String staticClassName = clazz.getSimpleName();
        engine.put(totalName, clazz);
        engine.eval("var " + staticClassName + " = " + totalName + ".static;");
    }
}
