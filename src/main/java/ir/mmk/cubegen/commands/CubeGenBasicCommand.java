package ir.mmk.cubegen.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import ir.mmk.cubegen.CubeGen;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CubeGenBasicCommand implements BasicCommand {

    private final CubeGen plugin;
    private CubeGenCommand executor;

    public CubeGenBasicCommand(CubeGen plugin) {
        this.plugin = plugin;
    }

    private CubeGenCommand getExecutor() {
        if (executor == null) {
            executor = new CubeGenCommand(plugin);
        }
        return executor;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        getExecutor().onCommand(sender, null, "minecubes", args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        List<String> list = getExecutor().onTabComplete(sender, null, "minecubes", args);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("minecubes.use") || sender.hasPermission("cubegen.use");
    }
}
