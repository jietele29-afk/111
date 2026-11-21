package lvhaoxuan.custom.cuilian.object;

import lvhaoxuan.custom.cuilian.loader.Loader;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

public class SuitEffect {

    public static String defaultScript = "var a = [\n"
            + "[1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1],\n"
            + "[0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0],\n"
            + "[0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0],\n"
            + "[0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0],\n"
            + "[0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0],\n"
            + "[0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0],\n"
            + "[0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0]];\n\n"
            + "function onEffectTick(entity){\n"
            + "\tvar Particle = Java.type(\"org.bukkit.Particle\");\n"
            + "\tfor (var i = a.length - 1; i >= 0; i--) {\n"
            + "\t\tfor (var j = a[i].length - 1; j >= 0; j--) {\n"
            + "\t\t\tif (a[i][j] != 0) {\n"
            + "\t\t\t\tdraw(entity.getLocation(), entity.getLocation().add(-a[i].length * 1.0 / 10 + j * 1.0 / 5 + 0.1, 0.2, 0), 1.8 - i * 1.0 / 5 + 0.2, Particle.FLAME, 20);\n"
            + "\t\t\t}\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}\n\n"
            + "function draw(PlayerLocation, EffectLocation, g, pe, addAngle) {\n"
            + "\tvar Ploc = PlayerLocation.clone();\n"
            + "\tvar Eloc = EffectLocation.clone();\n"
            + "\tvar jdc = getAngle(Ploc.getX(), Ploc.getY(), Eloc.getX(), Eloc.getY());\n"
            + "\tif (jdc < 90) {\n"
            + "\t\tjdc += addAngle;\n"
            + "\t} else {\n"
            + "\t\tjdc -= addAngle;\n"
            + "\t}\n"
            + "\tvar jl = getDistance(Ploc.getX(), Ploc.getY(), Eloc.getX(), Eloc.getY());\n"
            + "\tvar radians = toRadians(jdc + Ploc.getYaw() - 180);\n"
            + "\tvar x = Math.cos(radians) * jl;\n"
            + "\tvar y = Math.sin(radians) * jl;\n"
            + "\tPloc.add(x, g, y);\n"
            + "\tPloc.getWorld().spawnParticle(pe, Ploc, 0);\n"
            + "\tPloc.subtract(x, g, y);\n"
            + "}\n\n"
            + "function toRadians(angdeg) {\n"
            + "\treturn angdeg / 180.0 * Math.PI;\n"
            + "}\n\n"
            + "function getDistance(x1, y1, x2, y2) {\n"
            + "\tvar x = Math.abs(x2 - x1);\n"
            + "\tvar y = Math.abs(y2 - y1);\n"
            + "\treturn Math.sqrt(x * x + y * y);\n"
            + "}\n\n"
            + "function getAngle(x1, y1, x2, y2) {\n"
            + "\tvar x = x2 - x1;\n"
            + "\tvar y = y2 - y1;\n"
            + "\tvar hypotenuse = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));\n"
            + "\tif (hypotenuse === 0) return 0;\n"
            + "\tvar cos = x / hypotenuse;\n"
            + "\tvar radian = Math.acos(cos);\n"
            + "\tvar angle = 180 / (Math.PI / radian);\n"
            + "\tif (y < 0) {\n"
            + "\t\tangle = -angle;\n"
            + "\t} else if ((y == 0) && (x < 0)) {\n"
            + "\t\tangle = 180;\n"
            + "\t}\n"
            + "\treturn angle;\n"
            + "}";

    public List<String> potionEffect;
    public List<String> attribute;
    public List<String> script;

    public SuitEffect(List<String> potionEffect, List<String> attribute, List<String> script) {
        this.potionEffect = potionEffect != null ? potionEffect : new ArrayList<>();
        this.attribute = attribute != null ? attribute : new ArrayList<>();
        this.script = script != null ? script : new ArrayList<>();
    }

    public static SuitEffect deserialize(YamlConfiguration config, String path) {
        if (config.get(path) == null) {
            return null;
        }
        List<String> names = config.getStringList(path + ".Script");
        List<String> scripts = new ArrayList<>();
        for (String name : names) {
            String loaded = Loader.loadSuitEffectScriptStr(name);
            if (loaded != null && !loaded.isEmpty()) {
                scripts.add(loaded);
            }
        }
        return new SuitEffect(
                config.getStringList(path + ".PotionEffect"),
                config.getStringList(path + ".Attribute"),
                scripts
        );
    }
}
