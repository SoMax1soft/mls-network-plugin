// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.lag.DelayStrength;
import italy.mlsac.net.suppression.lag.GroundStrength;
import italy.mlsac.net.suppression.lag.IntervalRange;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.MissStrength;
import italy.mlsac.net.suppression.lag.Pursuit;
import italy.mlsac.net.suppression.lag.RotateStrength;
import italy.mlsac.net.suppression.lag.SetbackStrength;
import italy.mlsac.net.suppression.lag.SnapReach;
import italy.mlsac.net.suppression.lag.motion.PositionHistory;
import org.junit.jupiter.api.Test;

class LagLevelTableTest {

    private final LagTable table = LagTable.standard();

    @Test
    void delayHoldTimesAreUnchanged() {
        assertEquals(new DelayStrength(100), table.delay(LagLevel.LIGHT));
        assertEquals(new DelayStrength(250), table.delay(LagLevel.MEDIUM));
        assertEquals(new DelayStrength(500), table.delay(LagLevel.HEAVY));
    }

    @Test
    void missChancesAndCaps() {
        assertEquals(MissStrength.capped(0.50, 3), table.miss(LagLevel.LIGHT));
        assertEquals(MissStrength.capped(0.75, 8), table.miss(LagLevel.MEDIUM));
        MissStrength heavy = table.miss(LagLevel.HEAVY);
        assertEquals(1.00, heavy.chance());
        assertFalse(heavy.hasCap());
        assertFalse(heavy.capReachedAfter(1_000_000));
        assertTrue(table.miss(LagLevel.MEDIUM).capReachedAfter(8));
        assertFalse(table.miss(LagLevel.MEDIUM).capReachedAfter(7));
    }

    @Test
    void setbackIntervalsRewindsAndDistances() {
        assertEquals(new SetbackStrength(new IntervalRange(3000, 5000), 3, new SnapReach(0.3, 3.0),
                Pursuit.DURING_FIGHTS), table.setback(LagLevel.LIGHT));
        assertEquals(new SetbackStrength(new IntervalRange(1500, 3000), 5, new SnapReach(0.3, 4.0),
                Pursuit.DURING_FIGHTS), table.setback(LagLevel.MEDIUM));
        assertEquals(new SetbackStrength(new IntervalRange(500, 1000), 10, new SnapReach(0.3, 6.0),
                Pursuit.RELENTLESS), table.setback(LagLevel.HEAVY));
        assertTrue(PositionHistory.CAPACITY > table.setback(LagLevel.HEAVY).rewindTicks());
    }

    @Test
    void groundPulses() {
        assertEquals(new GroundStrength(0.10, 10, 10, -0.20), table.ground(LagLevel.LIGHT));
        assertEquals(new GroundStrength(0.00, 4, 3, -0.30), table.ground(LagLevel.MEDIUM));
        assertEquals(new GroundStrength(-0.20, 2, 1, -0.50), table.ground(LagLevel.HEAVY));
    }

    @Test
    void rotateIntervalsAndOffsets() {
        assertEquals(new RotateStrength(new IntervalRange(3000, 5000), 10, 20, 3, Pursuit.DURING_FIGHTS),
                table.rotate(LagLevel.LIGHT));
        assertEquals(new RotateStrength(new IntervalRange(1500, 3000), 20, 45, 6, Pursuit.DURING_FIGHTS),
                table.rotate(LagLevel.MEDIUM));
        assertEquals(new RotateStrength(new IntervalRange(500, 1500), 45, 90, 10, Pursuit.RELENTLESS),
                table.rotate(LagLevel.HEAVY));
    }

    @Test
    void onlyHeavyIgnoresTheFightRuleAndFallsBack() {
        assertTrue(Pursuit.DURING_FIGHTS.waitsForFight());
        assertFalse(Pursuit.DURING_FIGHTS.fallsBackWithinReach());
        assertFalse(Pursuit.RELENTLESS.waitsForFight());
        assertTrue(Pursuit.RELENTLESS.fallsBackWithinReach());
    }

    @Test
    void snapReachBoundsAreInclusive() {
        SnapReach reach = new SnapReach(0.3, 6.0);
        assertTrue(reach.admits(0.3));
        assertTrue(reach.admits(6.0));
        assertFalse(reach.admits(0.29));
        assertFalse(reach.admits(6.01));
    }
}
