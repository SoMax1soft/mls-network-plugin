// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.compensation.CompensatedViewer;
import italy.mlsac.net.pipeline.compensation.CompensationBook;
import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import italy.mlsac.net.pipeline.legacy.LegacyCollector;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.player.ClientVersion;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.player.VersionNames;
import italy.mlsac.net.pipeline.rows.RowProducer;
import italy.mlsac.net.pipeline.ultra.LiveStreams;
import java.util.Optional;
import java.util.UUID;

public final class DataPipeline implements PipelineInput {
    private final PipelinePorts ports;
    private final PlayerTurns turns;
    private final VersionNames versions = new VersionNames();
    private final ServerSamples samples = new ServerSamples();
    private final CompensationBook compensation;
    private final MovementPrefilter prefilter;
    private final InputTaps taps;
    private final LiveStreams live;
    private final LegacyCollector legacy;
    private final TickCycle tickCycle;

    public DataPipeline(PipelinePorts ports) {
        this(ports, new PlayerTurns(), InputTapSource.NONE);
    }

    public DataPipeline(PipelinePorts ports, InputTapSource tapSource) {
        this(ports, new PlayerTurns(), tapSource);
    }

    public DataPipeline(PipelinePorts ports, PlayerTurns turns) {
        this(ports, turns, InputTapSource.NONE);
    }

    public DataPipeline(PipelinePorts ports, PlayerTurns turns, InputTapSource tapSource) {
        this.ports = ports;
        this.turns = turns;
        this.compensation = new CompensationBook(ports.pings(), ports.monotonic());
        ClientSightRewind lens = new ClientSightRewind(compensation, samples);
        OcclusionProbe probe = new OcclusionProbe(ports.rays());
        this.prefilter = new MovementPrefilter(ports.connections(), versions);
        this.taps = tapSource.open(new TapParts(samples, lens, probe, ports, turns));
        this.live = new LiveStreams(ports.switches(), ports.standing(), ports.monotonic(), samples, lens, probe,
                ports.ultraWindows());
        this.legacy = new LegacyCollector(ports.switches(), ports.backend(), ports.standing(), ports.legacyWindows());
        this.tickCycle = new TickCycle(ports.states(), ports.switches(), samples, taps, live, legacy,
                compensation, turns);
    }

    public PlayerTurns turns() {
        return turns;
    }

    @Override
    public void enabled() {
        for (UUID player : ports.states().connectedPlayers()) {
            synchronized (turns.of(player)) {
                compensation.track(player, ports.connections().viewSeed(player));
            }
        }
    }

    @Override
    public void disabled() {
        taps.closeAll();
        if (!taps.anyActive()) {
            samples.clearSamplesAndHistories();
        }
        legacy.forgetAll();
    }

    @Override
    public void gameTick(long tick) {
        tickCycle.run();
    }

    @Override
    public void playerTick(UUID player, long tick) {
        tickCycle.runFor(player);
    }

    @Override
    public void movement(MovementPacket packet) {
        if (!packet.finite() || !prefilter.accepts(packet)) {
            return;
        }
        UUID player = packet.player();
        compensation.clientTick(player);
        if (movementNeeded(player)) {
            InputTap tap = taps.tapOrNull(player);
            if (tap != null) {
                tap.movement(packet);
            }
            RowProducer rows = live.rowsFor(player);
            if (rows != null) {
                rows.movement(packet);
            }
        }
        if (packet.carriesRotation()) {
            legacy.rotation(player, packet.yaw(), packet.pitch());
        }
    }

    @Override
    public void clientTickEnd(TickEndPacket packet) {
        Optional<ClientVersion> version = versions.parse(packet.clientVersion());
        if (version.isEmpty() || !version.get().atLeast(1, 21, 2)) {
            return;
        }
        UUID player = packet.player();
        if (!movementNeeded(player)) {
            return;
        }
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.noMoveTick(packet.nanos());
        }
        RowProducer rows = live.rowsFor(player);
        if (rows != null) {
            rows.noMoveTick(packet.nanos());
        }
    }

    @Override
    public void teleportConfirmed(UUID player) {
        if (!movementNeeded(player)) {
            return;
        }
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.teleportConfirmed();
        }
        RowProducer rows = live.rowsFor(player);
        if (rows != null) {
            rows.teleportConfirmed();
        }
    }

    @Override
    public void swing(SwingPacket packet) {
        UUID player = packet.player();
        if (!packet.mainHand() || !movementNeeded(player)) {
            return;
        }
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.swing(packet.nanos());
        }
        RowProducer rows = live.rowsFor(player);
        if (rows != null) {
            rows.swing(packet.nanos());
        }
    }

    @Override
    public void slotChanged(UUID player) {
        if (!movementNeeded(player)) {
            return;
        }
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.slotChanged();
        }
        RowProducer rows = live.rowsFor(player);
        if (rows != null) {
            rows.slotChanged();
        }
    }

    private boolean movementNeeded(UUID player) {
        return ports.switches().ultraEnabled() || taps.tapOrNull(player) != null;
    }

    @Override
    public void attack(AttackPacket packet) {
        UUID player = packet.player();
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.attack(packet);
        }
        RowProducer rows = live.rowsFor(player);
        if (rows != null) {
            rows.attack(packet);
        }
        boolean playerTarget = ports.connections().playerByEntity(packet.entityNumber()) != null;
        DebugTally.count(playerTarget ? "attack.on-player" : "attack.on-non-player");
        DebugTally.trace(player, () -> (playerTarget ? "hit a player" : "hit a non-player entity")
                + (rows == null ? ", no Ultra stream" : ", Ultra stream open"));
        if (playerTarget) {
            legacy.attackedPlayer(player);
        }
    }

    @Override
    public boolean pong(UUID player, int transactionId) {
        return compensation.pong(player, transactionId);
    }

    @Override
    public void pingWritten(UUID viewer, int transactionId) {
        compensation.pingWritten(viewer, transactionId);
    }

    @Override
    public void entitySpawned(UUID viewer, int entityNumber, Vec3 position) {
        CompensatedViewer tracked = compensation.viewerOrNull(viewer);
        if (tracked != null) {
            tracked.spawn(entityNumber, position);
        }
    }

    @Override
    public void entityMoved(UUID viewer, EntityMove move) {
        CompensatedViewer tracked = compensation.viewerOrNull(viewer);
        if (tracked != null) {
            tracked.move(move);
        }
    }

    @Override
    public void entitiesDestroyed(UUID viewer, int[] entityNumbers) {
        CompensatedViewer tracked = compensation.viewerOrNull(viewer);
        if (tracked != null) {
            for (int entityNumber : entityNumbers) {
                tracked.destroy(entityNumber);
            }
        }
    }

    @Override
    public void viewReset(UUID viewer) {
        CompensatedViewer tracked = compensation.viewerOrNull(viewer);
        if (tracked != null) {
            tracked.reset();
        }
    }

    @Override
    public void joined(UUID player) {
        compensation.track(player, ports.connections().viewSeed(player));
    }

    @Override
    public void quit(UUID player) {
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.discontinuity();
        }
        taps.playerLeft(player);
        samples.forget(player);
        live.forget(player);
        prefilter.forget(player);
        compensation.forget(player);
        legacy.forget(player);
    }

    @Override
    public void died(UUID player) {
        lifeBreak(player);
    }

    @Override
    public void respawned(UUID player) {
        lifeBreak(player);
    }

    @Override
    public void worldSwitched(UUID player) {
        lifeBreak(player);
    }

    private void lifeBreak(UUID player) {
        samples.removeSample(player);
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.discontinuity();
        }
        RowProducer rows = live.existingRows(player);
        if (rows != null) {
            rows.discontinuity();
        }
    }

    @Override
    public void teleported(UUID player) {
        samples.removeSample(player);
        RowProducer rows = live.existingRows(player);
        if (rows != null) {
            rows.serverTeleported();
        }
        InputTap tap = taps.tapOrNull(player);
        if (tap != null) {
            tap.sampleDiscontinuity();
        }
        legacy.teleported(player);
    }

    public boolean inLegacyCombat(UUID player) {
        return legacy.inFight(player);
    }
}
