// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.region.BlockPosition;
import italy.mlsac.net.region.RegionExemptions;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class RegionPatrol implements PlayerTickWork {

    static final int SPACING_TICKS = 20;
    static final String CHECK_PURPOSE = "region.check";
    private static final int SEEN_SLOTS = 512;

    private final RegionExemptions regions;
    private final Clock clock;
    private final Set<UUID> watched = ConcurrentHashMap.newKeySet();
    private final Player[] seen = new Player[SEEN_SLOTS];

    public RegionPatrol(RegionExemptions regions, Clock clock) {
        this.regions = regions;
        this.clock = clock;
    }

    @Override
    public void perform(Player player, long tick) {
        UUID id = player.getUniqueId();
        int slot = System.identityHashCode(player) & (SEEN_SLOTS - 1);
        boolean firstSight = seen[slot] != player && !watched.contains(id) && watched.add(id);
        seen[slot] = player;
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
        Arrays.fill(seen, null);
    }
}
