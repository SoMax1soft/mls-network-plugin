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
import italy.mlsac.net.pipeline.turn.PlayerTurn;
import italy.mlsac.net.pipeline.turn.TurnEpoch;
import italy.mlsac.net.pipeline.turn.TurnPart;
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
    private final TurnEpoch epoch;
    private final DetectionSwitches switches;

    public LiveStreams(DetectionSwitches switches, PlayerStanding standing, MonotonicClock clock,
            ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe, UltraWindowSink sink) {
        this(switches, standing, clock, samples, lens, probe, sink, new TurnEpoch());
    }

    public LiveStreams(DetectionSwitches switches, PlayerStanding standing, MonotonicClock clock,
            ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe, UltraWindowSink sink,
            TurnEpoch epoch) {
        this.epoch = epoch;
        this.switches = switches;
        this.gate = new StreamGate(switches, standing, clock);
        this.clock = clock;
        this.samples = samples;
        this.lens = lens;
        this.probe = probe;
        this.sink = sink;
    }

    public RowProducer rowsFor(UUID player) {
        return gate.passes(player, false) ? rows(player) : null;
    }

    public RowProducer rowsForOnline(UUID player, PlayerTurn turn) {
        if (!gate.passes(player, turn)) {
            return null;
        }
        LiveTracker tracker = (LiveTracker) turn.part(TurnPart.LIVE_TRACKER);
        if (tracker == null) {
            tracker = tracker(player);
            turn.keep(TurnPart.LIVE_TRACKER, tracker);
        }
        return tracker.rows();
    }

    private RowProducer rows(UUID player) {
        return tracker(player).rows();
    }

    private LiveTracker tracker(UUID player) {
        LiveTracker tracker = trackers.get(player);
        if (tracker == null) {
            tracker = new LiveTracker(player, clock.nanos(MonotonicPurpose.STREAM_START), samples, lens, probe, sink,
                    switches::wallChecksPerSecond);
            trackers.put(player, tracker);
        }
        return tracker;
    }

    public RowProducer existingRows(UUID player) {
        LiveTracker tracker = trackers.get(player);
        return tracker == null ? null : tracker.rows();
    }

    public void forget(UUID player) {
        trackers.remove(player);
        epoch.advance();
        gate.forget(player);
    }
}
