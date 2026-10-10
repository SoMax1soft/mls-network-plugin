// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.region.BlockPosition;
import italy.mlsac.net.region.RegionExemptions;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class RegionPatrol implements PlayerTickWork {

    static final int SPACING_TICKS = 4;
    static final String CHECK_PURPOSE = "region.check";

    private final RegionExemptions regions;
    private final Clock clock;
    private final Set<UUID> watched = ConcurrentHashMap.newKeySet();

    public RegionPatrol(RegionExemptions regions, Clock clock) {
        this.regions = regions;
        this.clock = clock;
    }

    @Override
    public void perform(Player player, long tick) {
        UUID id = player.getUniqueId();
        boolean firstSight = watched.add(id);
        if (!firstSight && Math.floorMod(id.hashCode() + tick, SPACING_TICKS) != 0) {
            return;
        }
        Location at = player.getLocation();
        World world = player.getWorld();
        regions.evaluate(id, new BlockPosition(world.getUID(), world.getName(), at.getBlockX(), at.getBlockY(),
                at.getBlockZ()), clock.wallMillis(CHECK_PURPOSE));
    }

    @Override
    public void forget(UUID player) {
        watched.remove(player);
    }
}
