package ir.mmk.cubegen.gui;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.cube.PlacedCube;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CubeManageGui implements Listener {

    public static final String GUI_TITLE_PREFIX = MessageUtil.colorize("&3CubeGen Control: &b");
    private static final Map<UUID, PlacedCube> openCubes = new ConcurrentHashMap<>();

    public static void open(Player player, PlacedCube cube) {
        if (player == null || cube == null) return;

        boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("cubegen.admin");
        if (!isOwner && !isAdmin) {
            MessageUtil.sendMessage(player, "&cYou cannot manage this CubeGen! It belongs to &e" + cube.getOwnerName() + "&c!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        openCubes.put(player.getUniqueId(), cube);

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + cube.getTemplateId());

        // Background filler
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 10: Info
        inv.setItem(10, createGuiItem(Material.BEACON, "&b&lCubeGen Info",
                "&7• Template: &f" + cube.getTemplateId(),
                "&7• Size: &f" + cube.getSize() + "x" + cube.getSize() + "x" + cube.getSize(),
                "&7• Type: &f" + cube.getType(),
                "&7• Auto-Regen: &f" + (cube.getRegenInterval() > 0 ? cube.getRegenInterval() + "s" : "Disabled"),
                "&7• Owner: &f" + cube.getOwnerName(),
                "&7• Location: &f" + cube.getCenterX() + ", " + cube.getCenterY() + ", " + cube.getCenterZ()));

        // Slot 12: Storage
        inv.setItem(12, createGuiItem(Material.CHEST, "&6&lCubeGen Storage",
                "&7Open the 54-slot Double Chest storage.",
                "&eStored Items: &f" + cube.getStoredItemCount() + " items",
                "",
                "&a▸ Click to open storage!"));

        // Slot 14: AutoMine
        boolean autoMine = cube.isAutoMineEnabled();
        inv.setItem(14, createGuiItem(Material.NETHERITE_PICKAXE, "&d&lAutoMine Automation",
                "&7Automatically mines blocks inside the cube",
                "&7one by one and deposits them into storage.",
                "",
                "&7Status: " + (autoMine ? "&a&lENABLED" : "&c&lDISABLED"),
                "",
                "&e▸ Click to toggle!"));

        // Slot 15: Auto Smelt
        boolean autoSmelt = cube.isAutoSmeltEnabled();
        inv.setItem(15, createGuiItem(Material.FURNACE, "&6&lAuto Smelt",
                "&7Automatically smelts all mined drops",
                "&7(e.g. Raw Iron into Iron Ingots)",
                "&7directly before depositing into Storage.",
                "",
                "&7Requires: &eAutoMine Active",
                "&7Status: " + (autoSmelt ? (autoMine ? "&a&lENABLED" : "&e&lPAUSED (AutoMine Off)") : "&c&lDISABLED"),
                "",
                "&e▸ Click to toggle!"));

        // Slot 16: Storage Mode (Manual Mining redirect)
        boolean storageMode = cube.isStorageModeEnabled();
        inv.setItem(16, createGuiItem(Material.HOPPER, "&e&lStorage Mode (Manual)",
                "&7When enabled, blocks you manually mine",
                "&7go into Storage instead of your inventory.",
                "",
                "&7Status: " + (storageMode ? "&a&lENABLED" : "&c&lDISABLED"),
                "",
                "&e▸ Click to toggle!"));

        // Slot 22: Pick Up
        inv.setItem(22, createGuiItem(Material.NETHER_STAR, "&a&lPick Up CubeGen",
                "&7Click here to collect this CubeGen.",
                "&7All placed blocks will be removed and",
                "&7the item will return to your inventory.",
                "&eStored items will be safely dropped!"));

        // Slot 26: Close
        inv.setItem(26, createGuiItem(Material.BARRIER, "&c&lClose Menu",
                "&7Click to close this window."));

        player.openInventory(inv);
    }

    private static ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            if (lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) list.add(MessageUtil.colorize(l));
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        PlacedCube cube = openCubes.get(player.getUniqueId());
        if (cube == null) {
            player.closeInventory();
            return;
        }

        boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("cubegen.admin");
        if (!isOwner && !isAdmin) {
            openCubes.remove(player.getUniqueId());
            player.closeInventory();
            MessageUtil.sendMessage(player, "&cYou cannot manage this CubeGen! It belongs to &e" + cube.getOwnerName() + "&c!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        CubeGen plugin = CubeGen.getPlugin();

        // 1. Storage Click (Slot 12)
        if (slot == 12) {
            if (!player.hasPermission("cubegen.storage") && !player.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(player, "&cYou do not have permission to use CubeGen Storage!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }
            player.closeInventory();
            CubeStorageGui.open(player, cube);
            return;
        }

        // 2. AutoMine Toggle (Slot 14)
        if (slot == 14) {
            if (!player.hasPermission("cubegen.automine") && !player.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(player, "&cYou do not have permission to use AutoMine!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }
            boolean newState = !cube.isAutoMineEnabled();
            if (newState && cube.isStorageFull()) {
                MessageUtil.sendMessage(player, "&cCannot enable AutoMine: CubeGen Storage is full!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }
            cube.setAutoMineEnabled(newState);
            if (newState) {
                cube.startAutoMineTask(plugin);
                MessageUtil.sendMessage(player, "&aAutoMine has been &2&lENABLED &afor this CubeGen!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            } else {
                cube.stopAutoMineTask();
                MessageUtil.sendMessage(player, "&eAutoMine has been &c&lDISABLED &efor this CubeGen.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
            plugin.getPlacedCubeManager().save();
            open(player, cube); // Refresh GUI
            return;
        }

        // 3. Auto Smelt Toggle (Slot 15)
        if (slot == 15) {
            if (!player.hasPermission("cubegen.autosmelt") && !player.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(player, "&cYou do not have permission to use Auto Smelt!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }

            if (!cube.isAutoMineEnabled()) {
                MessageUtil.sendMessage(player, "&cAuto Smelt can only be enabled when AutoMine is active!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                return;
            }

            boolean newState = !cube.isAutoSmeltEnabled();
            cube.setAutoSmeltEnabled(newState);
            if (newState) {
                MessageUtil.sendMessage(player, "&aAuto Smelt has been &2&lENABLED&a! Mined ores will be smelted.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            } else {
                MessageUtil.sendMessage(player, "&eAuto Smelt has been &c&lDISABLED&e.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
            plugin.getPlacedCubeManager().save();
            open(player, cube); // Refresh GUI
            return;
        }

        // 4. Storage Mode Toggle (Slot 16)
        if (slot == 16) {
            if (!player.hasPermission("cubegen.storage") && !player.hasPermission("cubegen.admin")) {
                MessageUtil.sendMessage(player, "&cYou do not have permission to use Storage Mode!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }
            boolean newState = !cube.isStorageModeEnabled();
            if (newState && cube.isStorageFull()) {
                MessageUtil.sendMessage(player, "&cCannot enable Storage Mode: CubeGen Storage is full!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
                return;
            }
            cube.setStorageModeEnabled(newState);
            if (newState) {
                MessageUtil.sendMessage(player, "&aStorage Mode &2&lENABLED&a! Mined items will go into CubeGen Storage.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            } else {
                MessageUtil.sendMessage(player, "&eStorage Mode &c&lDISABLED&e. Mined items will drop normally.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
            plugin.getPlacedCubeManager().save();
            open(player, cube); // Refresh GUI
            return;
        }

        // 4. Pick Up (Slot 22)
        if (slot == 22) {
            openCubes.remove(player.getUniqueId());
            player.closeInventory();
            plugin.getPlacedCubeManager().unplaceCube(cube, player, true);
            return;
        }

        // 5. Close (Slot 26)
        if (slot == 26) {
            openCubes.remove(player.getUniqueId());
            player.closeInventory();
        }
    }
}
