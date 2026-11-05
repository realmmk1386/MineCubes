package ir.mmk.cubegen.listener;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.cube.PlacedCube;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CubeBlockListener implements Listener {

    private final CubeGen plugin;

    public CubeBlockListener(CubeGen plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // 1. Check if broken block is a bedrock frame block or ladder
        PlacedCube cube = plugin.getPlacedCubeManager().getCubeAtFrameLocation(block.getLocation());
        if (cube != null) {
            event.setCancelled(true);
            if (cube.isLadderBlock(block.getLocation())) {
                MessageUtil.sendMessage(player, "&cYou cannot break the CubeGen ladder! Use &eShift + Right-Click &con the bedrock frame to pick it up.");
            } else {
                MessageUtil.sendMessage(player, "&cYou cannot break the CubeGen frame! Use &eShift + Right-Click &cto pick it up.");
            }
            return;
        }

        // 2. Inner block broken
        PlacedCube innerCube = plugin.getPlacedCubeManager().getCubeAtLocation(block.getLocation());
        if (innerCube != null && innerCube.isInnerBlock(block.getLocation())) {
            boolean isOwner = innerCube.getOwnerUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission("cubegen.admin");

            if (!isOwner && !isAdmin) {
                boolean allowOthersToMine = plugin.getConfig().getBoolean("setting.allow_others_to_mine", true);
                if (!allowOthersToMine) {
                    event.setCancelled(true);
                    MessageUtil.sendMessage(player, "&cYou cannot mine inside another player's CubeGen!");
                    return;
                }
            }

            // If Storage Mode is enabled, redirect drops to CubeGen Storage
            if (innerCube.isStorageModeEnabled()) {
                if (innerCube.isStorageFull()) {
                    innerCube.handleStorageFull(plugin);
                    return; // Drops will drop naturally to player because event.setDropItems(false) is not called
                }

                event.setDropItems(false);
                Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
                if (drops.isEmpty()) {
                    drops = Collections.singletonList(new ItemStack(block.getType()));
                }
                boolean overflow = false;
                for (ItemStack drop : drops) {
                    if (!innerCube.addItemToStorage(drop)) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        overflow = true;
                    }
                }
                if (overflow || innerCube.isStorageFull()) {
                    innerCube.handleStorageFull(plugin);
                } else {
                    plugin.getPlacedCubeManager().save();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectBlocksFromExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protectBlocksFromExplosion(event.blockList());
    }

    private void protectBlocksFromExplosion(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (plugin.getPlacedCubeManager().getCubeAtFrameLocation(b.getLocation()) != null) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.getPlacedCubeManager().getCubeAtLocation(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (plugin.getPlacedCubeManager().getCubeAtLocation(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
