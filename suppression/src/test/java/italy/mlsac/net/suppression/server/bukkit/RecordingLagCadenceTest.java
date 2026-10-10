// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static italy.mlsac.net.suppression.server.bukkit.KitBench.STEVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagCadence;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagSubject;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecordingLagCadenceTest {

    private static final long TICK_MILLIS = 50L;
    private static final int JUMP_CYCLE = 12;

    private final KitBench bench = new KitBench(LagLevel.HEAVY, LagKind.SETBACK, LagKind.GROUND, LagKind.ROTATE);
    private final JumpingSubject jumper = new JumpingSubject();
    private final LagCadence cadence = new LagCadence(bench.roster, bench.kit.ticker(), id -> Optional.of(jumper),
            bench.clock);

    private void serverTicks(int count) {
        for (int i = 0; i < count; i++) {
            bench.clock.advance(TICK_MILLIS);
            jumper.step();
            bench.kit.combat().attacked(STEVE, bench.clock.clockMillis());
            cadence.serverTicked(new ServerTick(jumper.ticks));
        }
    }

    @Test
    void withoutRecordingEveryMotionKindActs() {
        bench.flag();
        serverTicks(120);
        assertTrue(jumper.setBacks > 0);
        assertTrue(jumper.pushes > 0);
        assertTrue(jumper.turns > 0);
    }

    @Test
    void recordedSuspectGetsNoSetbackPulseOrTurnAndLagResumesAfterTheRecording() {
        bench.server.startRecording(STEVE);
        bench.flag();
        assertFalse(bench.server.subscriberLines().isEmpty());
        assertTrue(bench.kit.readout().lagLevels(STEVE).isEmpty());
        serverTicks(200);
        assertEquals(0, jumper.setBacks);
        assertEquals(0, jumper.pushes);
        assertEquals(0, jumper.turns);

        bench.server.stopRecording(STEVE);
        serverTicks(120);
        assertTrue(jumper.setBacks > 0);
        assertTrue(jumper.pushes > 0);
        assertTrue(jumper.turns > 0);
    }

    @Test
    void recordingStartedMidLagStopsTheEffectsAtOnce() {
        bench.flag();
        serverTicks(100);
        assertTrue(jumper.setBacks > 0);
        bench.server.startRecording(STEVE);
        int setBacks = jumper.setBacks;
        int pushes = jumper.pushes;
        int turns = jumper.turns;
        serverTicks(200);
        assertEquals(setBacks, jumper.setBacks);
        assertEquals(pushes, jumper.pushes);
        assertEquals(turns, jumper.turns);
        assertTrue(bench.roster.watches(STEVE));
    }

    private static final class JumpingSubject implements LagSubject, LagActuator {

        private long ticks;
        private double x;
        private int setBacks;
        private int pushes;
        private int turns;

        void step() {
            ticks++;
            x += 0.25;
        }

        private int phase() {
            return (int) (ticks % JUMP_CYCLE);
        }

        @Override
        public TickFrame frame(long now, long tick) {
            int phase = phase();
            boolean grounded = phase == 0 || phase == JUMP_CYCLE - 1;
            double y = grounded ? 64.0 : 64.0 + 0.42 * phase - 0.04 * phase * phase;
            return new TickFrame(now, tick, new WorldPoint("world", x, y, 0.0), grounded, 0.0, 0.0f, Set.of());
        }

        @Override
        public LagActuator actuator() {
            return this;
        }

        @Override
        public void setBack(WorldPoint target, float fallDistanceToRestore) {
            setBacks++;
        }

        @Override
        public void pushVelocity(Vec3 velocity) {
            pushes++;
        }

        @Override
        public void turnView(int syncId, double yawDelta, double pitchDelta) {
            turns++;
        }
    }
}
