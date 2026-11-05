package ir.mmk.cubegen.cube;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CubeStorageHolder implements InventoryHolder {

    private final PlacedCube cube;
    private Inventory inventory;

    public CubeStorageHolder(PlacedCube cube) {
        this.cube = cube;
    }

    public PlacedCube getCube() {
        return cube;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
