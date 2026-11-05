package ir.mmk.cubegen.gui;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.entity.Player;

/**
 * Legacy compatibility forwarder to modern TemplateBlockEditorGui.
 */
public class CubeSetting {

    public CubeSetting(Player player, String cubeName) {
        if (player != null && cubeName != null) {
            TemplateBlockEditorGui.open(player, cubeName, CubeGen.getPlugin());
        }
    }
}
