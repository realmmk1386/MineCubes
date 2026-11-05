package ir.mmk.cubegen.commands;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.cube.PlacedCube;
import ir.mmk.cubegen.gui.TemplateBlockEditorGui;
import ir.mmk.cubegen.template.CubeTemplate;
import ir.mmk.cubegen.util.ItemUtil;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CubeGenCommand implements CommandExecutor, TabCompleter {

    private final CubeGen plugin;

    public CubeGenCommand(CubeGen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // ----------------- /cg give <player> <template> [amount] -----------------
        if (sub.equals("give")) {
            if (!sender.hasPermission("cubegen.admin") && !sender.hasPermission("cubegen.give")) {
                MessageUtil.sendMessage(sender, "&cYou do not have permission to use this command!");
                return true;
            }

            if (args.length < 3) {
                MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " give <player> <template> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                MessageUtil.sendMessage(sender, "&cPlayer &e" + args[1] + " &cnot found or offline!");
                return true;
            }

            String templateId = args[2];
            CubeTemplate template = plugin.getTemplateManager().getTemplate(templateId);
            if (template == null) {
                MessageUtil.sendMessage(sender, "&cTemplate &e" + templateId + " &cdoes not exist!");
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[3]));
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "&cAmount must be a valid integer!");
                    return true;
                }
            }

            ItemStack item = template.getLinkedItem();
            item.setAmount(amount);

            HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), drop);
                }
                MessageUtil.sendMessage(target, "&eYour inventory was full! CubeGen item was dropped at your feet.");
            }

            MessageUtil.sendMessage(sender, "&aGave &e" + amount + "x &b" + template.getDisplayName() + " &ato &e" + target.getName() + "&a!");
            MessageUtil.sendMessage(target, "&aYou received &e" + amount + "x &b" + template.getDisplayName() + "&a!");
            return true;
        }

        // ----------------- /cg template ... -----------------
        if (sub.equals("template") || sub.equals("tpl")) {
            if (!sender.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(sender, "&cYou do not have permission to manage templates!");
                return true;
            }

            if (args.length < 2) {
                sendTemplateHelp(sender, label);
                return true;
            }

            String action = args[1].toLowerCase(Locale.ROOT);

            // /cg template list
            if (action.equals("list")) {
                Collection<CubeTemplate> list = plugin.getTemplateManager().getAllTemplates();
                MessageUtil.sendMessage(sender, "&8&m--------------------------------------------------");
                MessageUtil.sendMessage(sender, "&b&lCubeGen Templates &7(" + list.size() + "):");
                for (CubeTemplate t : list) {
                    MessageUtil.sendMessage(sender, " &8• &b" + t.getId() + " &7- Size: &f" + t.getSize()
                            + " &7| Type: &f" + t.getType() + " &7| Regen: &f" + t.getRegenInterval() + "s"
                            + " &7| Blocks: &e" + t.getBlockChances().size());
                }
                MessageUtil.sendMessage(sender, "&8&m--------------------------------------------------");
                return true;
            }

            // /cg template create <id> [size] [type] [regen]
            if (action.equals("create")) {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template create <name> [size] [type] [regen_seconds]");
                    return true;
                }

                String id = args[2];
                int size = 5;
                String type = "protect";
                int regen = 10;

                if (args.length >= 4) {
                    try { size = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                }
                if (args.length >= 5) {
                    type = args[4].equalsIgnoreCase("normal") ? "normal" : "protect";
                }
                if (args.length >= 6) {
                    try { regen = Integer.parseInt(args[5]); } catch (Exception ignored) {}
                }

                boolean created = plugin.getTemplateManager().createTemplate(id, size, type, regen);
                if (created) {
                    MessageUtil.sendMessage(sender, "&aSuccessfully created template &b" + id + "&a!");
                    MessageUtil.sendMessage(sender, "&7Use &b/" + label + " template edit " + id + " &7to set blocks in GUI.");
                } else {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + id + " &calready exists!");
                }
                return true;
            }

            // /cg template remove <id>
            if (action.equals("remove") || action.equals("delete")) {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template remove <name>");
                    return true;
                }
                if (plugin.getTemplateManager().deleteTemplate(args[2])) {
                    MessageUtil.sendMessage(sender, "&aTemplate &b" + args[2] + " &aremoved successfully!");
                } else {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + args[2] + " &cnot found!");
                }
                return true;
            }

            // /cg template edit <id> (GUI)
            if (action.equals("edit") || action.equals("setblocks")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, "&cThis command can only be used by players!");
                    return true;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template edit <name>");
                    return true;
                }

                Player player = (Player) sender;
                String id = args[2];
                CubeTemplate t = plugin.getTemplateManager().getTemplate(id);
                if (t == null) {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + id + " &cdoes not exist!");
                    return true;
                }

                TemplateBlockEditorGui.open(player, id, plugin);
                return true;
            }

            // /cg template linkitem <id>
            if (action.equals("linkitem") || action.equals("link")) {
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, "&cThis command can only be used by players!");
                    return true;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template linkitem <name>");
                    return true;
                }

                Player player = (Player) sender;
                String id = args[2];
                CubeTemplate t = plugin.getTemplateManager().getTemplate(id);
                if (t == null) {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + id + " &cdoes not exist!");
                    return true;
                }

                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    MessageUtil.sendMessage(sender, "&cYou must hold an item in your main hand to link it!");
                    return true;
                }

                ItemStack linked = held.clone();
                linked.setAmount(1);
                t.setLinkedItem(linked);
                plugin.getTemplateManager().save();

                MessageUtil.sendMessage(sender, "&aSuccessfully linked your held item to template &b" + id + "&a!");
                return true;
            }

            // /cg template setsize <id> <size>
            if (action.equals("setsize")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template setsize <name> <size>");
                    return true;
                }
                CubeTemplate t = plugin.getTemplateManager().getTemplate(args[2]);
                if (t == null) {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + args[2] + " &cnot found!");
                    return true;
                }
                try {
                    int size = Integer.parseInt(args[3]);
                    if (size < 3 || size % 2 == 0) {
                        MessageUtil.sendMessage(sender, "&cSize must be an odd number greater than or equal to 3 (e.g. 3, 5, 7)!");
                        return true;
                    }
                    t.setSize(size);
                    plugin.getTemplateManager().save();
                    MessageUtil.sendMessage(sender, "&aSet size for template &b" + t.getId() + " &ato &e" + size + "&a!");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "&cInvalid size number!");
                }
                return true;
            }

            // /cg template setregen <id> <seconds>
            if (action.equals("setregen")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template setregen <name> <seconds>");
                    return true;
                }
                CubeTemplate t = plugin.getTemplateManager().getTemplate(args[2]);
                if (t == null) {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + args[2] + " &cnot found!");
                    return true;
                }
                try {
                    int regen = Integer.parseInt(args[3]);
                    t.setRegenInterval(Math.max(0, regen));
                    plugin.getTemplateManager().save();
                    MessageUtil.sendMessage(sender, "&aSet regen interval for &b" + t.getId() + " &ato &e" + regen + "s&a!");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "&cInvalid number!");
                }
                return true;
            }

            // /cg template settype <id> <normal|protect>
            if (action.equals("settype")) {
                if (args.length < 4) {
                    MessageUtil.sendMessage(sender, "&cUsage: &b/" + label + " template settype <name> <normal|protect>");
                    return true;
                }
                CubeTemplate t = plugin.getTemplateManager().getTemplate(args[2]);
                if (t == null) {
                    MessageUtil.sendMessage(sender, "&cTemplate &e" + args[2] + " &cnot found!");
                    return true;
                }
                String type = args[3].equalsIgnoreCase("normal") ? "normal" : "protect";
                t.setType(type);
                plugin.getTemplateManager().save();
                MessageUtil.sendMessage(sender, "&aSet type for &b" + t.getId() + " &ato &e" + type + "&a!");
                return true;
            }

            sendTemplateHelp(sender, label);
            return true;
        }

        // ----------------- /cg list -----------------
        if (sub.equals("list")) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(sender, "&cThis command can only be used by players!");
                return true;
            }
            Player player = (Player) sender;
            List<PlacedCube> cubes = plugin.getPlacedCubeManager().getPlayerCubes(player.getUniqueId());

            MessageUtil.sendMessage(player, "&8&m--------------------------------------------------");
            MessageUtil.sendMessage(player, "&b&lYour Placed CubeGens &7(" + cubes.size() + "):");
            if (cubes.isEmpty()) {
                MessageUtil.sendMessage(player, "&7You have not placed any CubeGens yet.");
            } else {
                int i = 1;
                for (PlacedCube c : cubes) {
                    MessageUtil.sendMessage(player, " &8[&f" + (i++) + "&8] &b" + c.getTemplateId()
                            + " &7at &e" + c.getWorldName() + " (" + c.getCenterX() + ", " + c.getCenterY() + ", " + c.getCenterZ() + ")");
                }
            }
            MessageUtil.sendMessage(player, "&8&m--------------------------------------------------");
            return true;
        }

        // ----------------- /cg unplace [id/template] or /cg pickup -----------------
        if (sub.equals("unplace") || sub.equals("pickup")) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(sender, "&cThis command can only be used by players!");
                return true;
            }
            Player player = (Player) sender;
            String query = (args.length >= 2) ? args[1] : null;

            PlacedCube cube = plugin.getPlacedCubeManager().findCubeForPlayer(player, query);
            if (cube == null) {
                MessageUtil.sendMessage(player, "&cNo matching placed CubeGen found!");
                return true;
            }

            plugin.getPlacedCubeManager().unplaceCube(cube, player, true);
            return true;
        }

        // ----------------- /cg reload -----------------
        if (sub.equals("reload")) {
            if (!sender.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(sender, "&cYou do not have permission to reload the plugin!");
                return true;
            }
            plugin.reloadPlugin();
            MessageUtil.sendMessage(sender, "&aCubeGen configurations reloaded successfully!");
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        MessageUtil.sendMessage(sender, "&8&m--------------------------------------------------");
        MessageUtil.sendMessage(sender, "&b&lMine&e&lCubes &f- &3Commands");
        MessageUtil.sendMessage(sender, "&8&m--------------------------------------------------");
        MessageUtil.sendMessage(sender, "&b/" + label + " give <player> <template> [amount] &7- Give CubeGen item");
        MessageUtil.sendMessage(sender, "&b/" + label + " template list &7- List all templates");
        MessageUtil.sendMessage(sender, "&b/" + label + " template create <name> [size] [type] [regen] &7- Create template");
        MessageUtil.sendMessage(sender, "&b/" + label + " template edit <name> &7- Open GUI to set blocks & chances");
        MessageUtil.sendMessage(sender, "&b/" + label + " template linkitem <name> &7- Link held item to template");
        MessageUtil.sendMessage(sender, "&b/" + label + " template setsize <name> <size> &7- Change template size");
        MessageUtil.sendMessage(sender, "&b/" + label + " template setregen <name> <seconds> &7- Change regen time");
        MessageUtil.sendMessage(sender, "&b/" + label + " template settype <name> <normal|protect> &7- Change type");
        MessageUtil.sendMessage(sender, "&b/" + label + " template remove <name> &7- Remove a template");
        MessageUtil.sendMessage(sender, "&b/" + label + " list &7- List your placed cubes");
        MessageUtil.sendMessage(sender, "&b/" + label + " unplace [name] &7- Pick up a placed cube");
        MessageUtil.sendMessage(sender, "&b/" + label + " reload &7- Reload configurations");
        MessageUtil.sendMessage(sender, "&8&m--------------------------------------------------");
    }

    private void sendTemplateHelp(CommandSender sender, String label) {
        MessageUtil.sendMessage(sender, "&cUsage:");
        MessageUtil.sendMessage(sender, "&b/" + label + " template list");
        MessageUtil.sendMessage(sender, "&b/" + label + " template create <name> [size] [type] [regen]");
        MessageUtil.sendMessage(sender, "&b/" + label + " template edit <name>");
        MessageUtil.sendMessage(sender, "&b/" + label + " template linkitem <name>");
        MessageUtil.sendMessage(sender, "&b/" + label + " template setsize <name> <size>");
        MessageUtil.sendMessage(sender, "&b/" + label + " template setregen <name> <seconds>");
        MessageUtil.sendMessage(sender, "&b/" + label + " template settype <name> <normal|protect>");
        MessageUtil.sendMessage(sender, "&b/" + label + " template remove <name>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender.hasPermission("cubegen.admin") || sender.hasPermission("cubegen.give")) list.add("give");
            if (sender.hasPermission("cubegen.admin")) {
                list.add("template");
                list.add("reload");
            }
            list.add("list");
            list.add("unplace");
            return filter(list, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
            return filter(players, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(plugin.getTemplateManager().getTemplateIds()), args[2]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("template") || args[0].equalsIgnoreCase("tpl"))) {
            return filter(Arrays.asList("list", "create", "edit", "linkitem", "setsize", "setregen", "settype", "remove"), args[1]);
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("template") || args[0].equalsIgnoreCase("tpl"))) {
            String act = args[1].toLowerCase(Locale.ROOT);
            if (act.equals("edit") || act.equals("linkitem") || act.equals("setsize") || act.equals("setregen") || act.equals("settype") || act.equals("remove")) {
                return filter(new ArrayList<>(plugin.getTemplateManager().getTemplateIds()), args[2]);
            }
        }

        if (args.length == 4 && (args[0].equalsIgnoreCase("template") || args[0].equalsIgnoreCase("tpl")) && args[1].equalsIgnoreCase("settype")) {
            return filter(Arrays.asList("normal", "protect"), args[3]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("unplace") || args[0].equalsIgnoreCase("pickup"))) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                List<String> cubes = new ArrayList<>();
                for (PlacedCube c : plugin.getPlacedCubeManager().getPlayerCubes(p.getUniqueId())) {
                    cubes.add(c.getTemplateId());
                }
                return filter(cubes, args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        List<String> res = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                res.add(s);
            }
        }
        return res;
    }
}
