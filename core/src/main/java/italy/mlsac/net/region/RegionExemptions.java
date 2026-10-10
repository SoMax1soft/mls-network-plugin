// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.region;

import italy.mlsac.net.config.detection.RegionEntry;
import italy.mlsac.net.config.detection.RegionSettings;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class RegionExemptions {

    static final long CACHE_MILLIS = 500L;

    private final Supplier<RegionSettings> settings;
    private final RegionLookup lookup;
    private final PluginLog log;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean missingWarned = new AtomicBoolean();

    public RegionExemptions(Supplier<RegionSettings> settings, RegionLookup lookup, PluginLog log) {
        this.settings = settings;
        this.lookup = lookup;
        this.log = log;
    }

    public boolean evaluate(UUID player, BlockPosition position, long clockMillis) {
        Cached cached = cache.get(player);
        if (cached != null && cached.position().equals(position) && clockMillis - cached.at() < CACHE_MILLIS) {
            return cached.exempt();
        }
        boolean exempt = decide(settings.get(), position);
        cache.put(player, new Cached(position, clockMillis, exempt));
        return exempt;
    }

    public boolean lastKnown(UUID player) {
        Cached cached = cache.get(player);
        return cached != null && cached.exempt();
    }

    public void forget(UUID player) {
        cache.remove(player);
    }

    public void clear() {
        cache.clear();
    }

    boolean decide(RegionSettings current, BlockPosition position) {
        if (!current.enabled() || current.entries().isEmpty()) {
            return false;
        }
        if (current.needsRegionLookup() && !lookup.available() && missingWarned.compareAndSet(false, true)) {
            log.warn(LogTopic.CONFIG, "WorldGuard not available, region checking disabled");
        }
        Optional<String> top = current.needsRegionLookup() && lookup.available()
                ? lookup.topRegion(position) : Optional.empty();
        for (RegionEntry entry : current.entries()) {
            if (entry.exempts(position.worldLabel(), top)) {
                return true;
            }
        }
        return false;
    }

    private record Cached(BlockPosition position, long at, boolean exempt) {
    }
}
