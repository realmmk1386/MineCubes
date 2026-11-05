package ir.mmk.cubegen.template;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class TemplateManager {

    private final CubeGen plugin;
    private final File templateFile;
    private YamlConfiguration templateConfig;
    private final Map<String, CubeTemplate> templates = new LinkedHashMap<>();

    public TemplateManager(CubeGen plugin) {
        this.plugin = plugin;
        this.templateFile = new File(plugin.getDataFolder(), "cubegens.yml");
        load();
    }

    public void load() {
        templates.clear();
        if (!templateFile.exists()) {
            createDefaultConfigFile();
        }

        templateConfig = YamlConfiguration.loadConfiguration(templateFile);
        ConfigurationSection templatesSection = templateConfig.getConfigurationSection("templates");

        if (templatesSection == null) {
            templatesSection = templateConfig.createSection("templates");
        }

        for (String id : templatesSection.getKeys(false)) {
            ConfigurationSection sec = templatesSection.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("name", id);
            int size = sec.getInt("size", 5);
            String type = sec.getString("type", "protect");
            int regen = sec.getInt("regen", 10);

            CubeTemplate template = new CubeTemplate(id, name, size, type, regen);

            // Load blocks
            ConfigurationSection blocksSec = sec.getConfigurationSection("blocks");
            if (blocksSec != null) {
                for (String matKey : blocksSec.getKeys(false)) {
                    Material mat = Material.matchMaterial(matKey);
                    if (mat != null && mat.isBlock()) {
                        double chance = blocksSec.getDouble(matKey, 100.0);
                        template.setBlockChance(mat, chance);
                    }
                }
            }

            // Load linked item if present
            if (sec.contains("item")) {
                ItemStack item = sec.getItemStack("item");
                template.setLinkedItem(item);
            }

            templates.put(id.toLowerCase(Locale.ROOT), template);
        }

        plugin.getLogger().info("Loaded " + templates.size() + " MineCubes templates from cubegens.yml");
    }

    public void save() {
        if (templateConfig == null) {
            templateConfig = new YamlConfiguration();
        }

        templateConfig.set("templates", null); // clear
        ConfigurationSection templatesSection = templateConfig.createSection("templates");

        for (CubeTemplate template : templates.values()) {
            ConfigurationSection sec = templatesSection.createSection(template.getId());
            sec.set("name", template.getDisplayName());
            sec.set("size", template.getSize());
            sec.set("type", template.getType());
            sec.set("regen", template.getRegenInterval());

            ConfigurationSection blocksSec = sec.createSection("blocks");
            for (Map.Entry<Material, Double> entry : template.getBlockChances().entrySet()) {
                blocksSec.set(entry.getKey().name(), entry.getValue());
            }

            if (template.getRawLinkedItem() != null) {
                sec.set("item", template.getRawLinkedItem());
            }
        }

        try {
            templateConfig.save(templateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save cubegens.yml!", e);
        }
    }

    public CubeTemplate getTemplate(String id) {
        if (id == null) return null;
        return templates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<CubeTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Set<String> getTemplateIds() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    public boolean createTemplate(String id, int size, String type, int regen) {
        String key = id.toLowerCase(Locale.ROOT);
        if (templates.containsKey(key)) {
            return false;
        }

        CubeTemplate template = new CubeTemplate(id, id, size, type, regen);
        template.setBlockChance(Material.STONE, 80.0);
        template.setBlockChance(Material.COBBLESTONE, 20.0);
        templates.put(key, template);
        save();
        return true;
    }

    public boolean deleteTemplate(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (templates.remove(key) != null) {
            save();
            return true;
        }
        return false;
    }

    private void createDefaultConfigFile() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            YamlConfiguration config = new YamlConfiguration();

            // Default template
            config.set("templates.default.name", "Default MineCube");
            config.set("templates.default.size", 5);
            config.set("templates.default.type", "protect");
            config.set("templates.default.regen", 10);
            config.set("templates.default.blocks.STONE", 70.0);
            config.set("templates.default.blocks.COBBLESTONE", 20.0);
            config.set("templates.default.blocks.COAL_ORE", 7.0);
            config.set("templates.default.blocks.IRON_ORE", 3.0);

            // Diamond template
            config.set("templates.diamond.name", "Diamond MineCube");
            config.set("templates.diamond.size", 5);
            config.set("templates.diamond.type", "protect");
            config.set("templates.diamond.regen", 8);
            config.set("templates.diamond.blocks.STONE", 50.0);
            config.set("templates.diamond.blocks.DEEPSLATE", 30.0);
            config.set("templates.diamond.blocks.DIAMOND_ORE", 15.0);
            config.set("templates.diamond.blocks.DEEPSLATE_DIAMOND_ORE", 5.0);

            config.save(templateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create default cubegens.yml!", e);
        }
    }
}
