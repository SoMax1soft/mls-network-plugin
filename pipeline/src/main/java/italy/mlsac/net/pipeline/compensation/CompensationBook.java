// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.player.ClientVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CompensationBook {
    private static final double SEED_RANGE_SQUARED = 128.0 * 128.0;

    private final TransactionPings pings;
    private final MonotonicClock clock;
    private final ConcurrentHashMap<UUID, CompensatedViewer> viewers = new ConcurrentHashMap<>();

    public CompensationBook(TransactionPings pings, MonotonicClock clock) {
        this.pings = pings;
        this.clock = clock;
    }

    public void track(UUID viewer, ViewSeed seed) {
        if (seed == null) {
            return;
        }
        Optional<ClientVersion> version = ClientVersion.parse(seed.clientVersion());
        if (version.isPresent() && version.get().below(1, 17, 0)) {
            return;
        }
        CompensatedViewer tracked = new CompensatedViewer(ClientQuirks.of(version),
                new PingAcknowledgements(viewer, pings, clock));
        List<ViewSeed.SeenPlayer> others = seed.others() == null ? List.of() : seed.others();
        for (ViewSeed.SeenPlayer other : others) {
            if (other.player().equals(viewer) || seed.world() == null || !seed.world().equals(other.world())) {
                continue;
            }
            if (other.position().squaredDistanceTo(seed.position()) <= SEED_RANGE_SQUARED) {
                tracked.spawn(other.entityNumber(), other.position());
            }
        }
        viewers.put(viewer, tracked);
    }

    public void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    public Optional<CompensatedViewer> viewer(UUID viewer) {
        return Optional.ofNullable(viewers.get(viewer));
    }

    public CompensatedViewer viewerOrNull(UUID viewer) {
        return viewers.get(viewer);
    }

    public Iterable<UUID> viewerIds() {
        return viewers.keySet();
    }

    public void endServerTick() {
        for (CompensatedViewer viewer : viewers.values()) {
            probeIfRoom(viewer);
        }
    }

    public void endViewerTick(UUID viewer) {
        CompensatedViewer tracked = viewers.get(viewer);
        if (tracked != null) {
            probeIfRoom(tracked);
        }
    }

    private static void probeIfRoom(CompensatedViewer viewer) {
        if (viewer.acknowledgements().backlogSmall()) {
            viewer.acknowledgements().sendProbe();
        }
    }

    public void pingWritten(UUID viewer, int id) {
        CompensatedViewer tracked = viewers.get(viewer);
        if (tracked != null) {
            tracked.acknowledgements().probeFlushed(id);
        }
    }

    public boolean pong(UUID viewer, int id) {
        if (!PingAcknowledgements.ownsProbe(id)) {
            return false;
        }
        CompensatedViewer tracked = viewers.get(viewer);
        return tracked != null && tracked.acknowledgements().answered(id);
    }

    public void clientTick(UUID viewer) {
        CompensatedViewer tracked = viewers.get(viewer);
        if (tracked != null) {
            tracked.clientTick();
        }
    }
}
