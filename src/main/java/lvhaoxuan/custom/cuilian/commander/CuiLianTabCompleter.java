package lvhaoxuan.custom.cuilian.commander;

import lvhaoxuan.custom.cuilian.object.Gem;
import lvhaoxuan.custom.cuilian.object.Level;
import lvhaoxuan.custom.cuilian.object.Stone;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CuiLianTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        boolean admin = sender.isOp() || sender.hasPermission("cuilian.admin");

        if (args.length == 1) {
            suggestions.add("help");
            suggestions.add("moveLevel");
            suggestions.add("gem");
            if (admin) {
                suggestions.add("op");
                suggestions.add("set");
                suggestions.add("stone");
                suggestions.add("stones");
                suggestions.add("protect");
                suggestions.add("protects");
                suggestions.add("reload");
            }
            return filter(suggestions, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("gem") || sub.equals("inlay")) {
            if (args.length == 2) {
                suggestions.add("remove");
                if (admin) {
                    suggestions.add("list");
                    suggestions.add("give");
                    suggestions.add("binder");
                    suggestions.add("extractor");
                }
                return filter(suggestions, args[1]);
            }
            if (admin) {
                String gSub = args[1].toLowerCase(Locale.ROOT);
                if (gSub.equals("give")) {
                    if (args.length == 3) {
                        suggestions.addAll(Gem.gems.keySet());
                        return filter(suggestions, args[2]);
                    }
                    if (args.length == 4) {
                        return List.of("1", "5", "16", "64");
                    }
                    if (args.length == 5) {
                        return filter(onlinePlayerNames(), args[4]);
                    }
                } else if (gSub.equals("binder") || gSub.equals("extractor")) {
                    if (args.length == 3) {
                        return List.of("1", "5", "16");
                    }
                    if (args.length == 4) {
                        return filter(onlinePlayerNames(), args[3]);
                    }
                }
            }
            return List.of();
        }

        if (!admin) {
            return List.of();
        }

        if (sub.equals("stone")) {
            if (args.length == 2) {
                suggestions.addAll(Stone.stones.keySet());
                return filter(suggestions, args[1]);
            }
            if (args.length == 3) {
                return List.of("1", "16", "64");
            }
            if (args.length == 4) {
                return filter(onlinePlayerNames(), args[3]);
            }
        } else if (sub.equals("protect")) {
            if (args.length == 2) {
                for (Level lv : Level.levels.values()) {
                    if (lv.protectRune != null) {
                        suggestions.add(String.valueOf(lv.value));
                    }
                }
                return filter(suggestions, args[1]);
            }
            if (args.length == 3) {
                return List.of("1", "5", "16");
            }
            if (args.length == 4) {
                return filter(onlinePlayerNames(), args[3]);
            }
        } else if (sub.equals("set") && args.length == 2) {
            suggestions.add("0");
            for (Integer lv : Level.levels.keySet()) {
                suggestions.add(String.valueOf(lv));
            }
            return filter(suggestions, args[1]);
        }

        return List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return list;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String item : list) {
            if (item.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(item);
            }
        }
        return out;
    }
}
