// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static italy.mlsac.net.suppression.server.bukkit.KitBench.STEVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagCadence;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LagCadenceTest {

    private static final long TICK_MILLIS = 50L;
    private static final double STRIDE = 0.25;
    private static final int HEAVY_REWIND = 10;

    private final KitBench bench = new KitBench(LagLevel.HEAVY, LagKind.SETBACK);
    private final WalkingSubject walker = new WalkingSubject();
    private final List<UUID> askedFor = new ArrayList<>();
    private boolean online = true;
    private final LagCadence cadence = new LagCadence(bench.roster, bench.kit.ticker(), id -> {
        askedFor.add(id);
        return online ? Optional.of(walker) : Optional.empty();
    }, bench.clock);

    private void serverTicks(int count) {
        for (int i = 0; i < count; i++) {
            bench.clock.advance(TICK_MILLIS);
            walker.step();
            bench.kit.combat().attacked(STEVE, bench.clock.clockMillis());
            cadence.serverTicked(new ServerTick(walker.ticks));
        }
    }

    @Test
    void rosterFollowsTickLagEffects() {
        assertFalse(bench.roster.watches(STEVE));
        bench.flag();
        assertTrue(bench.roster.watches(STEVE));
    }

    @Test
    void fightingSuspectIsSetBackFiveTicks() {
        bench.flag();
        serverTicks(80);
        assertFalse(walker.setBacks.isEmpty());
        WorldPoint target = walker.setBacks.get(0);
        assertEquals(walker.xAtSetBack.get(0) - HEAVY_REWIND * STRIDE, target.x(), 1.0E-9);
    }

    @Test
    void expiredEffectLeavesTheRosterAfterOneLastTick() {
        bench.flag();
        serverTicks(5);
        bench.clock.advance(KitBench.LAG_SECONDS * 1000L);
        serverTicks(1);
        assertFalse(bench.roster.watches(STEVE));
        int asked = askedFor.size();
        serverTicks(3);
        assertEquals(asked, askedFor.size());
    }

    @Test
    void offlineSuspectIsDropped() {
        bench.flag();
        online = false;
        serverTicks(1);
        assertFalse(bench.roster.watches(STEVE));
    }

    @Test
    void playerTicksOnlyReachWatchedSuspects() {
        cadence.playerTicked(new PlayerTick(STEVE, 1L));
        assertTrue(askedFor.isEmpty());
        bench.flag();
        cadence.playerTicked(new PlayerTick(STEVE, 2L));
        assertEquals(List.of(STEVE), askedFor);
    }

    private static final class WalkingSubject implements LagSubject, LagActuator {

        private final List<WorldPoint> setBacks = new ArrayList<>();
        private final List<Double> xAtSetBack = new ArrayList<>();
        private long ticks;
        private double x;

        void step() {
            ticks++;
            x += STRIDE;
        }

        @Override
        public TickFrame frame(long now, long tick) {
            return new TickFrame(now, tick, new WorldPoint("world", x, 64.0, 0.0), true, 0.0, 0.0f, Set.of());
        }

        @Override
        public LagActuator actuator() {
            return this;
        }

        @Override
        public void setBack(WorldPoint target, float fallDistanceToRestore) {
            setBacks.add(target);
            xAtSetBack.add(x);
        }

        @Override
        public void pushVelocity(Vec3 velocity) {
        }

        @Override
        public void turnView(int syncId, double yawDelta, double pitchDelta) {
        }
    }
}
