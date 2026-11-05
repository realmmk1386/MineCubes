package ir.mmk.cubegen.listener;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.cube.PlacedCube;
import ir.mmk.cubegen.gui.CubeManageGui;
import ir.mmk.cubegen.template.CubeTemplate;
import ir.mmk.cubegen.util.ItemUtil;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CubeInteractListener implements Listener {

    private final CubeGen plugin;

    public CubeInteractListener(CubeGen plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        // 1. Placing CubeGen item
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && ItemUtil.isCubeGenItem(held)) {
            event.setCancelled(true);

            if (!player.hasPermission("cubegen.place") && !player.hasPermission("cubegen.use")) {
                MessageUtil.sendMessage(player, "&cYou do not have permission to place MineCubes!");
                return;
            }

            String templateId = ItemUtil.getTemplateIdFromItem(held);
            CubeTemplate template = plugin.getTemplateManager().getTemplate(templateId);

            if (template == null) {
                MessageUtil.sendMessage(player, "&cInvalid MineCube template: " + templateId);
                return;
            }

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            // Find the actual solid ground block underneath (if player clicked grass, flowers, snow, etc.)
            Block groundBlock = clickedBlock;
            while (groundBlock.getY() > groundBlock.getWorld().getMinHeight()
                    && (ItemUtil.isReplaceable(groundBlock.getType()) || !groundBlock.getType().isSolid() || groundBlock.isLiquid())) {
                groundBlock = groundBlock.getRelative(BlockFace.DOWN);
            }

            // Ensure the entire CubeGen is placed completely on top of the solid ground surface
            // so terrain and builds below are never replaced or destroyed!
            int offset = template.getSize() / 2;
            int frameExtent = "protect".equalsIgnoreCase(template.getType()) ? (offset + 1) : offset;

            // Lowest block of the structure (bedrock floor) will sit directly on groundBlock.getY() + 1
            int surfaceY = groundBlock.getY() + 1;
            int centerY = surfaceY + frameExtent;
            int centerX = groundBlock.getX();
            int centerZ = groundBlock.getZ();

            Location placeCenter = new Location(groundBlock.getWorld(), centerX, centerY, centerZ);

            boolean success = plugin.getPlacedCubeManager().placeCube(player, template, placeCenter);
            if (success) {
                held.setAmount(held.getAmount() - 1);
                // Safely teleport the player outside the newly placed CubeGen so they don't get trapped inside
                teleportPlayerSafelyOutside(player, placeCenter, frameExtent, groundBlock);
            }
            return;
        }

        // 2. Interacting with placed CubeGen (Bedrock frame or normal cube)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block clicked = event.getClickedBlock();
            PlacedCube cube = null;

            if (clicked.getType() == Material.BEDROCK) {
                cube = plugin.getPlacedCubeManager().getCubeAtFrameLocation(clicked.getLocation());
            } else if (player.isSneaking() || held.getType() == Material.AIR) {
                // If it's a normal cube (no bedrock) or sneaking interaction on an inner block
                PlacedCube potential = plugin.getPlacedCubeManager().getCubeAtLocation(clicked.getLocation());
                if (potential != null && "normal".equalsIgnoreCase(potential.getType())) {
                    cube = potential;
                }
            }

            if (cube != null) {
                event.setCancelled(true);

                // Strict Ownership / Admin check
                boolean isOwner = cube.getOwnerUUID().equals(player.getUniqueId());
                boolean isAdmin = player.hasPermission("cubegen.admin");

                if (!isOwner && !isAdmin) {
                    MessageUtil.sendMessage(player, "&cThis MineCube belongs to &e" + cube.getOwnerName() + "&c! Only the owner can access settings, storage, or pick it up.");
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                    return;
                }

                // Shift + Right-Click: Direct Pick Up
                if (player.isSneaking()) {
                    plugin.getPlacedCubeManager().unplaceCube(cube, player, true);
                } else {
                    // Normal Right-Click: Open Management GUI
                    CubeManageGui.open(player, cube);
                }
            }
        }
    }

    private void teleportPlayerSafelyOutside(Player player, Location placeCenter, int frameExtent, Block clickedBlock) {
        World world = placeCenter.getWorld();
        if (world == null) return;

        Location pLoc = player.getLocation();
        double centerX = placeCenter.getX() + 0.5;
        double centerZ = placeCenter.getZ() + 0.5;

        double dx = pLoc.getX() - centerX;
        double dz = pLoc.getZ() - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Direction vector away from center
        if (dist < 0.3) {
            Vector dir = pLoc.getDirection();
            dir.setY(0);
            if (dir.lengthSquared() < 0.01) {
                dx = 0;
                dz = 1;
            } else {
                dir.normalize().multiply(-1); // step backwards from gaze
                dx = dir.getX();
                dz = dir.getZ();
            }
        } else {
            dx /= dist;
            dz /= dist;
        }

        // Stand safely outside the bounding box with 1.8 blocks clearance
        double outsideDist = frameExtent + 1.8;
        double targetX = centerX + dx * outsideDist;
        double targetZ = centerZ + dz * outsideDist;

        // Find safe ground level near clickedBlock.getY() + 1
        int startY = clickedBlock.getY() + 1;
        int bestY = startY;
        boolean foundSafe = false;

        for (int dy = 0; dy <= 6; dy++) {
            int[] testYs = (dy == 0) ? new int[]{startY} : new int[]{startY + dy, startY - dy};
            for (int y : testYs) {
                if (y < world.getMinHeight() + 2 || y > world.getMaxHeight() - 2) continue;
                Block ground = world.getBlockAt((int) Math.floor(targetX), y - 1, (int) Math.floor(targetZ));
                Block feet = world.getBlockAt((int) Math.floor(targetX), y, (int) Math.floor(targetZ));
                Block head = world.getBlockAt((int) Math.floor(targetX), y + 1, (int) Math.floor(targetZ));

                if (ground.getType().isSolid() && !feet.getType().isSolid() && !head.getType().isSolid()
                        && !feet.isLiquid() && !ground.isLiquid()) {
                    bestY = y;
                    foundSafe = true;
                    break;
                }
            }
            if (foundSafe) break;
        }

        Location safeLoc = new Location(world, targetX, bestY, targetZ);
        Location lookTarget = placeCenter.clone().add(0.5, 0.5, 0.5);
        Vector lookDir = lookTarget.toVector().subtract(safeLoc.toVector());
        if (lookDir.lengthSquared() > 0.01) {
            safeLoc.setDirection(lookDir);
        }

        player.teleport(safeLoc);
        try {
            player.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
            world.spawnParticle(Particle.PORTAL, safeLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
        } catch (Throwable ignored) {}
    }
}
