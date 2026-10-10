// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.numeric.AngleUnits;
import italy.mlsac.net.pipeline.numeric.AngleWrap;
import italy.mlsac.net.pipeline.rows.ClickColumns;
import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.TickColumns;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UltraFeatures {
    public static final int NUMERIC_COLUMNS = 54;
    public static final int BINARY_COLUMNS = 18;
    private static final int ASSIST_AT = 44;
    private static final int STRIKE_AT = 50;
    private static final int ASSIST_FLAGS_AT = 8;
    private static final int STRIKE_FLAGS_AT = 14;

    private UltraFeatures() {
    }

    static void fill(StretchEntry entry, StretchEntry previous, double[] features, double[] flags,
            float[] featureRow, float[] flagRow) {
        TickColumns row = new TickColumns(entry.row());
        TickColumns prev = previous == null ? null : new TickColumns(previous.row());
        TickRow tick = entry.row();
        List<ClickColumns> clicks = validClicks(entry.clicks());
        double yawSensStep = tick.rotation().yawSensStep() > 0 ? tick.rotation().yawSensStep() : 1.0;
        double pitchSensStep = tick.rotation().pitchSensStep() > 0 ? tick.rotation().pitchSensStep() : 1.0;
        boolean same = row.targetValid() && prev != null && prev.targetValid() && !tick.target().changed();
        Arrays.fill(features, 0.0);
        Arrays.fill(flags, 0.0);
        features[0] = tick.rotation().yawTurn() / yawSensStep;
        features[1] = tick.rotation().pitchTurn() / pitchSensStep;
        features[2] = tick.rotation().yawTurnChange() / yawSensStep;
        features[3] = tick.rotation().pitchTurnChange() / pitchSensStep;
        features[4] = tick.rotation().yawTurnJolt() / yawSensStep;
        features[5] = tick.rotation().pitchTurnJolt() / pitchSensStep;
        features[6] = tick.rotation().yawDivisorResidue()
                / Math.max(Math.ulp(Math.abs(tick.packet().yaw())), 1e-12);
        features[7] = tick.rotation().pitchDivisorResidue() / pitchSensStep;
        features[8] = flag(tick.packet().rotationPresent());
        features[9] = tick.aim().angle();
        features[10] = tick.aim().lead();
        features[11] = tick.aim().verticalLead();
        features[12] = same ? AngleWrap.FLOOR.apply(tick.aim().lead() - previous.row().aim().lead()) : 0.0;
        features[13] = row.yawOffsetScaled();
        features[14] = row.pitchOffsetScaled();
        features[15] = flag(row.mainEyeHit());
        features[16] = row.eyeRaysLanded() / 9.0;
        features[17] = Math.min(tick.onTarget().ticks(), 40) / 40.0;
        features[18] = same ? AngleWrap.FLOOR.apply(entry.bodyBearing() - previous.bodyBearing()) : 0.0;
        double reach = tick.self().reach() > 0 ? tick.self().reach() : 3.0;
        features[19] = row.targetDistance() / reach;
        features[20] = row.inflation();
        features[21] = flag(tick.target().changed());
        features[22] = tick.inputs().attacks();
        features[23] = tick.inputs().swings();
        features[24] = tick.inputs().airSwings();
        double swingsPerSecond = tick.self().swingsPerSecond();
        int rowsAfterLastSwing = tick.inputs().rowsAfterLastSwing();
        features[25] = swingsPerSecond > 0 ? Math.min(1, (rowsAfterLastSwing + 0.5) * swingsPerSecond / 20) : 0.0;
        features[26] = Math.min(rowsAfterLastSwing, 40) / 40.0;
        clickFeatures(features, clicks);
        double exposure = tick.occlusion().exposure();
        features[32] = Math.max(0, exposure);
        features[33] = flag(row.mainEyeHit() && exposure == 0);
        features[34] = StrictMath.hypot(tick.move().x(), tick.move().z());
        features[35] = tick.move().y();
        features[36] = flag(tick.packet().grounded());
        features[37] = flag(tick.self().sprinting());
        features[38] = flag(tick.self().sneaking());
        features[39] = entry.memory().air200();
        features[40] = entry.memory().hits200();
        features[41] = entry.memory().air600();
        features[42] = entry.memory().hits600();
        features[43] = entry.memory().missShare();
        entry.assist().copyInto(features, ASSIST_AT);
        entry.strike().copyInto(features, STRIKE_AT);
        flags[0] = flag(tick.rotation().valid());
        flags[1] = flag(tick.rotation().yawSensKnown() && tick.rotation().pitchSensKnown() && tick.rotation().valid());
        flags[2] = flag(row.targetValid());
        flags[3] = flag(tick.move().valid());
        flags[4] = flag(swingsPerSecond > 0 && rowsAfterLastSwing < 255);
        flags[5] = flag(!clicks.isEmpty());
        flags[6] = flag(row.targetValid() && exposure >= 0);
        flags[7] = flag(entry.memory().valid());
        for (int index = 0; index < AssistReadout.SIGNALS; index++) {
            flags[ASSIST_FLAGS_AT + index] = flag(Double.isFinite(features[ASSIST_AT + index]));
        }
        for (int index = 0; index < StrikeReadout.SIGNALS; index++) {
            flags[STRIKE_FLAGS_AT + index] = flag(Double.isFinite(features[STRIKE_AT + index]));
        }
        toSingle(features, featureRow);
        toSingle(flags, flagRow);
    }

    private static void clickFeatures(double[] features, List<ClickColumns> clicks) {
        if (clicks.isEmpty()) {
            return;
        }
        double primary = 0.0;
        int rays = 0;
        double relativeY = 0.0;
        boolean throughWall = false;
        double miss = Double.POSITIVE_INFINITY;
        for (ClickColumns click : clicks) {
            primary = Math.max(primary, flag(click.mainEyeHit()));
            rays = Math.max(rays, click.eyeRaysLanded());
            relativeY += click.impactOffsetY();
            throughWall |= click.row().occlusion().rayOccluded() == 1;
            miss = Math.min(miss, click.nearMissGap());
        }
        features[27] = primary;
        features[28] = rays / 9.0;
        features[29] = relativeY / clicks.size();
        features[30] = flag(throughWall);
        features[31] = miss;
    }

    private static List<ClickColumns> validClicks(List<ClickRow> clicks) {
        if (clicks.isEmpty()) {
            return List.of();
        }
        List<ClickColumns> valid = new ArrayList<>(clicks.size());
        for (ClickRow click : clicks) {
            ClickColumns columns = new ClickColumns(click);
            if (columns.targetValid()) {
                valid.add(columns);
            }
        }
        return valid;
    }

    static double desired(TickRow row) {
        return AngleUnits.degrees(Math.atan2(-row.aim().relative().x(), row.aim().relative().z()));
    }

    private static double flag(boolean value) {
        return value ? 1.0 : 0.0;
    }

    private static void toSingle(double[] values, float[] single) {
        for (int index = 0; index < values.length; index++) {
            single[index] = Double.isFinite(values[index]) ? (float) values[index] : 0.0f;
        }
    }
}
