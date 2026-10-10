// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.banimation.EffectRegistry;
import italy.mlsac.net.platform.ServerVersionRules;
import java.util.Optional;
import org.bukkit.Particle;

public final class BukkitEffectRegistry implements EffectRegistry {

    private final NamedConstants<Particle> particles = new NamedConstants<>("org.bukkit.Particle", Particle.class);
    private final SoundBoard sounds;
    private final ServerVersionRules rules;

    public BukkitEffectRegistry(SoundBoard sounds, ServerVersionRules rules) {
        this.sounds = sounds;
        this.rules = rules;
    }

    public Optional<Particle> particle(String name) {
        return particles.find(name);
    }

    @Override
    public boolean hasParticle(String name) {
        return particle(name).isPresent();
    }

    @Override
    public boolean hasSound(String name) {
        return sounds.exists(name);
    }

    @Override
    public boolean modernParticleNames() {
        return rules.modernRegistryNames();
    }
}
