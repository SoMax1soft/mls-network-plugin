// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.CURVATURE;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.LAG;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.LAG_SQUARED;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.PULL;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.PULL_SQUARED;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.PULL_TIMES_LAG;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.SETTLED_CHANGE;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.SETTLED_SIZE;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.STEP_CHANGE;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.STEP_SIZE;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.TURN;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.TURN_SQUARED;
import static italy.mlsac.net.pipeline.ultra.CloseRangeColumn.TURN_TIMES_LAG;

import italy.mlsac.net.pipeline.numeric.AngleWrap;

public final class AimAssistTrace {
    private static final int CLOSE_RANGE_ROWS = 60;
    private static final int RELEASE_ROWS = 120;
    private static final int LIFETIME_ROWS = 600;
    private static final int CLOSE_QUORUM = 12;
    private static final int SETTLED_QUORUM = 20;
    private static final int RELEASE_QUORUM = 3;
    private static final double ON_AIM_DEGREES = 2.0;
    private static final double OFF_AIM_DEGREES = 2.5;
    private static final int FAR_CHANGE = 0;
    private static final int FAR_SIZE = 1;

    private final SlidingTable closeRange = new SlidingTable(CLOSE_RANGE_ROWS, CloseRangeColumn.COUNT);
    private final SlidingTable releases = new SlidingTable(RELEASE_ROWS, 1);
    private final SlidingTable lifetime = new SlidingTable(LIFETIME_ROWS, 2);
    private final double[] closeRangeScratch = new double[CloseRangeColumn.COUNT];
    private LastAppended last = LastAppended.BLANK;
    private long rowsInStretch;

    public void forgetShortTerm() {
        closeRange.empty();
        releases.empty();
        rowsInStretch = 0;
    }

    AssistReadout observe(AssistReading reading) {
        long k = rowsInStretch;
        boolean hasLast = k >= 1;
        double turn = reading.turn();
        boolean same = reading.bothValid() && hasLast && last.bothValid() && reading.sameTarget();
        boolean onAim = same && reading.within(ON_AIM_DEGREES);
        boolean steady = reading.bothValid() && hasLast && last.targetValid() && reading.sameTarget();
        boolean settled = steady && k >= 3 && last.steadyThreeInARow() && reading.within(ON_AIM_DEGREES);
        boolean wandering = steady && reading.beyond(OFF_AIM_DEGREES);
        double stepChange = hasLast ? Math.abs(turn - last.turn()) : Double.NaN;
        closeRange.append(closeRangeRow(reading, k, onAim, settled, stepChange));
        double lifetimeRatio = settledRatio(lifetime, FAR_CHANGE, FAR_SIZE);
        lifetime.append(wandering ? stepChange : Double.NaN, wandering ? Math.abs(turn) : Double.NaN);
        double viewYaw = AngleWrap.SHIFTED_REMAINDER.apply(reading.yaw() - reading.lead());
        double viewPitch = reading.pitch() - reading.verticalLead();
        double relativeMotion = relativeMotion(reading, same, viewYaw, viewPitch);
        boolean onBody = reading.crosshairOnBody() && reading.bothValid();
        boolean leftBody = !onBody && hasLast && last.onBody() && same;
        double growth = hasLast
                ? bounded(Math.log((relativeMotion + 0.1) / (last.relativeMotion() + 0.1)), -4, 4) : Double.NaN;
        releases.append(leftBody ? growth : Double.NaN);
        AssistReadout readout = readout(lifetimeRatio);
        last = new LastAppended(turn, last.turn(), reading.lead(), viewYaw, viewPitch, relativeMotion,
                reading.targetValid(), reading.bothValid(), onBody, last.steadyBitsWith(steady));
        rowsInStretch++;
        return readout;
    }

    private double[] closeRangeRow(AssistReading reading, long k, boolean onAim, boolean settled,
            double stepChange) {
        double turn = reading.turn();
        double lag = onAim ? -last.lead() : Double.NaN;
        double turnValue = onAim ? turn : Double.NaN;
        double pull = onAim ? turn - last.turn() : Double.NaN;
        double[] row = closeRangeScratch;
        row[STEP_CHANGE.at()] = onAim ? stepChange : Double.NaN;
        row[STEP_SIZE.at()] = onAim ? Math.abs(turn) : Double.NaN;
        row[CURVATURE.at()] = onAim && k >= 2 ? Math.abs(turn - 2 * last.turn() + last.turnBefore()) : Double.NaN;
        row[TURN.at()] = turnValue;
        row[LAG.at()] = lag;
        row[TURN_TIMES_LAG.at()] = turnValue * lag;
        row[TURN_SQUARED.at()] = turnValue * turnValue;
        row[LAG_SQUARED.at()] = lag * lag;
        row[PULL.at()] = pull;
        row[PULL_TIMES_LAG.at()] = pull * lag;
        row[PULL_SQUARED.at()] = pull * pull;
        row[SETTLED_CHANGE.at()] = settled ? stepChange : Double.NaN;
        row[SETTLED_SIZE.at()] = settled ? Math.abs(turn) : Double.NaN;
        return row;
    }

    private double relativeMotion(AssistReading reading, boolean same, double viewYaw, double viewPitch) {
        if (!same) {
            return Double.NaN;
        }
        double targetYaw = AngleWrap.SHIFTED_REMAINDER.apply(viewYaw - last.viewYaw());
        double targetPitch = viewPitch - last.viewPitch();
        if (Double.isNaN(targetYaw) || Double.isNaN(targetPitch)) {
            return Double.NaN;
        }
        return StrictMath.hypot(reading.turn() - targetYaw, reading.turnPitch() - targetPitch);
    }

    private AssistReadout readout(double lifetimeRatio) {
        double scale = Math.max(closeMean(STEP_SIZE), 0.1);
        double roughness = bounded(closeMean(STEP_CHANGE) / scale, 0, 10);
        double curvature = bounded(closeMean(CURVATURE) / scale, 0, 10);
        double control = linkage(TURN, TURN_TIMES_LAG, TURN_SQUARED);
        double pull = linkage(PULL, PULL_TIMES_LAG, PULL_SQUARED);
        double settledRatio = settledRatio(closeRange, SETTLED_CHANGE.at(), SETTLED_SIZE.at());
        double personal = bounded(Math.log((settledRatio + 0.05) / (lifetimeRatio + 0.05)), -4, 4);
        double release = releases.average(0, RELEASE_QUORUM);
        return AssistReadout.of(roughness, curvature, control, pull, personal, release);
    }

    private double linkage(CloseRangeColumn motion, CloseRangeColumn crossed, CloseRangeColumn motionSquared) {
        double motionMean = closeMean(motion);
        double lagMean = closeMean(LAG);
        double covariance = closeMean(crossed) - motionMean * lagMean;
        double motionSpread = Math.max(closeMean(motionSquared) - motionMean * motionMean, 1e-8);
        double lagSpread = Math.max(closeMean(LAG_SQUARED) - lagMean * lagMean, 1e-8);
        return bounded(covariance / Math.sqrt(motionSpread * lagSpread), -1, 1);
    }

    private double closeMean(CloseRangeColumn column) {
        return closeRange.average(column.at(), CLOSE_QUORUM);
    }

    private static double settledRatio(SlidingTable table, int change, int size) {
        return table.average(change, SETTLED_QUORUM) / Math.max(table.average(size, SETTLED_QUORUM), 0.1);
    }

    private static double bounded(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
