package ir.mmk.cubegen.util;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemUtil {

    private static NamespacedKey templateKey;
    private static final Map<Material, Material> DIRECT_SMELT = new EnumMap<>(Material.class);
    private static final Map<Material, Material> RECIPE_CACHE = new ConcurrentHashMap<>();

    static {
        // Direct common smelting mappings for instant O(1) performance and multi-version compatibility
        registerSmelt("RAW_IRON", "IRON_INGOT");
        registerSmelt("RAW_COPPER", "COPPER_INGOT");
        registerSmelt("RAW_GOLD", "GOLD_INGOT");
        registerSmelt("IRON_ORE", "IRON_INGOT");
        registerSmelt("DEEPSLATE_IRON_ORE", "IRON_INGOT");
        registerSmelt("COPPER_ORE", "COPPER_INGOT");
        registerSmelt("DEEPSLATE_COPPER_ORE", "COPPER_INGOT");
        registerSmelt("GOLD_ORE", "GOLD_INGOT");
        registerSmelt("DEEPSLATE_GOLD_ORE", "GOLD_INGOT");
        registerSmelt("NETHER_GOLD_ORE", "GOLD_INGOT");
        registerSmelt("ANCIENT_DEBRIS", "NETHERITE_SCRAP");
        registerSmelt("COBBLESTONE", "STONE");
        registerSmelt("COBBLED_DEEPSLATE", "DEEPSLATE");
        registerSmelt("STONE", "SMOOTH_STONE");
        registerSmelt("SAND", "GLASS");
        registerSmelt("RED_SAND", "GLASS");
        registerSmelt("SANDSTONE", "SMOOTH_SANDSTONE");
        registerSmelt("RED_SANDSTONE", "SMOOTH_RED_SANDSTONE");
        registerSmelt("NETHERRACK", "NETHER_BRICK");
        registerSmelt("CLAY_BALL", "BRICK");
        registerSmelt("CLAY", "TERRACOTTA");
        registerSmelt("WET_SPONGE", "SPONGE");
        registerSmelt("CACTUS", "GREEN_DYE");
        registerSmelt("SEA_PICKLE", "LIME_DYE");
        registerSmelt("KELP", "DRIED_KELP");

        // Wood to Charcoal
        registerSmelt("OAK_LOG", "CHARCOAL");
        registerSmelt("SPRUCE_LOG", "CHARCOAL");
        registerSmelt("BIRCH_LOG", "CHARCOAL");
        registerSmelt("JUNGLE_LOG", "CHARCOAL");
        registerSmelt("ACACIA_LOG", "CHARCOAL");
        registerSmelt("DARK_OAK_LOG", "CHARCOAL");
        registerSmelt("MANGROVE_LOG", "CHARCOAL");
        registerSmelt("CHERRY_LOG", "CHARCOAL");
        registerSmelt("OAK_WOOD", "CHARCOAL");
        registerSmelt("SPRUCE_WOOD", "CHARCOAL");
        registerSmelt("BIRCH_WOOD", "CHARCOAL");
        registerSmelt("JUNGLE_WOOD", "CHARCOAL");
        registerSmelt("ACACIA_WOOD", "CHARCOAL");
        registerSmelt("DARK_OAK_WOOD", "CHARCOAL");
        registerSmelt("MANGROVE_WOOD", "CHARCOAL");
        registerSmelt("CHERRY_WOOD", "CHARCOAL");
    }

    private static void registerSmelt(String inputName, String outputName) {
        try {
            Material in = Material.matchMaterial(inputName);
            Material out = Material.matchMaterial(outputName);
            if (in != null && out != null) {
                DIRECT_SMELT.put(in, out);
            }
        } catch (Throwable ignored) {}
    }

    public static ItemStack getSmeltedResult(ItemStack raw) {
        if (raw == null || raw.getType() == Material.AIR) return raw;

        Material inputMat = raw.getType();
        Material direct = DIRECT_SMELT.get(inputMat);
        if (direct != null) {
            ItemStack res = raw.clone();
            res.setType(direct);
            return res;
        }

        Material cached = RECIPE_CACHE.get(inputMat);
        if (cached != null) {
            ItemStack res = raw.clone();
            res.setType(cached);
            return res;
        }

        // Dynamically query Bukkit recipe registry
        try {
            Iterator<Recipe> it = Bukkit.recipeIterator();
            while (it.hasNext()) {
                Recipe recipe = it.next();
                if (recipe instanceof FurnaceRecipe) {
                    FurnaceRecipe fr = (FurnaceRecipe) recipe;
                    if (fr.getInputChoice() != null && fr.getInputChoice().test(raw)) {
                        Material resultMat = fr.getResult().getType();
                        RECIPE_CACHE.put(inputMat, resultMat);
                        ItemStack res = raw.clone();
                        res.setType(resultMat);
                        return res;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return raw;
    }

    public static boolean isReplaceable(Material mat) {
        if (mat == null || mat.isAir()) return true;
        if (mat == Material.GRASS_BLOCK || mat == Material.SNOW_BLOCK || mat == Material.MOSS_BLOCK) {
            return false;
        }
        String name = mat.name();
        if (name.endsWith("_BLOCK")) return false;

        return name.equals("SHORT_GRASS")
                || name.equals("GRASS")
                || name.equals("TALL_GRASS")
                || name.equals("SEAGRASS")
                || name.equals("TALL_SEAGRASS")
                || name.contains("FERN")
                || name.contains("FLOWER")
                || name.contains("TULIP")
                || name.contains("SAPLING")
                || name.contains("VINE")
                || name.contains("ROOTS")
                || name.contains("FUNGUS")
                || name.contains("MUSHROOM")
                || name.contains("SPROUTS")
                || name.contains("PETALS")
                || name.contains("CARPET")
                || name.contains("CORAL_FAN")
                || name.equals("DEAD_BUSH")
                || name.equals("SNOW")
                || name.equals("DANDELION")
                || name.equals("POPPY")
                || name.equals("BLUE_ORCHID")
                || name.equals("ALLIUM")
                || name.equals("AZURE_BLUET")
                || name.equals("OXEYE_DAISY")
                || name.equals("CORNFLOWER")
                || name.equals("LILY_OF_THE_VALLEY")
                || name.equals("WITHER_ROSE")
                || name.equals("TORCHFLOWER")
                || name.equals("PITCHER_PLANT")
                || name.equals("SUNFLOWER")
                || name.equals("LILAC")
                || name.equals("ROSE_BUSH")
                || name.equals("PEONY")
                || name.equals("SUGAR_CANE")
                || name.equals("BAMBOO_SAPLING")
                || name.equals("SWEET_BERRY_BUSH")
                || name.equals("HANGING_ROOTS")
                || name.equals("SPORE_BLOSSOM")
                || name.equals("LILY_PAD");
    }

    public static void init(CubeGen plugin) {
        templateKey = new NamespacedKey(plugin, "cubegen_template");
    }

    public static NamespacedKey getTemplateKey() {
        return templateKey;
    }

    public static boolean isCubeGenItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(templateKey, PersistentDataType.STRING);
    }

    public static String getTemplateIdFromItem(ItemStack item) {
        if (!isCubeGenItem(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(templateKey, PersistentDataType.STRING);
    }

    public static ItemStack tagCubeGenItem(ItemStack original, String templateId) {
        if (original == null || original.getType() == Material.AIR) {
            original = new ItemStack(Material.BEACON);
        }
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(templateKey, PersistentDataType.STRING, templateId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createDefaultTemplateItem(String templateId, String displayName, int size, String type, int regen) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&b&l" + displayName));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Right-click on ground to place your CubeGen."));
            lore.add(MessageUtil.colorize("&8-------------------------"));
            lore.add(MessageUtil.colorize("&eTemplate: &f" + templateId));
            lore.add(MessageUtil.colorize("&eSize: &f" + size + "x" + size + "x" + size));
            lore.add(MessageUtil.colorize("&eType: &f" + type));
            lore.add(MessageUtil.colorize("&eAuto-Regen: &f" + (regen > 0 ? regen + "s" : "Disabled")));
            lore.add(MessageUtil.colorize("&8-------------------------"));
            lore.add(MessageUtil.colorize("&aShift + Right-Click frame to pick up!"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(templateKey, PersistentDataType.STRING, templateId);
            item.setItemMeta(meta);
        }
        return item;
    }
}
