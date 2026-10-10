// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import java.util.EnumMap;
import java.util.Map;

public final class LagTable {

    private final Map<LagLevel, DelayStrength> delay = new EnumMap<>(LagLevel.class);
    private final Map<LagLevel, MissStrength> miss = new EnumMap<>(LagLevel.class);
    private final Map<LagLevel, SetbackStrength> setback = new EnumMap<>(LagLevel.class);
    private final Map<LagLevel, GroundStrength> ground = new EnumMap<>(LagLevel.class);
    private final Map<LagLevel, RotateStrength> rotate = new EnumMap<>(LagLevel.class);

    private LagTable() {
    }

    public static LagTable standard() {
        LagTable table = new LagTable();
        table.row(LagLevel.LIGHT, new DelayStrength(100), MissStrength.capped(0.50, 3),
                new SetbackStrength(new IntervalRange(3000, 5000), 3, new SnapReach(0.3, 3.0), Pursuit.DURING_FIGHTS),
                new GroundStrength(0.10, 10, 10, -0.20),
                new RotateStrength(new IntervalRange(3000, 5000), 10, 20, 3, Pursuit.DURING_FIGHTS));
        table.row(LagLevel.MEDIUM, new DelayStrength(250), MissStrength.capped(0.75, 8),
                new SetbackStrength(new IntervalRange(1500, 3000), 5, new SnapReach(0.3, 4.0), Pursuit.DURING_FIGHTS),
                new GroundStrength(0.00, 4, 3, -0.30),
                new RotateStrength(new IntervalRange(1500, 3000), 20, 45, 6, Pursuit.DURING_FIGHTS));
        table.row(LagLevel.HEAVY, new DelayStrength(500), MissStrength.uncapped(1.00),
                new SetbackStrength(new IntervalRange(500, 1000), 10, new SnapReach(0.3, 6.0), Pursuit.RELENTLESS),
                new GroundStrength(-0.20, 2, 1, -0.50),
                new RotateStrength(new IntervalRange(500, 1500), 45, 90, 10, Pursuit.RELENTLESS));
        return table;
    }

    private void row(LagLevel level, DelayStrength delayRow, MissStrength missRow, SetbackStrength setbackRow,
                     GroundStrength groundRow, RotateStrength rotateRow) {
        delay.put(level, delayRow);
        miss.put(level, missRow);
        setback.put(level, setbackRow);
        ground.put(level, groundRow);
        rotate.put(level, rotateRow);
    }

    public DelayStrength delay(LagLevel level) {
        return delay.get(level);
    }

    public MissStrength miss(LagLevel level) {
        return miss.get(level);
    }

    public SetbackStrength setback(LagLevel level) {
        return setback.get(level);
    }

    public GroundStrength ground(LagLevel level) {
        return ground.get(level);
    }

    public RotateStrength rotate(LagLevel level) {
        return rotate.get(level);
    }
}
