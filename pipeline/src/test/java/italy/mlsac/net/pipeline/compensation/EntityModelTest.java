// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.ClientVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityModelTest {
    private static final UUID VIEWER = new UUID(3, 3);
    private static final int ENTITY = 42;
    private static final double EPSILON = 1e-12;

    @Test
    void workedExampleThreeRelativeMovesOnePong() {
        List<Integer> sent = new ArrayList<>();
        long[] clock = {1_000L};
        PingAcknowledgements acknowledgements = new PingAcknowledgements(VIEWER, (viewer, id) -> sent.add(id), purpose -> clock[0]);
        CompensatedViewer viewer = new CompensatedViewer(ClientQuirks.of(ClientVersion.parse("V_1_21_4")), acknowledgements);
        viewer.spawn(ENTITY, new Vec3(0.0, 64.0, 0.0));
        viewer.move(new EntityMove(ENTITY, true, true, 0.25, 0.0, 0.0));
        assertTrue(sent.isEmpty());
        viewer.move(new EntityMove(ENTITY, true, true, 0.25, 0.0, 0.0));
        assertEquals(List.of(PingAcknowledgements.firstProbeId()), sent);
        acknowledgements.probeFlushed(PingAcknowledgements.firstProbeId());
        clock[0] = 51_000_000L;
        assertTrue(acknowledgements.answered(PingAcknowledgements.firstProbeId()));
        assertEquals(50.999, acknowledgements.roundTripMillis(), EPSILON);
        viewer.move(new EntityMove(ENTITY, true, true, 0.25, 0.0, 0.0));
        assertEquals(1, sent.size());
        viewer.clientTick();
        AcknowledgedBody entity = viewer.entity(ENTITY).orElseThrow();
        Box tight = entity.tightBox(0.6, 1.8);
        assertBox(new Box(0.45, 64.0, -0.3, 1.05, 65.8, 0.3), tight);
        Box possible = entity.possibleBox(0.6, 1.8);
        assertBox(new Box(-0.05, 64.0, -0.3, 1.05, 65.8, 0.3), possible);
    }

    @Test
    void pongForUnknownIdIsPassedOn() {
        PingAcknowledgements acknowledgements = new PingAcknowledgements(VIEWER, (viewer, id) -> { }, purpose -> 0L);
        assertFalse(acknowledgements.answered(PingAcknowledgements.firstProbeId() + 5));
        CompensationBook book = new CompensationBook((viewer, id) -> { }, purpose -> 0L);
        assertFalse(book.pong(VIEWER, PingAcknowledgements.firstProbeId()));
        assertFalse(book.pong(VIEWER, 7));
    }

    @Test
    void deferredActionsWaitBehindTheHeadAndAreCapped() {
        PingAcknowledgements acknowledgements = new PingAcknowledgements(VIEWER, (viewer, id) -> { }, purpose -> 0L);
        List<Integer> ran = new ArrayList<>();
        acknowledgements.afterAcknowledged(2, () -> ran.add(2));
        acknowledgements.afterAcknowledged(1, () -> ran.add(1));
        acknowledgements.sendProbe();
        acknowledgements.probeFlushed(PingAcknowledgements.firstProbeId());
        acknowledgements.answered(PingAcknowledgements.firstProbeId());
        assertTrue(ran.isEmpty());
        for (int index = 0; index < 8191; index++) {
            acknowledgements.afterAcknowledged(100, () -> ran.add(100));
        }
        assertEquals(List.of(2), ran);
    }

    @Test
    void oldClientsAreNotTracked() {
        CompensationBook book = new CompensationBook((viewer, id) -> { }, purpose -> 0L);
        book.track(VIEWER, new ViewSeed("V_1_16_5", null, null, List.of()));
        assertEquals(Optional.empty(), book.viewer(VIEWER));
        book.track(VIEWER, new ViewSeed(null, new UUID(9, 9), Vec3.ZERO, List.of()));
        assertTrue(book.viewer(VIEWER).isPresent());
    }

    private static void assertBox(Box expected, Box actual) {
        assertEquals(expected.low().x(), actual.low().x(), EPSILON);
        assertEquals(expected.low().y(), actual.low().y(), EPSILON);
        assertEquals(expected.low().z(), actual.low().z(), EPSILON);
        assertEquals(expected.high().x(), actual.high().x(), EPSILON);
        assertEquals(expected.high().y(), actual.high().y(), EPSILON);
        assertEquals(expected.high().z(), actual.high().z(), EPSILON);
    }
}
