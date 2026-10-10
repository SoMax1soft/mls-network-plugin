// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.DustColor;
import italy.mlsac.net.banimation.Emission;
import italy.mlsac.net.banimation.ParticleLook;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.features.server.catalog.BukkitEffectRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class ParticleBurst {

    static final double AUDIENCE_RADIUS = 64.0;
    static final double SMALLEST_DUST = 0.01;

    private final BukkitEffectRegistry registry;
    private final PluginLog log;
    private final Set<String> refused = ConcurrentHashMap.newKeySet();

    public ParticleBurst(BukkitEffectRegistry registry, PluginLog log) {
        this.registry = registry;
        this.log = log;
    }

    public void emit(World world, ParticleLook look, List<Emission> emissions, Point spread, double speed) {
        Optional<Particle> particle = registry.particle(look.particle());
        if (particle.isEmpty() || emissions.isEmpty()) {
            return;
        }
        Object data = look.dust().map(ParticleBurst::dustOptions).orElse(null);
        List<Player> watchers = world.getPlayers();
        double reach = AUDIENCE_RADIUS * AUDIENCE_RADIUS;
        for (Emission emission : emissions) {
            Point at = emission.at();
            Location point = new Location(world, at.x(), at.y(), at.z());
            for (Player watcher : watchers) {
                if (watcher.getLocation().distanceSquared(point) <= reach) {
                    send(watcher, particle.get(), at, emission.count(), spread, speed, data);
                }
            }
        }
    }

    private void send(Player watcher, Particle particle, Point at, int count, Point spread, double speed, Object data) {
        try {
            watcher.spawnParticle(particle, at.x(), at.y(), at.z(), count, spread.x(), spread.y(), spread.z(), speed,
                    data);
        } catch (IllegalArgumentException unsupported) {
            if (refused.add(particle.name())) {
                log.warn(LogTopic.PENALTY, "[Animation] Particle " + particle.name()
                        + " cannot be shown without extra data: " + unsupported.getMessage());
            }
        }
    }

    private static Particle.DustOptions dustOptions(DustColor dust) {
        return new Particle.DustOptions(Color.fromRGB(dust.red(), dust.green(), dust.blue()),
                (float) Math.max(SMALLEST_DUST, dust.size()));
    }
}
