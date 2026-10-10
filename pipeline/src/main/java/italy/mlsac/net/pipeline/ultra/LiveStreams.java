// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.rows.RowProducer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LiveStreams {
    private final StreamGate gate;
    private final MonotonicClock clock;
    private final ServerSamples samples;
    private final ClientSightRewind lens;
    private final OcclusionProbe probe;
    private final UltraWindowSink sink;
    private final Map<UUID, LiveTracker> trackers = new ConcurrentHashMap<>();

    public LiveStreams(DetectionSwitches switches, PlayerStanding standing, MonotonicClock clock,
            ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe, UltraWindowSink sink) {
        this.gate = new StreamGate(switches, standing, clock);
        this.clock = clock;
        this.samples = samples;
        this.lens = lens;
        this.probe = probe;
        this.sink = sink;
    }

    public RowProducer rowsFor(UUID player) {
        if (!gate.passes(player)) {
            return null;
        }
        LiveTracker tracker = trackers.get(player);
        if (tracker == null) {
            tracker = new LiveTracker(player, clock.nanos(MonotonicPurpose.STREAM_START), samples, lens, probe, sink);
            trackers.put(player, tracker);
        }
        return tracker.rows();
    }

    public RowProducer existingRows(UUID player) {
        LiveTracker tracker = trackers.get(player);
        return tracker == null ? null : tracker.rows();
    }

    public void forget(UUID player) {
        trackers.remove(player);
        gate.forget(player);
    }
}
