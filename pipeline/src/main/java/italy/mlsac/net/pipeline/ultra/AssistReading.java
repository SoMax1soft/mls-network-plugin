// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.TickColumns;
import italy.mlsac.net.pipeline.rows.TickRow;

record AssistReading(double turn, double turnPitch, double lead, double verticalLead, double offYaw,
        double offPitch, boolean targetValid, boolean turnKnown, boolean sameTarget, boolean crosshairOnBody,
        double yaw, double pitch) {

    static AssistReading from(TickColumns columns) {
        TickRow row = columns.row();
        return new AssistReading(row.rotation().yawTurn(), row.rotation().pitchTurn(), row.aim().lead(),
                row.aim().verticalLead(), Math.abs(columns.yawOffsetScaled()), Math.abs(columns.pitchOffsetScaled()),
                columns.targetValid(), row.rotation().valid(), !row.target().changed(), columns.mainEyeHit(),
                row.packet().yaw(), row.packet().pitch());
    }

    boolean bothValid() {
        return targetValid && turnKnown;
    }

    boolean within(double limit) {
        return offYaw <= limit && offPitch <= limit;
    }

    boolean beyond(double limit) {
        return offYaw > limit || offPitch > limit;
    }
}
