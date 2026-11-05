package ir.mmk.cubegen.gui;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.template.CubeTemplate;
import ir.mmk.cubegen.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateBlockEditorGui implements Listener {

    public static final String GUI_TITLE_PREFIX = MessageUtil.colorize("&3MineCube Editor: &b");

    private static final Map<UUID, EditorSession> activeSessions = new ConcurrentHashMap<>();

    public static class EditorSession {
        public final UUID playerUUID;
        public final String templateId;
        public final Map<Integer, Double> slotChances = new HashMap<>();
        public ItemStack[] savedBlockContents = new ItemStack[36];
        public int pendingSlot = -1;
        public Material pendingMaterial = null;

        public EditorSession(UUID playerUUID, String templateId) {
            this.playerUUID = playerUUID;
            this.templateId = templateId;
        }

        public double calculateTotalPercentage() {
            double total = 0.0;
            for (int i = 0; i < 36; i++) {
                if (savedBlockContents[i] != null && savedBlockContents[i].getType() != Material.AIR && savedBlockContents[i].getType().isBlock()) {
                    total += slotChances.getOrDefault(i, 0.0);
                }
            }
            return Math.round(total * 100.0) / 100.0;
        }

        public void autoBalanceTo100() {
            int activeCount = 0;
            double currentTotal = 0.0;
            for (int i = 0; i < 36; i++) {
                if (savedBlockContents[i] != null && savedBlockContents[i].getType() != Material.AIR && savedBlockContents[i].getType().isBlock()) {
                    activeCount++;
                    currentTotal += slotChances.getOrDefault(i, 0.0);
                }
            }
            if (activeCount == 0) return;

            if (currentTotal <= 0.0) {
                double each = Math.round((100.0 / activeCount) * 10.0) / 10.0;
                for (int i = 0; i < 36; i++) {
                    if (savedBlockContents[i] != null && savedBlockContents[i].getType() != Material.AIR && savedBlockContents[i].getType().isBlock()) {
                        slotChances.put(i, each);
                    }
                }
            } else {
                double scale = 100.0 / currentTotal;
                for (int i = 0; i < 36; i++) {
                    if (savedBlockContents[i] != null && savedBlockContents[i].getType() != Material.AIR && savedBlockContents[i].getType().isBlock()) {
                        double oldVal = slotChances.getOrDefault(i, 0.0);
                        double newVal = Math.round((oldVal * scale) * 10.0) / 10.0;
                        slotChances.put(i, newVal);
                    }
                }
            }
        }
    }

    public static void open(Player player, String templateId, CubeGen plugin) {
        CubeTemplate template = plugin.getTemplateManager().getTemplate(templateId);
        if (template == null) {
            MessageUtil.sendMessage(player, "&cTemplate not found: " + templateId);
            return;
        }

        EditorSession session = activeSessions.computeIfAbsent(player.getUniqueId(), u -> {
            EditorSession s = new EditorSession(u, templateId);
            int slot = 0;
            for (Map.Entry<Material, Double> entry : template.getBlockChances().entrySet()) {
                if (slot >= 36) break;
                ItemStack item = new ItemStack(entry.getKey());
                s.savedBlockContents[slot] = item;
                s.slotChances.put(slot, entry.getValue());
                slot++;
            }
            return s;
        });

        openInventoryForSession(player, session);
    }

    private static void openInventoryForSession(Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE_PREFIX + session.templateId);

        // Put block items in slots 0-35
        for (int i = 0; i < 36; i++) {
            ItemStack item = session.savedBlockContents[i];
            if (item != null && item.getType() != Material.AIR) {
                double chance = session.slotChances.getOrDefault(i, 0.0);
                ItemStack display = formatBlockItem(item.clone(), chance);
                inv.setItem(i, display);
            }
        }

        // Fill row 5 with black glass panes
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        // Row 6 controls
        inv.setItem(45, createGuiItem(Material.BOOK, "&e&lInformation",
                "&7• Place blocks in slots 1-36 above.",
                "&7• &bShift + Click &7a block to set its chance in chat.",
                "&7• Total percentage cannot exceed &e100%&7.",
                "&7• Click &aSave &7when you are done."));

        double total = session.calculateTotalPercentage();

        // Slot 46: Percentage Summary
        if (total > 100.0) {
            inv.setItem(46, createGuiItem(Material.REDSTONE_BLOCK, "&c&lTotal: " + String.format("%.1f", total) + "%",
                    "&4&lERROR: Total exceeds 100%!",
                    "&cPlease reduce chances or click",
                    "&cAuto-Balance before saving."));
        } else if (total == 100.0) {
            inv.setItem(46, createGuiItem(Material.EMERALD, "&a&lTotal: 100.0%",
                    "&7All block chances sum to exactly 100%!",
                    "&aReady to save."));
        } else {
            inv.setItem(46, createGuiItem(Material.GOLD_INGOT, "&e&lTotal: " + String.format("%.1f", total) + "%",
                    "&7Unallocated percentage: &a" + String.format("%.1f", (100.0 - total)) + "%",
                    "&7Total can be up to 100%."));
        }

        // Slot 47: Auto-Balance Button
        inv.setItem(47, createGuiItem(Material.ANVIL, "&b&lAuto-Balance to 100%",
                "&7Automatically scales all block chances",
                "&7so their exact sum equals 100.0%!",
                "",
                "&e▸ Click to auto-balance!"));

        // Slot 48: Save Button
        if (total > 100.0) {
            inv.setItem(48, createGuiItem(Material.REDSTONE_BLOCK, "&c&lCannot Save (Total > 100%)",
                    "&cTotal is &e" + String.format("%.1f", total) + "%&c!",
                    "&cTotal must be <= 100% to save.",
                    "&cClick Auto-Balance or adjust chances first."));
        } else {
            inv.setItem(48, createGuiItem(Material.EMERALD_BLOCK, "&a&lSave & Apply",
                    "&7Total percentage: &f" + String.format("%.1f", total) + "% / 100%",
                    "&aClick to save blocks and chances to templates."));
        }

        inv.setItem(49, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        inv.setItem(50, createGuiItem(Material.BARRIER, "&c&lCancel",
                "&7Close without saving changes."));

        for (int i = 51; i < 54; i++) {
            inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(inv);
    }

    private static ItemStack formatBlockItem(ItemStack item, double chance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&bSpawn Chance: &e" + String.format("%.1f", chance) + "%"));
            lore.add(MessageUtil.colorize("&7Shift + Click to change chance via chat"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(MessageUtil.colorize(l));
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String viewTitle = event.getView().getTitle();
        if (!viewTitle.startsWith(GUI_TITLE_PREFIX)) return;

        EditorSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        int rawSlot = event.getRawSlot();

        // Control buttons in bottom rows (slots 36-53)
        if (rawSlot >= 36 && rawSlot < 54) {
            event.setCancelled(true);

            if (rawSlot == 47) { // Auto-Balance
                syncInventoryToSession(event.getInventory(), session);
                session.autoBalanceTo100();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                MessageUtil.sendMessage(player, "&aAll block chances have been auto-balanced to 100%!");
                openInventoryForSession(player, session);
                return;
            }

            if (rawSlot == 48) { // Save button
                syncInventoryToSession(event.getInventory(), session);
                double total = session.calculateTotalPercentage();
                if (total > 100.0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.0f);
                    MessageUtil.sendMessage(player, "&8[&b&lMine&f&lCubes&8] &cError: Total spawn percentage is &e" + String.format("%.1f", total) + "%&c, which exceeds 100%! Please adjust chances or click Auto-Balance before saving.");
                    openInventoryForSession(player, session);
                    return;
                }

                saveSession(player, session, event.getInventory());
                activeSessions.remove(player.getUniqueId());
                player.closeInventory();
            } else if (rawSlot == 50) { // Cancel button
                activeSessions.remove(player.getUniqueId());
                player.closeInventory();
                MessageUtil.sendMessage(player, "&cChanges cancelled.");
            }
            return;
        }

        // Interacting with block slots (0-35)
        if (rawSlot >= 0 && rawSlot < 36) {
            if (event.isShiftClick()) {
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() != Material.AIR && current.getType().isBlock()) {
                    event.setCancelled(true);

                    syncInventoryToSession(event.getInventory(), session);

                    session.pendingSlot = rawSlot;
                    session.pendingMaterial = current.getType();

                    player.closeInventory();

                    MessageUtil.sendMessage(player, "&8--------------------------------------------------");
                    MessageUtil.sendMessage(player, "&ePlease type the spawn chance for &b" + current.getType().name() + " &ein chat (e.g. 50 or 15.5):");
                    MessageUtil.sendMessage(player, "&7(Type &ccancel &7to abort)");
                    MessageUtil.sendMessage(player, "&8--------------------------------------------------");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String viewTitle = event.getView().getTitle();
        if (!viewTitle.startsWith(GUI_TITLE_PREFIX)) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 36 && slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditorSession session = activeSessions.get(player.getUniqueId());

        if (session != null && session.pendingSlot != -1) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                session.pendingSlot = -1;
                session.pendingMaterial = null;
                MessageUtil.sendMessage(player, "&cChance change cancelled. Re-opening editor...");
                CubeGen.getPlugin().getSchedulerAdapter().runTask(() -> {
                    openInventoryForSession(player, session);
                });
                return;
            }

            try {
                double chance = Double.parseDouble(message);
                if (chance < 0) {
                    MessageUtil.sendMessage(player, "&cChance cannot be negative! Please enter a valid number or type 'cancel':");
                    return;
                }

                session.slotChances.put(session.pendingSlot, Math.round(chance * 10.0) / 10.0);
                MessageUtil.sendMessage(player, "&aSet spawn chance for &b" + session.pendingMaterial.name() + " &ato &e" + chance + "%&a!");

                session.pendingSlot = -1;
                session.pendingMaterial = null;

                CubeGen.getPlugin().getSchedulerAdapter().runTask(() -> {
                    openInventoryForSession(player, session);
                });
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, "&cInvalid number format! Please enter a valid number (e.g. 50 or 25.5) or type 'cancel':");
            }
        }
    }

    private static void syncInventoryToSession(Inventory inv, EditorSession session) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && item.getType().isBlock()) {
                session.savedBlockContents[i] = new ItemStack(item.getType());
                if (!session.slotChances.containsKey(i)) {
                    double remaining = Math.max(0.0, 100.0 - session.calculateTotalPercentage());
                    session.slotChances.put(i, remaining);
                }
            } else {
                session.savedBlockContents[i] = null;
                session.slotChances.remove(i);
            }
        }
    }

    private static void saveSession(Player player, EditorSession session, Inventory inv) {
        syncInventoryToSession(inv, session);

        CubeGen plugin = CubeGen.getPlugin();
        CubeTemplate template = plugin.getTemplateManager().getTemplate(session.templateId);
        if (template == null) {
            MessageUtil.sendMessage(player, "&cTemplate no longer exists!");
            return;
        }

        template.clearBlocks();

        int blockCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack item = session.savedBlockContents[i];
            if (item != null && item.getType() != Material.AIR && item.getType().isBlock()) {
                double chance = session.slotChances.getOrDefault(i, 0.0);
                template.setBlockChance(item.getType(), chance);
                blockCount++;
            }
        }

        if (blockCount == 0) {
            template.setBlockChance(Material.STONE, 80.0);
            template.setBlockChance(Material.COBBLESTONE, 20.0);
        }

        plugin.getTemplateManager().save();
        MessageUtil.sendMessage(player, "&aSuccessfully saved &e" + blockCount + " &ablock types for template &b" + template.getId() + "&a! (Total: " + String.format("%.1f", session.calculateTotalPercentage()) + "%)");
    }
}
