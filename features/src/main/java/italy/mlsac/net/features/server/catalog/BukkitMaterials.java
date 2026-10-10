// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.menu.MaterialCatalog;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

public final class BukkitMaterials implements MaterialCatalog {

    private final Map<String, Optional<Material>> items = new ConcurrentHashMap<>();

    public Optional<Material> item(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return items.computeIfAbsent(name, BukkitMaterials::lookUp);
    }

    @Override
    public boolean isItem(String material) {
        return item(material).isPresent();
    }

    private static Optional<Material> lookUp(String name) {
        try {
            return Optional.ofNullable(Material.matchMaterial(name)).filter(Material::isItem);
        } catch (RuntimeException | LinkageError notOnThisServer) {
            return Optional.empty();
        }
    }
}
