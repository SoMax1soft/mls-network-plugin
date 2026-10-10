// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.ScriptedRandom;
import italy.mlsac.net.suppression.lag.GroundStrength;
import italy.mlsac.net.suppression.lag.IntervalRange;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.motion.GroundLag;
import italy.mlsac.net.suppression.lag.motion.GroundPulseRule;
import italy.mlsac.net.suppression.lag.motion.GroundStep;
import italy.mlsac.net.suppression.lag.motion.IntervalSchedule;
import italy.mlsac.net.suppression.lag.motion.PositionHistory;
import italy.mlsac.net.suppression.lag.motion.RotateLag;
import italy.mlsac.net.suppression.lag.motion.SetbackLag;
import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.lag.motion.Turn;
import italy.mlsac.net.suppression.lag.motion.TurnRoll;
import italy.mlsac.net.suppression.lag.runtime.BodyCondition;
import italy.mlsac.net.suppression.lag.runtime.CombatStatus;
import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.lag.runtime.LagPauses;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.lag.runtime.TickScene;
import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.motion.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MotionLagTest {

    private static final long TICK = 50L;

    private final LagTable table = LagTable.standard();
    private final ScriptedRandom random = new ScriptedRandom(0.0);
    private final LagRuntime<String> runtime = new LagRuntime<>();
    private final List<String> actions = new ArrayList<>();
    private final List<Vec3> pushes = new ArrayList<>();
    private boolean passable = true;

    private final LagActuator actuator = new LagActuator() {
        @Override
        public void setBack(WorldPoint target, float fallDistanceToRestore) {
            actions.add("setback " + target.z() + " fall " + fallDistanceToRestore);
        }

        @Override
        public void pushVelocity(Vec3 velocity) {
            pushes.add(velocity);
            actions.add("push " + velocity.y());
        }

        @Override
        public void turnView(int syncId, double yawDelta, double pitchDelta) {
            actions.add("turn " + syncId);
        }
    };

    private final SetbackTerrain terrain = new SetbackTerrain() {
        @Override
        public boolean chunkLoaded(WorldPoint position) {
            return true;
        }

        @Override
        public boolean passable(WorldPoint position) {
            return passable;
        }
    };

    private static TickFrame frame(long now, double z) {
        return new TickFrame(now, now / TICK, new WorldPoint("world", 0, 64, z), true, 0, 2.5f, Set.of());
    }

    private void setbackTick(SetbackLag setback, LagLevel level, long now, double z, CombatStatus combat) {
        setback.advanceTick(new TickScene(frame(now, z), level, runtime, combat, actuator));
    }

    private void setbackTick(SetbackLag setback, long now, double z, CombatStatus combat) {
        setbackTick(setback, LagLevel.MEDIUM, now, z, combat);
    }

    private SetbackLag armedSetback(LagLevel level) {
        runtime.setback().schedule().keepFor(0, table.setback(level).interval(), random);
        return new SetbackLag(table, random, terrain);
    }

    private SetbackLag armedSetback() {
        return armedSetback(LagLevel.MEDIUM);
    }

    private void walkFullRing(SetbackLag setback, LagLevel level, long until, double stride, CombatStatus combat) {
        long first = until - TICK * (PositionHistory.CAPACITY - 1);
        for (long time = first; time <= until; time += TICK) {
            setbackTick(setback, level, time, stride * ((time - first) / TICK), combat);
        }
    }

    @Test
    void r1SetbackNeedsMoreHistoryThanTheRewind() {
        SetbackLag setback = armedSetback();
        for (long time = 1_300; time <= 1_500; time += TICK) {
            setbackTick(setback, time, (time - 1_300) / 100.0, CombatStatus.IN_COMBAT);
        }
        assertEquals(List.of(), actions);
        assertEquals(3_000, runtime.setback().schedule().nextAt());
    }

    @Test
    void r1SetbackRewindsFiveTicksAndClearsHistory() {
        SetbackLag setback = armedSetback();
        for (long time = 1_250; time <= 1_500; time += TICK) {
            setbackTick(setback, time, (time - 1_250) / 125.0, CombatStatus.IN_COMBAT);
        }
        assertEquals(List.of("setback 0.0 fall 2.5"), actions);
        assertEquals(0, runtime.setback().history().size());
    }

    @Test
    void r2TooFarOrTooCloseTargetsAreSkippedAndRescheduledBelowHeavy() {
        SetbackLag setback = armedSetback();
        for (long time = 1_250; time <= 1_500; time += TICK) {
            setbackTick(setback, time, (time - 1_250) / 50.0, CombatStatus.IN_COMBAT);
        }
        assertEquals(List.of(), actions);
        assertEquals(3_000, runtime.setback().schedule().nextAt());
        double[] still = {0.0, 0.05, 0.1, 0.1, 0.1, 0.2};
        for (int step = 0; step < still.length; step++) {
            setbackTick(setback, 2_750 + TICK * step, still[step], CombatStatus.IN_COMBAT);
        }
        assertEquals(List.of(), actions);
        assertEquals(4_500, runtime.setback().schedule().nextAt());
    }

    @Test
    void r3MediumOutOfCombatKeepsWaitingWithoutRescheduling() {
        SetbackLag setback = armedSetback();
        for (long time = 1_250; time <= 1_500; time += TICK) {
            setbackTick(setback, time, (time - 1_250) / 125.0, CombatStatus.CALM);
        }
        assertEquals(List.of(), actions);
        assertEquals(1_500, runtime.setback().schedule().nextAt());
        setbackTick(setback, 1_550, 2.4, CombatStatus.IN_COMBAT);
        assertEquals(List.of("setback 0.4 fall 2.5"), actions);
    }

    @Test
    void heavySetbackRunsWithoutAFightSoWalkingAwayDoesNotHelp() {
        SetbackLag setback = armedSetback(LagLevel.HEAVY);
        assertEquals(500, runtime.setback().schedule().nextAt());
        walkFullRing(setback, LagLevel.HEAVY, 500, 0.3, CombatStatus.CALM);
        assertEquals(List.of("setback 0.0 fall 2.5"), actions);
        assertEquals(1_000, runtime.setback().schedule().nextAt());
    }

    @Test
    void heavySetbackFallsBackToTheNewestEntryInsideTheDistance() {
        SetbackLag setback = armedSetback(LagLevel.HEAVY);
        walkFullRing(setback, LagLevel.HEAVY, 500, 1.0, CombatStatus.CALM);
        assertEquals(List.of("setback 9.0 fall 2.5"), actions);
    }

    @Test
    void heavySetbackWithAShortRingStillUsesAnEntryInsideTheDistance() {
        SetbackLag setback = armedSetback(LagLevel.HEAVY);
        for (long time = 300; time <= 500; time += TICK) {
            setbackTick(setback, LagLevel.HEAVY, time, (time - 300) / 50.0 * 0.2, CombatStatus.CALM);
        }
        assertEquals(List.of("setback 0.4 fall 2.5"), actions);
    }

    @Test
    void heavySetbackStillSkipsBlockedTargets() {
        passable = false;
        SetbackLag setback = armedSetback(LagLevel.HEAVY);
        walkFullRing(setback, LagLevel.HEAVY, 500, 1.0, CombatStatus.CALM);
        assertEquals(List.of(), actions);
        assertEquals(1_000, runtime.setback().schedule().nextAt());
    }

    @Test
    void successfulSetbackPausesGroundPulsesForFiveTicks() {
        SetbackLag setback = armedSetback(LagLevel.HEAVY);
        walkFullRing(setback, LagLevel.HEAVY, 500, 0.3, CombatStatus.IN_COMBAT);
        assertEquals(1, actions.size());
        long setbackTick = 500 / TICK;
        assertTrue(runtime.pauses().groundBlocked(setbackTick + LagPauses.AFTER_TELEPORT_TICKS));
        assertFalse(runtime.pauses().groundBlocked(setbackTick + LagPauses.AFTER_TELEPORT_TICKS + 1));
    }

    @Test
    void setbackIsSkippedWhenTheTargetIsBlocked() {
        passable = false;
        SetbackLag setback = armedSetback();
        for (long time = 1_250; time <= 1_500; time += TICK) {
            setbackTick(setback, time, (time - 1_250) / 125.0, CombatStatus.IN_COMBAT);
        }
        assertEquals(List.of(), actions);
    }

    @Test
    void r5TurnRollLimitsPitchAndDrawsSignYawPitchInOrder() {
        ScriptedRandom rolls = new ScriptedRandom(0.5).queue(0.7, 0.5, 0.9);
        Turn turn = new TurnRoll(rolls).roll(table.rotate(LagLevel.HEAVY), 85.0);
        assertEquals(-67.5, turn.yawDelta(), 1e-9);
        assertEquals(5.0, turn.pitchDelta(), 1e-9);
        assertEquals(3, rolls.draws());
    }

    @Test
    void mediumRotateUsesNegativeSyncIdsAndWaitsForCombat() {
        RotateLag rotate = new RotateLag(table, random, new TurnRoll(random));
        rotate.advanceTick(new TickScene(frame(0, 0), LagLevel.MEDIUM, runtime, CombatStatus.IN_COMBAT, actuator));
        rotate.advanceTick(new TickScene(frame(1_500, 0), LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        assertEquals(List.of(), actions);
        rotate.advanceTick(new TickScene(frame(1_550, 0), LagLevel.MEDIUM, runtime, CombatStatus.IN_COMBAT,
                actuator));
        rotate.advanceTick(new TickScene(frame(3_050, 0), LagLevel.MEDIUM, runtime, CombatStatus.IN_COMBAT,
                actuator));
        assertEquals(List.of("turn -1", "turn -2"), actions);
    }

    @Test
    void heavyRotateTurnsWithoutAFight() {
        RotateLag rotate = new RotateLag(table, random, new TurnRoll(random));
        rotate.advanceTick(new TickScene(frame(0, 0), LagLevel.HEAVY, runtime, CombatStatus.CALM, actuator));
        rotate.advanceTick(new TickScene(frame(500, 0), LagLevel.HEAVY, runtime, CombatStatus.CALM, actuator));
        rotate.advanceTick(new TickScene(frame(1_000, 0), LagLevel.HEAVY, runtime, CombatStatus.CALM, actuator));
        assertEquals(List.of("turn -1", "turn -2"), actions);
    }

    @Test
    void r6GroundMediumPulseRules() {
        GroundPulseRule rule = new GroundPulseRule();
        GroundStrength medium = table.ground(LagLevel.MEDIUM);
        assertEquals(OptionalDouble.of(0.0), rule.pulseFor(new GroundStep(true, false, 0.42, 1), medium));
        assertEquals(OptionalDouble.of(-0.30), rule.pulseFor(new GroundStep(false, false, -0.10, 4), medium));
        assertEquals(OptionalDouble.empty(), rule.pulseFor(new GroundStep(false, false, -0.40, 7), medium));
        assertEquals(OptionalDouble.of(-0.30), rule.pulseFor(new GroundStep(false, false, 0.0, 7), medium));
        assertEquals(OptionalDouble.empty(), rule.pulseFor(new GroundStep(false, false, 0.0, 5), medium));
    }

    @Test
    void groundPulseVelocityHasNoHorizontalPart() {
        GroundPulseRule rule = new GroundPulseRule();
        assertEquals(new Vec3(0.0, -0.5, 0.0), rule.velocity(-0.5));
        assertEquals(new Vec3(0.0, 0.1, 0.0), rule.velocity(0.1));
        assertEquals(new Vec3(0.0, 0.0, 0.0), rule.velocity(0.0));
    }

    @Test
    void sprintJumpPulsesAtEveryLevelStopHorizontalMotion() {
        for (LagLevel level : LagLevel.values()) {
            LagRuntime<String> fresh = new LagRuntime<>();
            pushes.clear();
            GroundLag ground = new GroundLag(table, new GroundPulseRule());
            double z = 0.0;
            double y = 64.0;
            double rise = 0.42;
            ground.advanceTick(new TickScene(sprintFrame(100, z, y, true), level, fresh, CombatStatus.CALM,
                    actuator));
            for (long tick = 101; tick <= 112; tick++) {
                z += 0.6;
                y += rise;
                rise = (rise - 0.08) * 0.98;
                ground.advanceTick(new TickScene(sprintFrame(tick, z, y, false), level, fresh, CombatStatus.CALM,
                        actuator));
            }
            assertFalse(pushes.isEmpty(), level.name());
            for (Vec3 push : pushes) {
                assertEquals(0.0, push.x(), 0.0, level.name());
                assertEquals(0.0, push.z(), 0.0, level.name());
            }
        }
    }

    @Test
    void groundStaysQuietForFiveTicksAfterAnyTeleport() {
        GroundLag ground = new GroundLag(table, new GroundPulseRule());
        runtime.pauses().teleported(100);
        jumpAt(ground, 104);
        assertTrue(pushes.isEmpty());
        jumpAt(ground, 105);
        assertTrue(pushes.isEmpty());
        jumpAt(ground, 106);
        assertEquals(1, pushes.size());
    }

    @Test
    void groundJumpStartIsCutAndSkippedAfterALaunch() {
        GroundLag ground = new GroundLag(table, new GroundPulseRule());
        long standTick = 100;
        TickFrame standing = new TickFrame(0, standTick, new WorldPoint("world", 0, 64.0, 0), true, 0, 0f, Set.of());
        TickFrame jumping = new TickFrame(TICK, standTick + 1, new WorldPoint("world", 0.3, 64.42, 0), false, 0, 0f,
                Set.of());
        ground.advanceTick(new TickScene(standing, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        ground.advanceTick(new TickScene(jumping, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        assertEquals(List.of("push 0.0"), actions);
        runtime.ground().reset();
        runtime.pauses().launched();
        runtime.pauses().settle(100);
        ground.advanceTick(new TickScene(standing, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        ground.advanceTick(new TickScene(jumping, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        assertEquals(1, actions.size());
    }

    @Test
    void groundSkipsSwimmersAndGliders() {
        GroundLag ground = new GroundLag(table, new GroundPulseRule());
        long standTick = 100;
        TickFrame standing = new TickFrame(0, standTick, new WorldPoint("world", 0, 64.0, 0), true, 0, 0f,
                Set.of(BodyCondition.IN_LIQUID));
        TickFrame rising = new TickFrame(TICK, standTick + 1, new WorldPoint("world", 0, 64.42, 0), false, 0, 0f,
                Set.of(BodyCondition.IN_LIQUID));
        ground.advanceTick(new TickScene(standing, LagLevel.HEAVY, runtime, CombatStatus.CALM, actuator));
        ground.advanceTick(new TickScene(rising, LagLevel.HEAVY, runtime, CombatStatus.CALM, actuator));
        assertTrue(actions.isEmpty());
    }

    @Test
    void strongerLevelPullsAFarDueTimeCloser() {
        IntervalSchedule schedule = new IntervalSchedule();
        ScriptedRandom draws = new ScriptedRandom(0.0);
        schedule.keepFor(0, new IntervalRange(8_000, 12_000), draws);
        assertEquals(8_000, schedule.nextAt());
        schedule.keepFor(1_000, new IntervalRange(2_000, 4_000), draws);
        assertEquals(3_000, schedule.nextAt());
        schedule.keepFor(1_050, new IntervalRange(4_000, 7_000), draws);
        assertEquals(3_000, schedule.nextAt());
    }

    private void jumpAt(GroundLag ground, long jumpTick) {
        runtime.ground().reset();
        TickFrame standing = new TickFrame(0, jumpTick - 1, new WorldPoint("world", 0, 64.0, 0), true, 0, 0f,
                Set.of());
        TickFrame jumping = new TickFrame(TICK, jumpTick, new WorldPoint("world", 0.2, 64.42, 0), false, 0, 0f,
                Set.of());
        ground.advanceTick(new TickScene(standing, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
        ground.advanceTick(new TickScene(jumping, LagLevel.MEDIUM, runtime, CombatStatus.CALM, actuator));
    }

    private static TickFrame sprintFrame(long tick, double z, double y, boolean grounded) {
        return new TickFrame(tick * TICK, tick, new WorldPoint("world", 0, y, z), grounded, 0, 0f, Set.of());
    }
}
