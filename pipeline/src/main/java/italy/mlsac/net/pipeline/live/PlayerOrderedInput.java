// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.engine.PlayerTurns;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import java.util.UUID;

public final class PlayerOrderedInput implements PipelineInput {

    private final DataPipeline pipeline;
    private final PlayerTurns turns;

    public PlayerOrderedInput(DataPipeline pipeline) {
        this.pipeline = pipeline;
        this.turns = pipeline.turns();
    }

    @Override
    public void enabled() {
        pipeline.enabled();
    }

    @Override
    public void disabled() {
        pipeline.disabled();
    }

    @Override
    public void gameTick(long tick) {
        pipeline.gameTick(tick);
    }

    @Override
    public void playerTick(UUID player, long tick) {
        synchronized (turns.of(player)) {
            pipeline.playerTick(player, tick);
        }
    }

    @Override
    public void movement(MovementPacket packet) {
        synchronized (turns.of(packet.player())) {
            pipeline.movement(packet);
        }
    }

    @Override
    public void clientTickEnd(TickEndPacket packet) {
        synchronized (turns.of(packet.player())) {
            pipeline.clientTickEnd(packet);
        }
    }

    @Override
    public void teleportConfirmed(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.teleportConfirmed(player);
        }
    }

    @Override
    public void swing(SwingPacket packet) {
        synchronized (turns.of(packet.player())) {
            pipeline.swing(packet);
        }
    }

    @Override
    public void slotChanged(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.slotChanged(player);
        }
    }

    @Override
    public void attack(AttackPacket packet) {
        synchronized (turns.of(packet.player())) {
            pipeline.attack(packet);
        }
    }

    @Override
    public boolean pong(UUID player, int transactionId) {
        synchronized (turns.of(player)) {
            return pipeline.pong(player, transactionId);
        }
    }

    @Override
    public void pingWritten(UUID viewer, int transactionId) {
        synchronized (turns.of(viewer)) {
            pipeline.pingWritten(viewer, transactionId);
        }
    }

    @Override
    public void entitySpawned(UUID viewer, int entityNumber, Vec3 position) {
        synchronized (turns.of(viewer)) {
            pipeline.entitySpawned(viewer, entityNumber, position);
        }
    }

    @Override
    public void entityMoved(UUID viewer, EntityMove move) {
        synchronized (turns.of(viewer)) {
            pipeline.entityMoved(viewer, move);
        }
    }

    @Override
    public void entitiesDestroyed(UUID viewer, int[] entityNumbers) {
        synchronized (turns.of(viewer)) {
            pipeline.entitiesDestroyed(viewer, entityNumbers);
        }
    }

    @Override
    public void viewReset(UUID viewer) {
        synchronized (turns.of(viewer)) {
            pipeline.viewReset(viewer);
        }
    }

    @Override
    public void joined(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.joined(player);
        }
    }

    @Override
    public void quit(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.quit(player);
        }
        turns.retire(player);
    }

    @Override
    public void died(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.died(player);
        }
    }

    @Override
    public void respawned(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.respawned(player);
        }
    }

    @Override
    public void worldSwitched(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.worldSwitched(player);
        }
    }

    @Override
    public void teleported(UUID player) {
        synchronized (turns.of(player)) {
            pipeline.teleported(player);
        }
    }

    public boolean inLegacyCombat(UUID player) {
        return pipeline.inLegacyCombat(player);
    }
}
