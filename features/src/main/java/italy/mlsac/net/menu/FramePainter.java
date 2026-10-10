// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Map;

public final class FramePainter {

    static final String FALLBACK_MATERIAL = "PAPER";

    private final LegacyColorizer colors;
    private final MaterialCatalog materials;

    public FramePainter(LegacyColorizer colors, MaterialCatalog materials) {
        this.colors = colors;
        this.materials = materials;
    }

    public ItemFace item(String material, String name, List<String> lore, TemplateValues values) {
        return new ItemFace(material(material), colors.colorize(values.apply(name)),
                lore.stream().map(line -> colors.colorize(values.apply(line))).toList());
    }

    public String title(String raw, TemplateValues values) {
        return colors.colorize(values.apply(raw));
    }

    public void button(Map<Integer, ItemFace> items, MenuButton button, TemplateValues values) {
        items.put(button.slot(), item(button.material(), button.name(), button.lore(), values));
    }

    public void fill(Map<Integer, ItemFace> items, MenuBlueprint blueprint) {
        blueprint.fillerMaterial().ifPresent(filler -> {
            ItemFace face = new ItemFace(material(filler), ItemFace.FILLER_NAME, List.of());
            for (int slot = 0; slot < blueprint.size(); slot++) {
                if (!blueprint.listSlots().contains(slot)) {
                    items.putIfAbsent(slot, face);
                }
            }
        });
    }

    private String material(String requested) {
        return requested != null && materials.isItem(requested) ? requested : FALLBACK_MATERIAL;
    }
}
