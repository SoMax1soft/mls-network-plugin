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
import italy.mlsac.net.pipeline.turn.PlayerTurn;
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
        boolean ultra = switches.ultraEnabled();
        samples.beginCaptureBatch();
        try {
            for (UUID player : states.connectedPlayers()) {
                PlayerTurn turn = turns.of(player);
                synchronized (turn) {
                    tickOf(player, turn, ultra);
                }
            }
        } finally {
            samples.endCaptureBatch();
        }
        compensation.openProbeBurst();
        try {
            for (UUID viewer : compensation.viewerIds()) {
                PlayerTurn turn = turns.of(viewer);
                synchronized (turn) {
                    compensation.endViewerTick(viewer, turn);
                }
            }
        } finally {
            compensation.closeProbeBurst();
        }
    }

    void runFor(UUID player) {
        PlayerTurn turn = turns.of(player);
        synchronized (turn) {
            tickOf(player, turn, switches.ultraEnabled());
            compensation.endViewerTick(player, turn);
        }
    }

    private void tickOf(UUID player, PlayerTurn turn, boolean ultra) {
        boolean sampled = (taps.anyActive() || ultra) && sample(player, turn);
        legacy.gameTick(player, sampled, turn);
    }

    private boolean sample(UUID player, PlayerTurn turn) {
        PlayerSample sample = states.sample(player, taps.anyActive(), turn);
        if (sample == null) {
            return false;
        }
        PlayerSample previous = samples.capture(turn, sample, states.serverPingAfterSample(player));
        boolean discontinuous = previous != null
                && (!previous.world().equals(sample.world()) || previous.alive() != sample.alive());
        InputTap tap = taps.tapOrNull(player);
        if (discontinuous) {
            if (tap != null) {
                tap.discontinuity();
            }
            RowProducer brokenRows = live.rowsForOnline(player, turn);
            if (brokenRows != null) {
                brokenRows.discontinuity();
            }
        }
        if (tap != null) {
            tap.gameTick();
        }
        RowProducer liveRows = live.rowsForOnline(player, turn);
        if (liveRows != null) {
            liveRows.gameTick();
        }
        return true;
    }
}
