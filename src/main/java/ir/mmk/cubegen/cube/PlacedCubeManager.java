package ir.mmk.cubegen.cube;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.template.CubeTemplate;
import ir.mmk.cubegen.util.ItemUtil;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlacedCubeManager {

    private final CubeGen plugin;
    private final File placedFile;
    private YamlConfiguration placedConfig;
    private final Map<UUID, PlacedCube> placedCubes = new ConcurrentHashMap<>();

    public PlacedCubeManager(CubeGen plugin) {
        this.plugin = plugin;
        this.placedFile = new File(plugin.getDataFolder(), "placed_cubes.yml");
        load();
    }

    public void load() {
        placedCubes.clear();
        if (!placedFile.exists()) {
            return;
        }

        placedConfig = YamlConfiguration.loadConfiguration(placedFile);
        ConfigurationSection sec = placedConfig.getConfigurationSection("cubes");
        if (sec == null) return;

        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                ConfigurationSection cSec = sec.getConfigurationSection(idStr);
                if (cSec == null) continue;

                UUID ownerUUID = UUID.fromString(cSec.getString("ownerUUID"));
                String ownerName = cSec.getString("ownerName", "Unknown");
                String templateId = cSec.getString("templateId", "default");
                String worldName = cSec.getString("world");
                int x = cSec.getInt("x");
                int y = cSec.getInt("y");
                int z = cSec.getInt("z");
                int size = cSec.getInt("size", 5);
                String type = cSec.getString("type", "protect");
                int regen = cSec.getInt("regen", 10);

                String facingStr = cSec.getString("ladderFacing", null);
                BlockFace ladderFacing = null;
                if (facingStr != null) {
                    try {
                        ladderFacing = BlockFace.valueOf(facingStr);
                    } catch (Exception ignored) {}
                }
                int borderOffset = (size / 2) + 1;
                int ladderCornerX = cSec.getInt("ladderCornerX", x - borderOffset);
                int ladderCornerZ = cSec.getInt("ladderCornerZ", z - borderOffset);

                PlacedCube cube = new PlacedCube(id, ownerUUID, ownerName, templateId,
                        worldName, x, y, z, size, type, regen,
                        ladderFacing != null ? ladderFacing : BlockFace.NORTH,
                        ladderCornerX, ladderCornerZ);

                cube.setAutoMineEnabled(cSec.getBoolean("autoMine", false));
                cube.setStorageModeEnabled(cSec.getBoolean("storageMode", false));
                cube.setAutoSmeltEnabled(cSec.getBoolean("autoSmelt", false));

                // Load virtual storage
                ConfigurationSection storageSec = cSec.getConfigurationSection("storage");
                if (storageSec != null) {
                    for (String slotKey : storageSec.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotKey);
                            ItemStack item = storageSec.getItemStack(slotKey);
                            cube.setStorageSlot(slot, item);
                        } catch (Exception ignored) {}
                    }
                }

                placedCubes.put(id, cube);
                cube.startRegenTask(plugin);
                if (cube.isAutoMineEnabled()) {
                    cube.startAutoMineTask(plugin);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load placed cube: " + idStr + " (" + e.getMessage() + ")");
            }
        }

        plugin.getLogger().info("Loaded " + placedCubes.size() + " placed CubeGens.");
    }

    public void save() {
        if (placedConfig == null) {
            placedConfig = new YamlConfiguration();
        }

        placedConfig.set("cubes", null); // clear
        ConfigurationSection sec = placedConfig.createSection("cubes");

        for (PlacedCube cube : placedCubes.values()) {
            if (!cube.isActive()) continue;
            ConfigurationSection cSec = sec.createSection(cube.getId().toString());
            cSec.set("ownerUUID", cube.getOwnerUUID().toString());
            cSec.set("ownerName", cube.getOwnerName());
            cSec.set("templateId", cube.getTemplateId());
            cSec.set("world", cube.getWorldName());
            cSec.set("x", cube.getCenterX());
            cSec.set("y", cube.getCenterY());
            cSec.set("z", cube.getCenterZ());
            cSec.set("size", cube.getSize());
            cSec.set("type", cube.getType());
            cSec.set("regen", cube.getRegenInterval());
            cSec.set("autoMine", cube.isAutoMineEnabled());
            cSec.set("storageMode", cube.isStorageModeEnabled());
            cSec.set("autoSmelt", cube.isAutoSmeltEnabled());
            if (cube.getLadderFacing() != null) {
                cSec.set("ladderFacing", cube.getLadderFacing().name());
                cSec.set("ladderCornerX", cube.getLadderCornerX());
                cSec.set("ladderCornerZ", cube.getLadderCornerZ());
            }

            // Save storage
            ConfigurationSection storageSec = cSec.createSection("storage");
            ItemStack[] stored = cube.getStorageContents();
            for (int i = 0; i < stored.length; i++) {
                if (stored[i] != null && stored[i].getType() != Material.AIR) {
                    storageSec.set(String.valueOf(i), stored[i]);
                }
            }
        }

        try {
            placedConfig.save(placedFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save placed_cubes.yml!", e);
        }
    }

    public boolean placeCube(Player player, CubeTemplate template, Location center) {
        World world = center.getWorld();
        if (world == null) {
            MessageUtil.sendMessage(player, "&cCannot place cube: world is invalid!");
            return false;
        }

        int maxPerPlayer = plugin.getConfig().getInt("setting.max_placed_per_player", 10);
        if (maxPerPlayer > 0 && !player.hasPermission("cubegen.admin")) {
            long count = getPlayerCubes(player.getUniqueId()).size();
            if (count >= maxPerPlayer) {
                MessageUtil.sendMessage(player, "&cYou have reached the maximum allowed placed CubeGens (" + maxPerPlayer + ")!");
                return false;
            }
        }

        int size = template.getSize();
        int offset = size / 2;
        int frameExtent = "protect".equalsIgnoreCase(template.getType()) ? (offset + 1) : offset;

        // Check world bounds
        if (center.getBlockY() - frameExtent < world.getMinHeight() || center.getBlockY() + frameExtent > world.getMaxHeight()) {
            MessageUtil.sendMessage(player, "&cCannot place CubeGen: height is out of world bounds!");
            return false;
        }

        // Check if overlaps with another existing cube
        for (PlacedCube existing : placedCubes.values()) {
            if (existing.isLocationPartOfCube(center)) {
                MessageUtil.sendMessage(player, "&cCannot place CubeGen: overlaps with another CubeGen!");
                return false;
            }
        }

        // Determine ladder corner & facing closest to the player
        double pX = player.getLocation().getX();
        double pZ = player.getLocation().getZ();
        double cX = center.getX() + 0.5;
        double cZ = center.getZ() + 0.5;
        double dx = pX - cX;
        double dz = pZ - cZ;

        int cornerX = (dx < 0) ? (center.getBlockX() - frameExtent) : (center.getBlockX() + frameExtent);
        int cornerZ = (dz < 0) ? (center.getBlockZ() - frameExtent) : (center.getBlockZ() + frameExtent);

        BlockFace ladderFacing;
        if (Math.abs(dz) >= Math.abs(dx)) {
            ladderFacing = (dz < 0) ? BlockFace.NORTH : BlockFace.SOUTH;
        } else {
            ladderFacing = (dx < 0) ? BlockFace.WEST : BlockFace.EAST;
        }

        PlacedCube placedCube = new PlacedCube(
                UUID.randomUUID(),
                player.getUniqueId(),
                player.getName(),
                template.getId(),
                world.getName(),
                center.getBlockX(),
                center.getBlockY(),
                center.getBlockZ(),
                template.getSize(),
                template.getType(),
                template.getRegenInterval(),
                ladderFacing,
                cornerX,
                cornerZ
        );

        // Check obstruction in target area
        for (Location loc : placedCube.getAllLocations()) {
            Block b = loc.getBlock();
            if (b.getType() != Material.AIR && !b.isLiquid() && !ItemUtil.isReplaceable(b.getType())) {
                MessageUtil.sendMessage(player, "&cCannot place CubeGen: The area is obstructed by &e" + b.getType().name() + "&c! Please place in an open space.");
                return false;
            }
        }

        // Clear all replaceable plants/foliage/snow/liquids in the CubeGen footprint so it sits directly on solid ground
        for (Location loc : placedCube.getAllLocations()) {
            Block b = loc.getBlock();
            if (ItemUtil.isReplaceable(b.getType()) || b.isLiquid()) {
                Block above = b.getRelative(BlockFace.UP);
                if (ItemUtil.isReplaceable(above.getType())) {
                    above.setType(Material.AIR, false);
                }
                b.setType(Material.AIR, false);
            }
        }

        placedCubes.put(placedCube.getId(), placedCube);
        placedCube.spawn(plugin.getTemplateManager());
        placedCube.startRegenTask(plugin);
        save();

        MessageUtil.sendMessage(player, "&aSuccessfully placed &b" + template.getDisplayName() + "&a!");
        return true;
    }

    public boolean unplaceCube(PlacedCube cube, Player player, boolean refundItem) {
        if (cube == null) return false;

        if (player != null && !player.hasPermission("cubegen.admin")) {
            if (!cube.getOwnerUUID().equals(player.getUniqueId())) {
                MessageUtil.sendMessage(player, "&cYou cannot pick up this CubeGen! It belongs to &e" + cube.getOwnerName() + "&c!");
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
                return false;
            }
        }

        cube.unplace(player, refundItem, plugin.getTemplateManager());
        placedCubes.remove(cube.getId());
        save();

        if (player != null && player.isOnline()) {
            MessageUtil.sendMessage(player, "&aSuccessfully picked up your CubeGen &b(" + cube.getTemplateId() + ")&a!");
        }
        return true;
    }

    public PlacedCube getCubeAtLocation(Location loc) {
        if (loc == null) return null;
        for (PlacedCube cube : placedCubes.values()) {
            if (cube.isLocationPartOfCube(loc)) {
                return cube;
            }
        }
        return null;
    }

    public PlacedCube getCubeAtFrameLocation(Location loc) {
        if (loc == null) return null;
        for (PlacedCube cube : placedCubes.values()) {
            if (cube.isFrameBlock(loc) || cube.isLadderBlock(loc)) {
                return cube;
            }
        }
        return null;
    }

    public List<PlacedCube> getPlayerCubes(UUID ownerUUID) {
        List<PlacedCube> list = new ArrayList<>();
        for (PlacedCube cube : placedCubes.values()) {
            if (cube.getOwnerUUID().equals(ownerUUID)) {
                list.add(cube);
            }
        }
        return list;
    }

    public PlacedCube getCubeById(UUID id) {
        return placedCubes.get(id);
    }

    public PlacedCube findCubeForPlayer(Player player, String query) {
        List<PlacedCube> cubes = getPlayerCubes(player.getUniqueId());
        if (cubes.isEmpty()) return null;

        if (query == null || query.isEmpty()) {
            return cubes.get(0);
        }

        for (PlacedCube c : cubes) {
            if (c.getTemplateId().equalsIgnoreCase(query) || c.getId().toString().startsWith(query)) {
                return c;
            }
        }
        return null;
    }

    public void stopAll() {
        for (PlacedCube cube : placedCubes.values()) {
            cube.stopRegenTask();
            cube.stopAutoMineTask();
        }
    }
}
