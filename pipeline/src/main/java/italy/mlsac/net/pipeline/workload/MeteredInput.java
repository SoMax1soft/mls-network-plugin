// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.workload;

import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.workload.WorkloadCounters;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public final class MeteredInput implements PipelineInput {

    private final PipelineInput inner;
    private final WorkloadCounters counters;
    private final LongSupplier nanos;
    private final IntSupplier tickedPlayers;

    public MeteredInput(PipelineInput inner, WorkloadCounters counters, LongSupplier nanos, IntSupplier tickedPlayers) {
        this.inner = inner;
        this.counters = counters;
        this.nanos = nanos;
        this.tickedPlayers = tickedPlayers;
    }

    @Override
    public void enabled() {
        inner.enabled();
    }

    @Override
    public void disabled() {
        inner.disabled();
    }

    @Override
    public void gameTick(long tick) {
        DebugTally.count("tick.server");
        long began = nanos.getAsLong();
        inner.gameTick(tick);
        counters.pipelineWork(nanos.getAsLong() - began, tickedPlayers.getAsInt());
    }

    @Override
    public void playerTick(UUID player, long tick) {
        DebugTally.count("tick.player");
        long began = nanos.getAsLong();
        inner.playerTick(player, tick);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void movement(MovementPacket packet) {
        DebugTally.count(packet.carriesRotation() ? "in.move+rotation" : "in.move");
        long began = nanos.getAsLong();
        inner.movement(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void clientTickEnd(TickEndPacket packet) {
        DebugTally.count("in.tick-end");
        long began = nanos.getAsLong();
        inner.clientTickEnd(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void teleportConfirmed(UUID player) {
        DebugTally.count("in.teleport-confirm");
        inner.teleportConfirmed(player);
    }

    @Override
    public void swing(SwingPacket packet) {
        DebugTally.count("in.swing");
        long began = nanos.getAsLong();
        inner.swing(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void slotChanged(UUID player) {
        DebugTally.count("in.slot");
        inner.slotChanged(player);
    }

    @Override
    public void attack(AttackPacket packet) {
        DebugTally.count("in.attack");
        DebugTally.trace(packet.player(), () -> "attack packet, entity #" + packet.entityNumber() + (packet.cancelled() ? " (cancelled by another plugin)" : ""));
        long began = nanos.getAsLong();
        inner.attack(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public boolean pong(UUID player, int transactionId) {
        DebugTally.count("in.pong");
        return inner.pong(player, transactionId);
    }

    @Override
    public void pingWritten(UUID viewer, int transactionId) {
        DebugTally.count("out.transaction-ping");
        inner.pingWritten(viewer, transactionId);
    }

    @Override
    public void entitySpawned(UUID viewer, int entityNumber, Vec3 position) {
        DebugTally.count("out.player-spawn");
        inner.entitySpawned(viewer, entityNumber, position);
    }

    @Override
    public void entityMoved(UUID viewer, EntityMove move) {
        DebugTally.count("out.entity-move");
        inner.entityMoved(viewer, move);
    }

    @Override
    public void entitiesDestroyed(UUID viewer, int[] entityNumbers) {
        DebugTally.count("out.entity-destroy");
        inner.entitiesDestroyed(viewer, entityNumbers);
    }

    @Override
    public void viewReset(UUID viewer) {
        DebugTally.count("out.view-reset");
        inner.viewReset(viewer);
    }

    @Override
    public void joined(UUID player) {
        DebugTally.count("player.join");
        inner.joined(player);
    }

    @Override
    public void quit(UUID player) {
        DebugTally.count("player.quit");
        inner.quit(player);
    }

    @Override
    public void died(UUID player) {
        inner.died(player);
    }

    @Override
    public void respawned(UUID player) {
        inner.respawned(player);
    }

    @Override
    public void worldSwitched(UUID player) {
        inner.worldSwitched(player);
    }

    @Override
    public void teleported(UUID player) {
        inner.teleported(player);
    }
}
