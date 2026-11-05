package ir.mmk.cubegen.data;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {
    private final CubeGen plugin;
    private final File folder;

    public PlayerDataManager(CubeGen plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "Player");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public File getPlayerFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    public YamlConfiguration getPlayerConfig(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void savePlayerConfig(UUID uuid, YamlConfiguration config) {
        try {
            config.save(getPlayerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player config for " + uuid, e);
        }
    }
}
