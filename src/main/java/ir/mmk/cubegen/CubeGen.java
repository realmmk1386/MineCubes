package ir.mmk.cubegen;

import ir.mmk.cubegen.commands.CubeGenCommand;
import ir.mmk.cubegen.cube.PlacedCubeManager;
import ir.mmk.cubegen.data.PlayerDataManager;
import ir.mmk.cubegen.gui.CubeManageGui;
import ir.mmk.cubegen.gui.TemplateBlockEditorGui;
import ir.mmk.cubegen.listener.CubeBlockListener;
import ir.mmk.cubegen.listener.CubeInteractListener;
import ir.mmk.cubegen.listener.PlayerJoin;
import ir.mmk.cubegen.template.TemplateManager;
import ir.mmk.cubegen.util.ItemUtil;
import ir.mmk.cubegen.util.MessageUtil;
import ir.mmk.cubegen.util.SchedulerAdapter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CubeGen extends JavaPlugin {

    private static CubeGen plugin;

    private SchedulerAdapter schedulerAdapter;
    private PlayerDataManager playerDataManager;
    private TemplateManager templateManager;
    private PlacedCubeManager placedCubeManager;

    @Override
    public void onLoad() {
        plugin = this;
        // Register commands early for Paper Lifecycle
        ir.mmk.cubegen.commands.CommandManager.register(this);
    }

    @Override
    public void onEnable() {
        plugin = this;

        // 1. Initialize utilities & schedulers
        this.schedulerAdapter = new SchedulerAdapter(this);
        ItemUtil.init(this);

        // 2. Load configurations
        saveDefaultConfig();
        reloadConfig();

        // 3. Initialize managers
        this.playerDataManager = new PlayerDataManager(this);
        this.templateManager = new TemplateManager(this);
        this.placedCubeManager = new PlacedCubeManager(this);

        // 4. Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new CubeInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new CubeBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new TemplateBlockEditorGui(), this);
        getServer().getPluginManager().registerEvents(new CubeManageGui(), this);
        getServer().getPluginManager().registerEvents(new ir.mmk.cubegen.gui.CubeStorageGui(), this);

        // 5. Register commands (if not already registered during onLoad)
        ir.mmk.cubegen.commands.CommandManager.register(this);

        MessageUtil.sendMessage(getServer().getConsoleSender(), "&8[&b&lMine&e&lCubes&8] &aPlugin v2.1.0 enabled successfully! (Paper / Folia / Spigot 26.2)");
    }

    @Override
    public void onDisable() {
        if (placedCubeManager != null) {
            placedCubeManager.stopAll();
            placedCubeManager.save();
        }
        if (templateManager != null) {
            templateManager.save();
        }

        MessageUtil.sendMessage(getServer().getConsoleSender(), "&8[&b&lMine&e&lCubes&8] &cPlugin has been disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (templateManager != null) {
            templateManager.load();
        }
        if (placedCubeManager != null) {
            placedCubeManager.stopAll();
            placedCubeManager.load();
        }
    }

    public static CubeGen getPlugin() {
        return plugin;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public PlacedCubeManager getPlacedCubeManager() {
        return placedCubeManager;
    }
}
