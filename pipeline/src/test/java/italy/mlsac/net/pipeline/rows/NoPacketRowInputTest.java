// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.compensation.CompensationBook;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.occlusion.MarginOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.ServerSamples;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NoPacketRowInputTest {
    private static final long MILLIS = 1_000_000L;

    private final UUID player = new UUID(5, 6);
    private final List<TickRow> rows = new ArrayList<>();
    private final List<ClickRow> clicks = new ArrayList<>();
    private final ServerSamples samples = new ServerSamples();

    @Test
    void recordedNoPacketRowTakesTheAttackSwingAndTickEndLikeTheTrainingData() {
        RowProducer producer = producer(NoPacketRowInput.CONSUMED);
        fightWithAQuietTick(producer);
        TickRow quiet = rows.get(2);
        assertTrue(quiet.packet().noPacket());
        assertEquals(1, quiet.inputs().attacks());
        assertEquals(2, quiet.inputs().swings());
        assertEquals(1, quiet.inputs().airSwings());
        assertEquals(1, quiet.inputs().slotChanges());
        assertTrue(quiet.rotation().valid());
        assertEquals(1, clicks.size());
        assertEquals(2, clicks.get(0).timing().rotationSource());
        assertEquals(quiet.timing().row(), clicks.get(0).timing().row());
        TickRow next = rows.get(3);
        assertEquals(0, next.inputs().attacks());
        assertEquals(1, next.timing().tickEnds());
        assertEquals(1, next.inputs().rowsAfterLastSwing());
        assertEquals(20, next.rotation().yawTurn());
        assertEquals(20, next.rotation().yawTurnChange());
    }

    @Test
    void liveNoPacketRowLeavesTheInputAndTheDerivativesToTheNextPacketRow() {
        RowProducer producer = producer(NoPacketRowInput.CARRIED_TO_NEXT_PACKET);
        fightWithAQuietTick(producer);
        TickRow quiet = rows.get(2);
        assertTrue(quiet.packet().noPacket());
        assertEquals(0, quiet.inputs().attacks());
        assertFalse(quiet.rotation().valid());
        TickRow next = rows.get(3);
        assertEquals(1, next.inputs().attacks());
        assertEquals(2, next.inputs().swings());
        assertEquals(1, next.inputs().slotChanges());
        assertEquals(2, next.timing().tickEnds());
        assertEquals(0, next.inputs().rowsAfterLastSwing());
        assertEquals(1, clicks.size());
        assertEquals(1, clicks.get(0).timing().rotationSource());
        assertEquals(20, next.rotation().yawTurn());
        assertEquals(10, next.rotation().yawTurnChange());
    }

    private void fightWithAQuietTick(RowProducer producer) {
        move(producer, 1, 0f);
        producer.noMoveTick(2 * MILLIS);
        move(producer, 51, 10f);
        producer.noMoveTick(52 * MILLIS);
        producer.swing(60 * MILLIS);
        producer.attack(new AttackPacket(player, 60 * MILLIS, 99, false));
        producer.swing(61 * MILLIS);
        producer.slotChanged();
        producer.noMoveTick(102 * MILLIS);
        move(producer, 151, 30f);
    }

    private RowProducer producer(NoPacketRowInput input) {
        return new RowProducer(player, 0, new RowSink() {
            public void tickRow(TickRow row) { rows.add(row); }
            public void clickRow(ClickRow row) { clicks.add(row); }
            public void fightStarted() { }
            public void segmentBroken() { }
        }, samples, new ClientSightRewind(new CompensationBook((p, id) -> { }, purpose -> 0), samples),
                new OcclusionQueue(new MarginOcclusion(new OcclusionProbe((world, from, to) -> RayVerdict.CLEAR))),
                input);
    }

    private void move(RowProducer producer, long millis, float yaw) {
        producer.movement(new MovementPacket(player, millis * MILLIS, yaw, 0, true, 0, 64, 0, true, true, false));
    }
}
