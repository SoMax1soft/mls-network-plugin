// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.compensation.CompensationBook;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.legacy.LegacyCollector;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.rows.RowProducer;
import italy.mlsac.net.pipeline.ultra.LiveStreams;
import java.util.UUID;

final class TickCycle {
    private final PlayerStates states;
    private final DetectionSwitches switches;
    private final ServerSamples samples;
    private final InputTaps taps;
    private final LiveStreams live;
    private final LegacyCollector legacy;
    private final CompensationBook compensation;
    private final PlayerTurns turns;

    TickCycle(PlayerStates states, DetectionSwitches switches, ServerSamples samples, InputTaps taps,
            LiveStreams live, LegacyCollector legacy, CompensationBook compensation, PlayerTurns turns) {
        this.states = states;
        this.switches = switches;
        this.samples = samples;
        this.taps = taps;
        this.live = live;
        this.legacy = legacy;
        this.compensation = compensation;
        this.turns = turns;
    }

    void run() {
        samples.beginCaptureBatch();
        try {
            for (UUID player : states.connectedPlayers()) {
                synchronized (turns.of(player)) {
                    tickOf(player);
                }
            }
        } finally {
            samples.endCaptureBatch();
        }
        for (UUID viewer : compensation.viewerIds()) {
            synchronized (turns.of(viewer)) {
                compensation.endViewerTick(viewer);
            }
        }
    }

    void runFor(UUID player) {
        tickOf(player);
        compensation.endViewerTick(player);
    }

    private void tickOf(UUID player) {
        if (taps.anyActive() || switches.ultraEnabled()) {
            sample(player);
        }
        legacy.gameTick(player);
    }

    private void sample(UUID player) {
        PlayerSample sample = states.sample(player);
        if (sample == null) {
            return;
        }
        PlayerSample previous = samples.capture(sample);
        samples.storePing(player, states.serverPing(player));
        boolean discontinuous = previous != null
                && (!previous.world().equals(sample.world()) || previous.alive() != sample.alive());
        InputTap tap = taps.tapOrNull(player);
        if (discontinuous) {
            if (tap != null) {
                tap.discontinuity();
            }
            RowProducer brokenRows = live.rowsFor(player);
            if (brokenRows != null) {
                brokenRows.discontinuity();
            }
        }
        if (tap != null) {
            tap.gameTick();
        }
        RowProducer liveRows = live.rowsFor(player);
        if (liveRows != null) {
            liveRows.gameTick();
        }
    }
}
