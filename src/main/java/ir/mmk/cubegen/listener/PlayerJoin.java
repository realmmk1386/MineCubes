package ir.mmk.cubegen.listener;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.data.PlayerDataManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {

    private final CubeGen plugin;

    public PlayerJoin(CubeGen plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        YamlConfiguration data = playerDataManager.getPlayerConfig(uuid);

        if (!data.contains("username") || !player.getName().equals(data.getString("username"))) {
            data.set("username", player.getName());
            playerDataManager.savePlayerConfig(uuid, data);
        }
    }
}
