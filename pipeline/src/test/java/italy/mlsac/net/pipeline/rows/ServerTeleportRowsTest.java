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
import italy.mlsac.net.pipeline.occlusion.LiveOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.ServerSamples;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServerTeleportRowsTest {
    private static final long MILLIS = 1_000_000L;
    private static final UUID WORLD = new UUID(9, 9);
    private static final int VICTIM_ENTITY = 77;

    private final UUID player = new UUID(1, 2);
    private final UUID victim = new UUID(3, 4);
    private final List<TickRow> rows = new ArrayList<>();
    private final List<String> sinkCalls = new ArrayList<>();
    private final ServerSamples samples = new ServerSamples();
    private final RowProducer producer = new RowProducer(player, 0, new CollectingSink(),
            samples, new ClientSightRewind(new CompensationBook((viewer, id) -> { }, purpose -> 0), samples),
            new OcclusionQueue(new LiveOcclusion(new OcclusionProbe((world, from, to) -> RayVerdict.CLEAR))),
            NoPacketRowInput.CARRIED_TO_NEXT_PACKET);

    @Test
    void serverTeleportStartsASegmentButKeepsTheFightRunning() {
        samples.capture(sample(victim, VICTIM_ENTITY, 1, 3.0));
        move(1, 0f, 0.0);
        producer.attack(new AttackPacket(player, 2 * MILLIS, VICTIM_ENTITY, false));
        move(3, 5f, 0.1);
        assertTrue(rows.get(1).timing().engagedInFight());
        producer.serverTeleported();
        move(4, 10f, 40.0);
        move(5, 15f, 40.1);
        TickRow afterTeleport = rows.get(2);
        assertEquals(1, afterTeleport.timing().segmentOrdinal());
        assertTrue(afterTeleport.timing().engagedInFight());
        assertFalse(afterTeleport.rotation().valid());
        assertFalse(afterTeleport.move().valid());
        TickRow next = rows.get(3);
        assertEquals(1, next.timing().segmentOrdinal());
        assertTrue(next.timing().engagedInFight());
        assertTrue(next.rotation().valid());
        assertEquals(5.0, next.rotation().yawTurn(), 1e-9);
        assertEquals(0.1, next.move().x(), 1e-9);
        assertEquals(List.of("fightStarted", "segmentBroken"), sinkCalls);
    }

    @Test
    void confirmationAfterTheServerTeleportDoesNotOpenAnotherSegment() {
        move(1, 0f, 0.0);
        producer.serverTeleported();
        producer.teleportConfirmed();
        move(2, 10f, 40.0);
        move(3, 20f, 40.0);
        assertEquals(1, rows.get(1).timing().segmentOrdinal());
        assertTrue(rows.get(1).packet().echo());
        assertEquals(1, rows.get(2).timing().segmentOrdinal());
    }

    private void move(long millis, float yaw, double x) {
        producer.movement(new MovementPacket(player, millis * MILLIS, yaw, 0, true, x, 64, 0, true, true, false));
    }

    private static PlayerSample sample(UUID who, int entity, long at, double x) {
        return new PlayerSample(who, WORLD, entity, at, x, 64.0, 0.0, 0f, 0f, 1.62, 1.8, 0.6, 0.0, 1f, true, false,
                new PlayerPosture(false, false, false, false, false, false), "DIAMOND_SWORD", 12.5f, "SURVIVAL",
                3.0);
    }

    private final class CollectingSink implements RowSink {
        @Override
        public void tickRow(TickRow row) {
            rows.add(row);
        }

        @Override
        public void clickRow(ClickRow row) {
        }

        @Override
        public void fightStarted() {
            sinkCalls.add("fightStarted");
        }

        @Override
        public void segmentBroken() {
            sinkCalls.add("segmentBroken");
        }
    }
}
