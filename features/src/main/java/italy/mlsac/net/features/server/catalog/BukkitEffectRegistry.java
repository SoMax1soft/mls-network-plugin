// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.banimation.ActorKind;
import italy.mlsac.net.banimation.EffectRegistry;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.platform.ServerVersionRules;
import java.util.Optional;
import org.bukkit.Particle;

public final class BukkitEffectRegistry implements EffectRegistry {

    static final GameVersion WARDEN_FROM = GameVersion.of(1, 19, 0);

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

    @Override
    public boolean hasActor(ActorKind kind) {
        return rules.server().atLeast(WARDEN_FROM);
    }
}
