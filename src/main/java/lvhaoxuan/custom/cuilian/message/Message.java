package lvhaoxuan.custom.cuilian.message;

import lvhaoxuan.custom.cuilian.NewCustomCuiLianPro;
import lvhaoxuan.custom.cuilian.util.ItemUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Message {

    public static String MUST_ADD_IN_BAG = "§f[§d淬炼保护符§f]§c请在背包内右键";
    public static String CANCEL_ADD = "§f[§d淬炼保护符§f]§c你取消了本次淬炼保护!";
    public static String CAN_NOT_ADD = "§f[§d淬炼保护符§f]§c这个物品不能够进行淬炼保护.";
    public static String ADD_SUCCESS = "§a§l附加淬炼保护成功,§d成功对装备附加:%s";
    public static List<String> ADD_MESSAGE = new ArrayList<>();
    public static String SUCCESS = "§a§l淬炼成功,§e装备目前为：%s";
    public static String SERVER_SUCCESS = "§6[§6§l恭喜§6]%p§a用%d§a成功将装备淬炼至§6%s";
    public static String CUILIAN_FAIL_PROTECT_RUNE = "§c§l淬炼失败,§e装备目前为：%s§a(使用淬炼保护)§7(降%d星)";
    public static String CUILIAN_FAIL = "§c§l淬炼失败,§e装备目前为：%s§7(降%d星)";
    public static String DELETE_PROTECT_RUNE = "§c§l旧的淬炼保护符已拆除";
    public static String UNDER_LINE = "§c§l§m  §6§l§m  §e§l§m  §a§l§m  §b§l§m  §e§l装备淬炼§b§l§m  §a§l§m  §e§l§m  §6§l§m  §c§l§m  ";
    public static String ENABLE_SUIT_EFFECT = "§a你激活了%s§a§l套装效果";
    public static String DISENABLE_SUIT_EFFECT = "§c你失去了%s§c§l套装效果";

    // Gem messages
    public static String GEM_DISABLED = "§f[§b宝石镶嵌§f]§c宝石镶嵌系统当前未开启。";
    public static String GEM_CAN_NOT_INLAY = "§f[§b宝石镶嵌§f]§c该装备无法镶嵌此宝石。";
    public static String GEM_NO_EMPTY_SLOT = "§f[§b宝石镶嵌§f]§c该装备没有空余的宝石槽位，请先使用打孔符开孔！";
    public static String GEM_LEVEL_TOO_LOW = "§f[§b宝石镶嵌§f]§c该宝石需要装备淬炼达到 §e%d §c星以上才可镶嵌！";
    public static String GEM_TYPE_NOT_ALLOWED = "§f[§b宝石镶嵌§f]§c此宝石无法镶嵌在该部位装备上！";
    public static String GEM_MAX_SAME_REACHED = "§f[§b宝石镶嵌§f]§c该装备已镶嵌达到同类宝石数量上限（最多 %d 颗）！";
    public static String GEM_INLAY_SUCCESS = "§f[§b宝石镶嵌§f]§a§l镶嵌成功！§e已将 %s §e镶嵌至装备。";
    public static String GEM_INLAY_FAIL = "§f[§b宝石镶嵌§f]§c§l镶嵌失败！§7宝石已碎裂消散。";
    public static String GEM_INLAY_FAIL_KEEP = "§f[§b宝石镶嵌§f]§c§l镶嵌失败！§e幸运的是宝石未损坏。";
    public static String GEM_INLAY_FAIL_DESTROY_EQUIP = "§f[§b宝石镶嵌§f]§4§l镶嵌失败！§c装备在冲击中损毁了！";
    public static List<String> GEM_SELECT_START = new ArrayList<>();
    public static String GEM_SELECT_CANCEL = "§f[§b宝石镶嵌§f]§c已取消本次宝石/打孔操作。";
    public static String GEM_PUNCH_SUCCESS = "§f[§b装备打孔§f]§a§l打孔成功！§e装备当前宝石槽位: §b%d/%d";
    public static String GEM_PUNCH_FAIL = "§f[§b装备打孔§f]§c§l打孔失败！§7打孔符已消耗。";
    public static String GEM_PUNCH_MAX = "§f[§b装备打孔§f]§c该装备的宝石槽位已达到上限（%d 孔）！";
    public static String GEM_REMOVE_SUCCESS = "§f[§b宝石拆卸§f]§a§l拆卸成功！§e已将 %s §e完好取回背包。";
    public static String GEM_REMOVE_FAIL = "§f[§b宝石拆卸§f]§c§l拆卸失败！§7拆卸符已消耗，宝石仍保留在装备上。";
    public static String GEM_REMOVE_NO_TOOL = "§f[§b宝石拆卸§f]§c你的背包里没有宝石拆卸符！";
    public static String GEM_REMOVE_NO_GEMS = "§f[§b宝石拆卸§f]§c该装备上没有已镶嵌的宝石！";

    public static void loadMessages() {
        File dataFolder = NewCustomCuiLianPro.ins.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File file = new File(dataFolder, "message.yml");
        if (!file.exists()) {
            NewCustomCuiLianPro.ins.saveResource("message.yml", true);
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(reader);
            MUST_ADD_IN_BAG = ItemUtil.color(cfg.getString("MUST_ADD_IN_BAG", MUST_ADD_IN_BAG));
            CANCEL_ADD = ItemUtil.color(cfg.getString("CANCEL_ADD", CANCEL_ADD));
            CAN_NOT_ADD = ItemUtil.color(cfg.getString("CAN_NOT_ADD", CAN_NOT_ADD));
            ADD_SUCCESS = ItemUtil.color(cfg.getString("ADD_SUCCESS", ADD_SUCCESS));
            ADD_MESSAGE = ItemUtil.colorList(cfg.getStringList("ADD_MESSAGE"));
            SUCCESS = ItemUtil.color(cfg.getString("SUCCESS", SUCCESS));
            SERVER_SUCCESS = ItemUtil.color(cfg.getString("SERVER_SUCCESS", SERVER_SUCCESS));
            CUILIAN_FAIL_PROTECT_RUNE = ItemUtil.color(cfg.getString("CUILIAN_FAIL_PROTECT_RUNE", CUILIAN_FAIL_PROTECT_RUNE));
            CUILIAN_FAIL = ItemUtil.color(cfg.getString("CUILIAN_FAIL", CUILIAN_FAIL));
            DELETE_PROTECT_RUNE = ItemUtil.color(cfg.getString("DELETE_PROTECT_RUNE", DELETE_PROTECT_RUNE));
            UNDER_LINE = ItemUtil.color(cfg.getString("UNDER_LINE", UNDER_LINE));
            ENABLE_SUIT_EFFECT = ItemUtil.color(cfg.getString("ENABLE_SUIT_EFFECT", ENABLE_SUIT_EFFECT));
            DISENABLE_SUIT_EFFECT = ItemUtil.color(cfg.getString("DISENABLE_SUIT_EFFECT", DISENABLE_SUIT_EFFECT));

            GEM_DISABLED = ItemUtil.color(cfg.getString("GEM_DISABLED", GEM_DISABLED));
            GEM_CAN_NOT_INLAY = ItemUtil.color(cfg.getString("GEM_CAN_NOT_INLAY", GEM_CAN_NOT_INLAY));
            GEM_NO_EMPTY_SLOT = ItemUtil.color(cfg.getString("GEM_NO_EMPTY_SLOT", GEM_NO_EMPTY_SLOT));
            GEM_LEVEL_TOO_LOW = ItemUtil.color(cfg.getString("GEM_LEVEL_TOO_LOW", GEM_LEVEL_TOO_LOW));
            GEM_TYPE_NOT_ALLOWED = ItemUtil.color(cfg.getString("GEM_TYPE_NOT_ALLOWED", GEM_TYPE_NOT_ALLOWED));
            GEM_MAX_SAME_REACHED = ItemUtil.color(cfg.getString("GEM_MAX_SAME_REACHED", GEM_MAX_SAME_REACHED));
            GEM_INLAY_SUCCESS = ItemUtil.color(cfg.getString("GEM_INLAY_SUCCESS", GEM_INLAY_SUCCESS));
            GEM_INLAY_FAIL = ItemUtil.color(cfg.getString("GEM_INLAY_FAIL", GEM_INLAY_FAIL));
            GEM_INLAY_FAIL_KEEP = ItemUtil.color(cfg.getString("GEM_INLAY_FAIL_KEEP", GEM_INLAY_FAIL_KEEP));
            GEM_INLAY_FAIL_DESTROY_EQUIP = ItemUtil.color(cfg.getString("GEM_INLAY_FAIL_DESTROY_EQUIP", GEM_INLAY_FAIL_DESTROY_EQUIP));
            GEM_SELECT_START = ItemUtil.colorList(cfg.getStringList("GEM_SELECT_START"));
            GEM_SELECT_CANCEL = ItemUtil.color(cfg.getString("GEM_SELECT_CANCEL", GEM_SELECT_CANCEL));
            GEM_PUNCH_SUCCESS = ItemUtil.color(cfg.getString("GEM_PUNCH_SUCCESS", GEM_PUNCH_SUCCESS));
            GEM_PUNCH_FAIL = ItemUtil.color(cfg.getString("GEM_PUNCH_FAIL", GEM_PUNCH_FAIL));
            GEM_PUNCH_MAX = ItemUtil.color(cfg.getString("GEM_PUNCH_MAX", GEM_PUNCH_MAX));
            GEM_REMOVE_SUCCESS = ItemUtil.color(cfg.getString("GEM_REMOVE_SUCCESS", GEM_REMOVE_SUCCESS));
            GEM_REMOVE_FAIL = ItemUtil.color(cfg.getString("GEM_REMOVE_FAIL", GEM_REMOVE_FAIL));
            GEM_REMOVE_NO_TOOL = ItemUtil.color(cfg.getString("GEM_REMOVE_NO_TOOL", GEM_REMOVE_NO_TOOL));
            GEM_REMOVE_NO_GEMS = ItemUtil.color(cfg.getString("GEM_REMOVE_NO_GEMS", GEM_REMOVE_NO_GEMS));
        } catch (Exception e) {
            NewCustomCuiLianPro.ins.getLogger().log(Level.SEVERE, "加载 message.yml 失败", e);
        }
    }
}
