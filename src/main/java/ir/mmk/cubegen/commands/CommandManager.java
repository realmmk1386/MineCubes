package ir.mmk.cubegen.commands;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.command.PluginCommand;

import java.util.List;

public class CommandManager {

    private static boolean registered = false;

    public static void register(CubeGen plugin) {
        if (registered) return;

        // 1. Try modern Paper 26.x registerCommand (BasicCommand & LifecycleEvents)
        try {
            Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
            CubeGenBasicCommand basicCmd = new CubeGenBasicCommand(plugin);
            plugin.registerCommand("minecubes", "Main MineCubes command", List.of("minecube", "mc", "cubegen", "cg"), basicCmd);
            registered = true;
            return;
        } catch (Throwable ignored) {
            // Not running on modern Paper, or registerCommand not supported yet
        }

        // 2. Try standard Bukkit / Spigot fallback
        try {
            PluginCommand cmd = plugin.getCommand("minecubes");
            if (cmd == null) {
                cmd = plugin.getCommand("cubeGen");
            }
            if (cmd != null) {
                CubeGenCommand cmdExecutor = new CubeGenCommand(plugin);
                cmd.setExecutor(cmdExecutor);
                cmd.setTabCompleter(cmdExecutor);
                registered = true;
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isRegistered() {
        return registered;
    }
}
