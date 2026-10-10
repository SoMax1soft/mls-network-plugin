// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.workload;

import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
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
        long began = nanos.getAsLong();
        inner.gameTick(tick);
        counters.pipelineWork(nanos.getAsLong() - began, tickedPlayers.getAsInt());
    }

    @Override
    public void playerTick(UUID player, long tick) {
        long began = nanos.getAsLong();
        inner.playerTick(player, tick);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void movement(MovementPacket packet) {
        long began = nanos.getAsLong();
        inner.movement(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void clientTickEnd(TickEndPacket packet) {
        long began = nanos.getAsLong();
        inner.clientTickEnd(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void teleportConfirmed(UUID player) {
        inner.teleportConfirmed(player);
    }

    @Override
    public void swing(SwingPacket packet) {
        long began = nanos.getAsLong();
        inner.swing(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public void slotChanged(UUID player) {
        inner.slotChanged(player);
    }

    @Override
    public void attack(AttackPacket packet) {
        long began = nanos.getAsLong();
        inner.attack(packet);
        counters.pipelineWork(nanos.getAsLong() - began, 1);
    }

    @Override
    public boolean pong(UUID player, int transactionId) {
        return inner.pong(player, transactionId);
    }

    @Override
    public void pingWritten(UUID viewer, int transactionId) {
        inner.pingWritten(viewer, transactionId);
    }

    @Override
    public void entitySpawned(UUID viewer, int entityNumber, Vec3 position) {
        inner.entitySpawned(viewer, entityNumber, position);
    }

    @Override
    public void entityMoved(UUID viewer, EntityMove move) {
        inner.entityMoved(viewer, move);
    }

    @Override
    public void entitiesDestroyed(UUID viewer, int[] entityNumbers) {
        inner.entitiesDestroyed(viewer, entityNumbers);
    }

    @Override
    public void viewReset(UUID viewer) {
        inner.viewReset(viewer);
    }

    @Override
    public void joined(UUID player) {
        inner.joined(player);
    }

    @Override
    public void quit(UUID player) {
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

    @Override
    public boolean restoreHistory(UUID player) {
        return inner.restoreHistory(player);
    }
}
