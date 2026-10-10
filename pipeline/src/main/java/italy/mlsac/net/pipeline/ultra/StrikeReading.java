// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.TickColumns;
import italy.mlsac.net.pipeline.rows.TickRow;

record StrikeReading(double turn, double turnPitch, boolean turnKnown, boolean targetValid, int attacks,
        double offYaw, double offPitch, boolean crosshairOnBody) {

    static StrikeReading from(TickColumns columns) {
        TickRow row = columns.row();
        return new StrikeReading(row.rotation().yawTurn(), row.rotation().pitchTurn(), row.rotation().valid(),
                columns.targetValid(), row.inputs().attacks(), columns.yawOffsetScaled(),
                columns.pitchOffsetScaled(), columns.mainEyeHit());
    }

    double motion() {
        return turnKnown ? Math.abs(turn) + Math.abs(turnPitch) : Double.NaN;
    }

    boolean attackOnTarget() {
        return attacks > 0 && targetValid;
    }

    double aimError() {
        return StrictMath.hypot(offYaw, offPitch);
    }

    double onBody() {
        if (!targetValid) {
            return Double.NaN;
        }
        return crosshairOnBody ? 1.0 : 0.0;
    }
}
