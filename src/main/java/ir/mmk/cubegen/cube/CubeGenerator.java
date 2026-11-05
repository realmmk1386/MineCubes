package ir.mmk.cubegen.cube;

import ir.mmk.cubegen.CubeGen;
import ir.mmk.cubegen.template.CubeTemplate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy compatibility wrapper for CubeGenerator.
 */
public class CubeGenerator {

    public Location origin;
    public Plugin plugin;
    public Integer cubeSize;
    public String cubeName;
    public List<Location> cubeBlocksLocations = new ArrayList<>();

    public CubeGenerator(Location origin, Integer cubeSize, Player player, Plugin plugin, String cubeName, String type) {
        this.origin = origin;
        this.cubeSize = cubeSize;
        this.plugin = plugin;
        this.cubeName = cubeName;

        CubeGen cg = CubeGen.getPlugin();
        if (type.equalsIgnoreCase("place") && player != null && origin != null) {
            CubeTemplate template = cg.getTemplateManager().getTemplate(cubeName);
            if (template == null) {
                template = cg.getTemplateManager().getTemplate("default");
            }
            if (template != null) {
                cg.getPlacedCubeManager().placeCube(player, template, origin);
            }
        } else if (type.equalsIgnoreCase("unplace") && player != null) {
            PlacedCube cube = cg.getPlacedCubeManager().findCubeForPlayer(player, cubeName);
            if (cube != null) {
                cg.getPlacedCubeManager().unplaceCube(cube, player, true);
            }
        }
    }
}
