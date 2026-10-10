// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.menu;

import italy.mlsac.net.features.server.catalog.BukkitMaterials;
import italy.mlsac.net.menu.ItemFace;
import italy.mlsac.net.menu.MenuFrame;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemFaces {

    static final String FALLBACK = "PAPER";

    private final BukkitMaterials materials;

    public ItemFaces(BukkitMaterials materials) {
        this.materials = materials;
    }

    public void paint(Inventory inventory, MenuFrame frame) {
        inventory.clear();
        for (Map.Entry<Integer, ItemFace> entry : frame.items().entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, stack(entry.getValue()));
            }
        }
    }

    @SuppressWarnings("deprecation")
    ItemStack stack(ItemFace face) {
        Material material = materials.item(face.material())
                .or(() -> materials.item(FALLBACK))
                .orElse(Material.PAPER);
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(face.name());
            if (!face.lore().isEmpty()) {
                meta.setLore(face.lore());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
