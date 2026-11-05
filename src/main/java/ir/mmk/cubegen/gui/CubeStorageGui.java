package ir.mmk.cubegen.gui;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.cube.CubeStorageHolder;
import ir.mmk.cubegen.cube.PlacedCube;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class CubeStorageGui implements Listener {

    public static void open(Player player, PlacedCube cube) {
        if (player == null || cube == null) return;

        boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("cubegen.admin");
        if (!isOwner && !isAdmin) {
            MessageUtil.sendMessage(player, "&cYou cannot open this storage! This CubeGen belongs to &e" + cube.getOwnerName() + "&c!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        player.openInventory(cube.getStorageInventory());
    }

    @EventHandler
    public void onStorageClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof CubeStorageHolder) {
            CubeStorageHolder holder = (CubeStorageHolder) inv.getHolder();
            PlacedCube cube = holder.getCube();
            if (cube != null) {
                boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
                boolean isAdmin = player.hasPermission("cubegen.admin");
                if (!isOwner && !isAdmin) {
                    event.setCancelled(true);
                    player.closeInventory();
                    MessageUtil.sendMessage(player, "&cYou cannot access this storage! This CubeGen belongs to &e" + cube.getOwnerName() + "&c!");
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof CubeStorageHolder) {
            CubeStorageHolder holder = (CubeStorageHolder) inv.getHolder();
            PlacedCube cube = holder.getCube();

            CubeGen plugin = CubeGen.getPlugin();
            plugin.getPlacedCubeManager().save();

            // When closing storage with ESC, seamlessly return to CubeManageGui!
            if (cube != null && cube.isActive() && player.isOnline()) {
                boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
                boolean isAdmin = player.hasPermission("cubegen.admin");
                if (isOwner || isAdmin) {
                    plugin.getSchedulerAdapter().runTask(() -> {
                        if (player.isOnline() && cube.isActive()) {
                            CubeManageGui.open(player, cube);
                        }
                    });
                }
            }
        }
    }
}
