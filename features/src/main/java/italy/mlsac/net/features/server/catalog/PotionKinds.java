// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.platform.RegistryNameAliases;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.potion.PotionEffectType;

public final class PotionKinds {

    public static final String SLOWNESS = "SLOWNESS";
    public static final String JUMP_BOOST = "JUMP_BOOST";
    public static final String LEVITATION = "LEVITATION";

    private final RegistryNameAliases aliases;
    private final Map<String, Optional<PotionEffectType>> resolved = new ConcurrentHashMap<>();

    public PotionKinds(RegistryNameAliases aliases) {
        this.aliases = aliases;
    }

    public Optional<PotionEffectType> find(String modernName) {
        return resolved.computeIfAbsent(modernName, this::lookUp);
    }

    @SuppressWarnings("deprecation")
    private Optional<PotionEffectType> lookUp(String modernName) {
        for (String candidate : aliases.candidates(modernName)) {
            try {
                PotionEffectType type = PotionEffectType.getByName(candidate);
                if (type != null) {
                    return Optional.of(type);
                }
            } catch (RuntimeException | LinkageError notOnThisServer) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
