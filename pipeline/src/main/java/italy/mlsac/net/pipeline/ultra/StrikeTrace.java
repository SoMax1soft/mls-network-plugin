// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public final class StrikeTrace {
    private static final int CALM_ROWS = 60;
    private static final int CALM_QUORUM = 12;
    private static final int KICK_ROWS = 2000;
    private static final int KICK_QUORUM = 3;
    private static final int ERROR_ROWS = 600;
    private static final int ATTACK_ERROR_QUORUM = 3;
    private static final int OPEN_ERROR_QUORUM = 20;
    private static final int SHORT_HOLD_ROWS = 200;
    private static final int LONG_HOLD_ROWS = 600;
    private static final int HOLD_QUORUM = 20;
    private static final double KICK_FLOOR = 0.1;
    private static final double ERROR_FLOOR = 0.05;
    private static final double LOG_LIMIT = 4.0;
    private static final int AT_ATTACK = 0;
    private static final int IN_THE_OPEN = 1;

    private final SlidingTable calm = new SlidingTable(CALM_ROWS, 1);
    private final SlidingTable kicks = new SlidingTable(KICK_ROWS, 1);
    private final SlidingTable errors = new SlidingTable(ERROR_ROWS, 2);
    private final SlidingTable shortHold = new SlidingTable(SHORT_HOLD_ROWS, 1);
    private final SlidingTable longHold = new SlidingTable(LONG_HOLD_ROWS, 1);
    private double lastMotion = Double.NaN;

    public void forgetShortTerm() {
        calm.empty();
        kicks.empty();
        errors.empty();
        shortHold.empty();
        longHold.empty();
        lastMotion = Double.NaN;
    }

    StrikeReadout observe(StrikeReading reading) {
        double motion = reading.motion();
        double peak = presentMax(motion, lastMotion);
        double before = calm.average(0, CALM_QUORUM);
        double kick = bounded(Math.log((peak + KICK_FLOOR) / (before + KICK_FLOOR)));
        calm.append(motion);
        lastMotion = motion;
        boolean attack = reading.attackOnTarget();
        kicks.append(attack ? kick : Double.NaN);
        double error = reading.aimError();
        errors.append(attack ? error : Double.NaN, reading.targetValid() && !attack ? error : Double.NaN);
        double onBody = reading.onBody();
        shortHold.append(onBody);
        longHold.append(onBody);
        double errorRatio = bounded(Math.log((errors.average(AT_ATTACK, ATTACK_ERROR_QUORUM) + ERROR_FLOOR)
                / (errors.average(IN_THE_OPEN, OPEN_ERROR_QUORUM) + ERROR_FLOOR)));
        return StrikeReadout.of(kicks.average(0, KICK_QUORUM), errorRatio, shortHold.average(0, HOLD_QUORUM),
                longHold.average(0, HOLD_QUORUM));
    }

    private static double presentMax(double current, double previous) {
        if (Double.isNaN(current)) {
            return previous;
        }
        if (Double.isNaN(previous)) {
            return current;
        }
        return Math.max(current, previous);
    }

    private static double bounded(double value) {
        return Math.max(-LOG_LIMIT, Math.min(LOG_LIMIT, value));
    }
}
