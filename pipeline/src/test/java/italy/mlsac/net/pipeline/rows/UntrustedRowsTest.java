// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import static org.junit.jupiter.api.Assertions.*;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.compensation.CompensationBook;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.occlusion.LiveOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.ServerSamples;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UntrustedRowsTest {
    private final UUID player = new UUID(1, 2);
    private final List<TickRow> rows = new ArrayList<>();
    private final List<ClickRow> clicks = new ArrayList<>();
    private final ServerSamples samples = new ServerSamples();
    private final RowProducer producer = new RowProducer(player, 0, new RowSink() {
        public void tickRow(TickRow row) { rows.add(row); }
        public void clickRow(ClickRow row) { clicks.add(row); }
        public void fightStarted() { }
        public void segmentBroken() { }
    }, samples, new ClientSightRewind(new CompensationBook((p, id) -> { }, purpose -> 0), samples),
            new OcclusionQueue(new LiveOcclusion(new OcclusionProbe((world, from, to) -> RayVerdict.CLEAR))),
            NoPacketRowInput.CARRIED_TO_NEXT_PACKET);

    @Test
    void cancelledPositionCannotBreakRotationOrPoisonTheNextPosition() {
        move(1, 0, 0, false);
        move(2, 10, 1000, true);
        move(3, 20, 1, false);
        assertEquals(0, rows.get(1).timing().segmentOrdinal());
        assertEquals(0, rows.get(2).timing().segmentOrdinal());
        assertEquals(10, rows.get(2).rotation().yawTurn());
        assertEquals(1, rows.get(2).move().x());
    }

    @Test
    void rawCoordinateJumpDoesNotDestroyTheRotationWindow() {
        move(1, 0, 0, false);
        move(2, 10, 1000, false);
        assertEquals(0, rows.get(1).timing().segmentOrdinal());
        assertTrue(rows.get(1).rotation().valid());
        assertFalse(rows.get(1).move().valid());
        move(3, 20, 1, false);
        assertEquals(1, rows.get(2).move().x());
    }

    @Test
    void malformedMovementCannotClearHistory() {
        move(1, 0, 0, false);
        move(2, Float.NaN, 0, false);
        move(3, 10, 0, false);
        assertEquals(2, rows.size());
        assertEquals(0, rows.get(1).timing().segmentOrdinal());
        assertEquals(10, rows.get(1).rotation().yawTurn());
    }

    @Test
    void tickEndsCannotConsumeAttacksOrAlterRealRotationDerivatives() {
        move(1, 0, 0, false);
        move(2, 10, 0, false);
        producer.noMoveTick(3_000_000);
        producer.attack(new AttackPacket(player, 4_000_000, 99, false));
        producer.noMoveTick(5_000_000);
        producer.noMoveTick(6_000_000);
        assertTrue(clicks.isEmpty());
        move(7, 30, 0, false);
        TickRow real = rows.get(rows.size() - 1);
        assertEquals(1, real.inputs().attacks());
        assertEquals(1, clicks.size());
        assertEquals(real.timing().row(), clicks.get(0).timing().row());
        assertEquals(20, real.rotation().yawTurn());
        assertEquals(10, real.rotation().yawTurnChange());
    }

    @Test
    void confirmedTeleportStillBreaksTheSegmentOnce() {
        move(1, 0, 0, false);
        producer.teleportConfirmed();
        move(2, 10, 0, false);
        move(3, 20, 0, false);
        assertEquals(1, rows.get(1).timing().segmentOrdinal());
        assertTrue(rows.get(1).packet().echo());
        assertFalse(rows.get(1).rotation().valid());
        assertEquals(1, rows.get(2).timing().segmentOrdinal());
        assertTrue(rows.get(2).rotation().valid());
    }

    private void move(long millis, float yaw, double x, boolean cancelled) {
        producer.movement(new MovementPacket(player, millis * 1_000_000, yaw, 0, true, x, 64, 0, true, true, cancelled));
    }

    @Test
    void attackFloodWithoutMovementKeepsBoundedPendingDetailsAndAggregateHitCounts() {
        move(1, 0, 0, false);
        for (int index = 0; index < 1000; index++) {
            producer.attack(new AttackPacket(player, 2_000_000L + index, 99, false));
        }
        move(3, 10, 0, false);
        assertEquals(256, clicks.size());
        assertEquals(1000, rows.get(1).inputs().attacks());
    }
}
