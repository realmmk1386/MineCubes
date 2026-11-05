package ir.mmk.cubegen.cube;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.template.CubeTemplate;
import ir.mmk.cubegen.template.TemplateManager;
import ir.mmk.cubegen.util.MessageUtil;
import ir.mmk.cubegen.util.SchedulerAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import ir.mmk.cubegen.util.ItemUtil;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlacedCube {

    private final UUID id;
    private final UUID ownerUUID;
    private final String ownerName;
    private final String templateId;
    private final String worldName;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int size;
    private final String type;
    private int regenInterval;

    // Storage & Automation
    private final ItemStack[] storageItems = new ItemStack[54];
    private transient Inventory storageInventory;
    private boolean autoMineEnabled = false;
    private boolean storageModeEnabled = false;
    private boolean autoSmeltEnabled = false;

    private final Set<Location> innerLocations = new HashSet<>();
    private final Set<Location> frameLocations = new HashSet<>();
    private final Set<Location> ladderLocations = new HashSet<>();
    private BlockFace ladderFacing = BlockFace.NORTH;
    private int ladderCornerX;
    private int ladderCornerZ;

    private transient SchedulerAdapter.TaskHandle regenTaskHandle;
    private transient SchedulerAdapter.TaskHandle autoMineTaskHandle;
    private transient boolean active = true;

    public PlacedCube(UUID id, UUID ownerUUID, String ownerName, String templateId,
                      String worldName, int centerX, int centerY, int centerZ,
                      int size, String type, int regenInterval) {
        this(id, ownerUUID, ownerName, templateId, worldName, centerX, centerY, centerZ,
                size, type, regenInterval, BlockFace.NORTH,
                centerX - (size / 2 + 1), centerZ - (size / 2 + 1));
    }

    public PlacedCube(UUID id, UUID ownerUUID, String ownerName, String templateId,
                      String worldName, int centerX, int centerY, int centerZ,
                      int size, String type, int regenInterval,
                      BlockFace ladderFacing, int ladderCornerX, int ladderCornerZ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.templateId = templateId;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.size = size;
        this.type = type != null ? type : "protect";
        this.regenInterval = regenInterval;
        this.ladderFacing = (ladderFacing != null) ? ladderFacing : BlockFace.NORTH;
        this.ladderCornerX = ladderCornerX;
        this.ladderCornerZ = ladderCornerZ;

        calculateLocations();
    }

    public void calculateLocations() {
        innerLocations.clear();
        frameLocations.clear();
        ladderLocations.clear();

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location center = new Location(world, centerX, centerY, centerZ);
        int offset = size / 2;

        // Inner blocks
        for (int x = -offset; x <= offset; x++) {
            for (int y = -offset; y <= offset; y++) {
                for (int z = -offset; z <= offset; z++) {
                    innerLocations.add(center.clone().add(x, y, z));
                }
            }
        }

        // Frame blocks (wireframe edges of protective bounding box + solid bottom floor)
        if ("protect".equalsIgnoreCase(type)) {
            int borderOffset = offset + 1;

            // 1. Solid Bedrock floor at the bottom layer (y = -borderOffset)
            // This prevents any empty air gaps underneath the generator!
            for (int x = -borderOffset; x <= borderOffset; x++) {
                for (int z = -borderOffset; z <= borderOffset; z++) {
                    frameLocations.add(center.clone().add(x, -borderOffset, z));
                }
            }

            // 2. Pillars and top wireframe edges (y > -borderOffset)
            for (int x = -borderOffset; x <= borderOffset; x++) {
                for (int y = -borderOffset + 1; y <= borderOffset; y++) {
                    for (int z = -borderOffset; z <= borderOffset; z++) {
                        int count = 0;
                        if (Math.abs(x) == borderOffset) count++;
                        if (Math.abs(y) == borderOffset) count++;
                        if (Math.abs(z) == borderOffset) count++;

                        if (count >= 2) {
                            frameLocations.add(center.clone().add(x, y, z));
                        }
                    }
                }
            }

            // 3. Ladder blocks along one corner pillar from bottom to top
            if (ladderCornerX == 0 && ladderCornerZ == 0) {
                ladderCornerX = centerX - borderOffset;
                ladderCornerZ = centerZ - borderOffset;
                ladderFacing = BlockFace.NORTH;
            }

            int ladderX = ladderCornerX;
            int ladderZ = ladderCornerZ;

            if (ladderFacing == BlockFace.NORTH) {
                ladderZ = ladderCornerZ - 1;
            } else if (ladderFacing == BlockFace.SOUTH) {
                ladderZ = ladderCornerZ + 1;
            } else if (ladderFacing == BlockFace.WEST) {
                ladderX = ladderCornerX - 1;
            } else if (ladderFacing == BlockFace.EAST) {
                ladderX = ladderCornerX + 1;
            } else {
                ladderZ = ladderCornerZ - 1;
                ladderFacing = BlockFace.NORTH;
            }

            for (int y = -borderOffset; y <= borderOffset; y++) {
                ladderLocations.add(new Location(world, ladderX, centerY + y, ladderZ));
            }
        }
    }

    public Inventory getStorageInventory() {
        if (storageInventory == null) {
            CubeStorageHolder holder = new CubeStorageHolder(this);
            storageInventory = Bukkit.createInventory(holder, 54, MessageUtil.colorize("&3CubeGen Storage: &b" + templateId));
            holder.setInventory(storageInventory);
            for (int i = 0; i < storageItems.length; i++) {
                if (storageItems[i] != null) {
                    storageInventory.setItem(i, storageItems[i].clone());
                }
            }
        }
        return storageInventory;
    }

    public ItemStack[] getStorageContents() {
        return getStorageInventory().getContents();
    }

    public void setStorageSlot(int index, ItemStack item) {
        if (index >= 0 && index < 54) {
            storageItems[index] = item;
            if (storageInventory != null) {
                storageInventory.setItem(index, item);
            }
        }
    }

    public void spawn(TemplateManager tm) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        CubeTemplate template = tm.getTemplate(templateId);

        // 1. Place frame and solid bedrock floor
        if ("protect".equalsIgnoreCase(type)) {
            for (Location loc : frameLocations) {
                Block block = loc.getBlock();
                if (block.getType() != Material.BEDROCK) {
                    block.setType(Material.BEDROCK);
                }
            }

            // 2. Place ladders along corner pillar
            for (Location loc : ladderLocations) {
                Block block = loc.getBlock();
                block.setType(Material.LADDER, false);
                if (block.getBlockData() instanceof Ladder) {
                    Ladder ladderData = (Ladder) block.getBlockData();
                    ladderData.setFacing(ladderFacing);
                    block.setBlockData(ladderData, false);
                }
            }
        }

        // 2. Place inner blocks
        for (Location loc : innerLocations) {
            Block block = loc.getBlock();
            Material mat = (template != null) ? template.getRandomMaterial() : Material.STONE;
            block.setType(mat);
        }

        world.playSound(new Location(world, centerX, centerY, centerZ), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    public void regenerate(TemplateManager tm) {
        if (!active) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        CubeTemplate template = tm.getTemplate(templateId);
        if (template == null) return;

        int regeneratedCount = 0;
        for (Location loc : innerLocations) {
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                Material mat = template.getRandomMaterial();
                block.setType(mat);
                regeneratedCount++;
            }
        }

        if (regeneratedCount > 0) {
            Location center = new Location(world, centerX, centerY, centerZ);
            try {
                world.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5);
            } catch (Throwable ignored) {}
        }
    }

    public void unplace(Player player, boolean refundItem, TemplateManager tm) {
        this.active = false;
        stopRegenTask();
        stopAutoMineTask();

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            int borderOffset = (size / 2) + 1;

            // 1. Remove all ladders FIRST with applyPhysics = false
            // This prevents them from dropping as items when bedrock support is removed!
            for (Location loc : ladderLocations) {
                Block block = loc.getBlock();
                if (block.getType() == Material.LADDER) {
                    block.setType(Material.AIR, false);
                }
            }

            // Also scan all 4 corner pillars and their adjacent blocks for any remaining ladders
            int[] dxs = {-borderOffset, borderOffset};
            int[] dzs = {-borderOffset, borderOffset};
            BlockFace[] checkFaces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (int cx : dxs) {
                for (int cz : dzs) {
                    for (int y = -borderOffset; y <= borderOffset; y++) {
                        Block pillarBlock = world.getBlockAt(centerX + cx, centerY + y, centerZ + cz);
                        for (BlockFace face : checkFaces) {
                            Block adj = pillarBlock.getRelative(face);
                            if (adj.getType() == Material.LADDER) {
                                adj.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            }

            // 2. Remove inner blocks with applyPhysics = false
            for (Location loc : innerLocations) {
                Block block = loc.getBlock();
                block.setType(Material.AIR, false);
            }

            // 3. Remove frame blocks & floor with applyPhysics = false
            for (Location loc : frameLocations) {
                Block block = loc.getBlock();
                if (block.getType() == Material.BEDROCK) {
                    block.setType(Material.AIR, false);
                }
            }

            // 4. Remove any dropped ladder Item entities within the bounding box
            Location center = new Location(world, centerX, centerY, centerZ);
            try {
                for (Entity entity : world.getNearbyEntities(center, borderOffset + 2, borderOffset + 2, borderOffset + 2)) {
                    if (entity instanceof Item) {
                        Item item = (Item) entity;
                        if (item.getItemStack().getType() == Material.LADDER) {
                            item.remove();
                        }
                    }
                }
            } catch (Throwable ignored) {}

            world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
        }

        // Return storage contents safely to the player so no loot is lost
        if (player != null && player.isOnline()) {
            boolean droppedAny = false;
            Inventory inv = getStorageInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    inv.setItem(i, null);
                    droppedAny = true;
                }
            }
            if (droppedAny) {
                MessageUtil.sendMessage(player, "&eAll items from CubeGen Storage were dropped at your feet.");
            }
        }

        // Refund CubeGen item
        if (refundItem && player != null && player.isOnline()) {
            CubeTemplate template = tm.getTemplate(templateId);
            ItemStack item = (template != null)
                    ? template.getLinkedItem()
                    : new ItemStack(Material.BEACON);

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                MessageUtil.sendMessage(player, "&eYour inventory was full! The CubeGen item was dropped at your feet.");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    public void startRegenTask(CubeGen plugin) {
        stopRegenTask();

        if (regenInterval <= 0) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Location center = new Location(world, centerX, centerY, centerZ);

        long ticks = regenInterval * 20L;
        regenTaskHandle = plugin.getSchedulerAdapter().runTimerAtLocation(center, () -> {
            regenerate(plugin.getTemplateManager());
        }, ticks, ticks);
    }

    public void stopRegenTask() {
        if (regenTaskHandle != null) {
            regenTaskHandle.cancel();
            regenTaskHandle = null;
        }
    }

    public void startAutoMineTask(CubeGen plugin) {
        stopAutoMineTask();

        if (!autoMineEnabled) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Location center = new Location(world, centerX, centerY, centerZ);

        long interval = plugin.getConfig().getLong("setting.automine.interval_ticks", 20L);
        if (interval < 5) interval = 5;

        autoMineTaskHandle = plugin.getSchedulerAdapter().runTimerAtLocation(center, () -> {
            tickAutoMine(plugin);
        }, interval, interval);
    }

    public void stopAutoMineTask() {
        if (autoMineTaskHandle != null) {
            autoMineTaskHandle.cancel();
            autoMineTaskHandle = null;
        }
    }

    public void handleStorageFull(CubeGen plugin) {
        boolean wasAutoMine = autoMineEnabled;
        boolean wasStorageMode = storageModeEnabled;

        if (wasAutoMine) {
            autoMineEnabled = false;
            stopAutoMineTask();
        }
        if (wasStorageMode) {
            storageModeEnabled = false;
        }

        if (wasAutoMine || wasStorageMode) {
            plugin.getPlacedCubeManager().save();

            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && owner.isOnline()) {
                MessageUtil.sendMessage(owner, "&6[CubeGen] &cStorage for your &b" + templateId + " &cCubeGen is &4&lFULL&c!");
                if (wasAutoMine) {
                    MessageUtil.sendMessage(owner, "&6[CubeGen] &eAutoMine has been &c&lDISABLED &eautomatically.");
                }
                if (wasStorageMode) {
                    MessageUtil.sendMessage(owner, "&6[CubeGen] &eStorage Mode has been &c&lDISABLED &eautomatically.");
                }
                owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
    }

    public void tickAutoMine(CubeGen plugin) {
        if (!active || !autoMineEnabled) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        if (isStorageFull()) {
            handleStorageFull(plugin);
            return;
        }

        // Find one solid inner block to mine
        for (Location loc : innerLocations) {
            Block block = loc.getBlock();
            if (block.getType() != Material.AIR && !block.getType().isAir()) {
                ItemStack tool = new ItemStack(Material.NETHERITE_PICKAXE);
                Collection<ItemStack> drops = block.getDrops(tool);
                if (drops.isEmpty()) {
                    drops = Collections.singletonList(new ItemStack(block.getType()));
                }

                boolean added = true;
                for (ItemStack drop : drops) {
                    if (autoSmeltEnabled && autoMineEnabled) {
                        drop = ItemUtil.getSmeltedResult(drop);
                    }
                    if (!addItemToStorage(drop)) {
                        added = false;
                        break;
                    }
                }

                if (!added || isStorageFull()) {
                    handleStorageFull(plugin);
                    return;
                }

                // Mine effect
                try {
                    world.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.7f, 1.0f);
                    world.spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, block.getBlockData());
                } catch (Throwable ignored) {}

                block.setType(Material.AIR);
                plugin.getPlacedCubeManager().save();
                return; // Mine 1 block per cycle
            }
        }
    }

    public boolean addItemToStorage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return true;
        Inventory inv = getStorageInventory();
        HashMap<Integer, ItemStack> overflow = inv.addItem(item);
        return overflow.isEmpty();
    }

    public boolean isStorageFull() {
        Inventory inv = getStorageInventory();
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) return false;
            if (item.getAmount() < item.getMaxStackSize()) return false;
        }
        return true;
    }

    public int getStoredItemCount() {
        int count = 0;
        Inventory inv = getStorageInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public boolean isLocationPartOfCube(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        int lx = loc.getBlockX();
        int ly = loc.getBlockY();
        int lz = loc.getBlockZ();

        for (Location l : innerLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        for (Location l : frameLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        for (Location l : ladderLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        return false;
    }

    public boolean isInnerBlock(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        int lx = loc.getBlockX();
        int ly = loc.getBlockY();
        int lz = loc.getBlockZ();

        for (Location l : innerLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        return false;
    }

    public boolean isFrameBlock(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        int lx = loc.getBlockX();
        int ly = loc.getBlockY();
        int lz = loc.getBlockZ();

        for (Location l : frameLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        return false;
    }

    public boolean isLadderBlock(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        int lx = loc.getBlockX();
        int ly = loc.getBlockY();
        int lz = loc.getBlockZ();

        for (Location l : ladderLocations) {
            if (l.getBlockX() == lx && l.getBlockY() == ly && l.getBlockZ() == lz) return true;
        }
        return false;
    }

    public Set<Location> getAllLocations() {
        Set<Location> all = new HashSet<>(innerLocations);
        all.addAll(frameLocations);
        all.addAll(ladderLocations);
        return all;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public String getTemplateId() { return templateId; }
    public String getWorldName() { return worldName; }
    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public int getCenterZ() { return centerZ; }
    public int getSize() { return size; }
    public String getType() { return type; }
    public int getRegenInterval() { return regenInterval; }
    public void setRegenInterval(int interval) { this.regenInterval = interval; }
    public Set<Location> getInnerLocations() { return innerLocations; }
    public Set<Location> getFrameLocations() { return frameLocations; }
    public Set<Location> getLadderLocations() { return ladderLocations; }
    public BlockFace getLadderFacing() { return ladderFacing; }
    public int getLadderCornerX() { return ladderCornerX; }
    public int getLadderCornerZ() { return ladderCornerZ; }
    public boolean isActive() { return active; }

    public boolean isAutoMineEnabled() { return autoMineEnabled; }
    public void setAutoMineEnabled(boolean autoMineEnabled) { this.autoMineEnabled = autoMineEnabled; }

    public boolean isStorageModeEnabled() { return storageModeEnabled; }
    public void setStorageModeEnabled(boolean storageModeEnabled) { this.storageModeEnabled = storageModeEnabled; }

    public boolean isAutoSmeltEnabled() { return autoSmeltEnabled; }
    public void setAutoSmeltEnabled(boolean autoSmeltEnabled) { this.autoSmeltEnabled = autoSmeltEnabled; }
}
