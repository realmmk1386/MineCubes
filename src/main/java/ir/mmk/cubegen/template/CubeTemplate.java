package ir.mmk.cubegen.template;

import ir.mmk.cubegen.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CubeTemplate {

    private final String id;
    private String displayName;
    private int size;
    private String type; // "normal" or "protect"
    private int regenInterval; // in seconds, 0 = disabled
    private final Map<Material, Double> blockChances = new LinkedHashMap<>();
    private ItemStack linkedItem;

    public CubeTemplate(String id) {
        this.id = id;
        this.displayName = id;
        this.size = 5;
        this.type = "protect";
        this.regenInterval = 10;
    }

    public CubeTemplate(String id, String displayName, int size, String type, int regenInterval) {
        this.id = id;
        this.displayName = displayName;
        this.size = size;
        this.type = type;
        this.regenInterval = regenInterval;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getType() {
        return type != null ? type : "normal";
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRegenInterval() {
        return regenInterval;
    }

    public void setRegenInterval(int regenInterval) {
        this.regenInterval = regenInterval;
    }

    public Map<Material, Double> getBlockChances() {
        return blockChances;
    }

    public void setBlockChance(Material material, double chance) {
        if (chance <= 0) {
            blockChances.remove(material);
        } else {
            blockChances.put(material, chance);
        }
    }

    public void clearBlocks() {
        blockChances.clear();
    }

    public Material getRandomMaterial() {
        if (blockChances.isEmpty()) {
            return Material.STONE;
        }

        double totalWeight = 0.0;
        for (double weight : blockChances.values()) {
            if (weight > 0) {
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0) {
            return Material.STONE;
        }

        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double current = 0.0;
        for (Map.Entry<Material, Double> entry : blockChances.entrySet()) {
            if (entry.getValue() <= 0) continue;
            current += entry.getValue();
            if (randomValue <= current) {
                return entry.getKey();
            }
        }

        return blockChances.keySet().iterator().next();
    }

    public ItemStack getLinkedItem() {
        if (linkedItem != null) {
            return ItemUtil.tagCubeGenItem(linkedItem, id);
        }
        return ItemUtil.createDefaultTemplateItem(id, getDisplayName(), size, getType(), regenInterval);
    }

    public ItemStack getRawLinkedItem() {
        return linkedItem;
    }

    public void setLinkedItem(ItemStack item) {
        if (item == null) {
            this.linkedItem = null;
        } else {
            this.linkedItem = item.clone();
            this.linkedItem.setAmount(1);
        }
    }
}
