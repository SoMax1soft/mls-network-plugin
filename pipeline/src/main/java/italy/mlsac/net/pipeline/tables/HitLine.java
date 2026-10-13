// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import italy.mlsac.net.pipeline.rows.ClickColumns;
import italy.mlsac.net.pipeline.rows.ClickRow;

public final class HitLine {
    private HitLine() {
    }

    public static void write(ClickRow row, RowFields out) {
        ClickColumns columns = new ClickColumns(row);
        var timing = row.timing();
        var target = row.target();
        var lens = row.measurement().lens();
        var history = row.history();
        var attacker = row.attacker();
        var posture = attacker.posture();
        out.integer(timing.row()).integer(timing.segmentOrdinal()).integer(timing.gameTick())
                .integer(timing.nanosIntoRecording()).number(timing.resolveMillis()).integer(timing.rotationSource())
                .integer(target.entityNumber()).flag(target.cancelled()).flag(target.targetIsPlayer())
                .flag(target.crosshairVictim()).flag(columns.targetValid())
                .integer(lens.mode().code()).number(lens.ping()).integer(lens.outstanding())
                .flag(columns.crosshairOnBox()).flag(columns.crosshairOnTightBox()).integer(columns.eyeRaysLanded())
                .flag(columns.mainEyeHit()).number(columns.impactOffsetX()).number(columns.impactOffsetY())
                .number(columns.impactOffsetZ()).number(columns.nearMissGap()).number(columns.offsetInHalfWidths())
                .number(columns.yawOffsetScaled()).number(columns.pitchOffsetScaled()).number(columns.inflation())
                .integer(row.occlusion().rayOccluded()).integer(row.occlusion().centerOccluded())
                .number(row.occlusion().exposure()).number(row.occlusion().wallMargin())
                .number(columns.targetDistance()).number(row.aim().angle()).number(row.aim().lead())
                .number(row.aim().verticalLead())
                .integer(row.onTarget().ticks()).number(row.onTarget().millis())
                .number(history.msSinceLastAttack()).integer(history.rowsSinceLastAttack())
                .integer(history.swingsSinceLastAttack()).integer(history.airSwingsSinceLastAttack())
                .integer(history.slotChangesSinceLastAttack()).integer(history.rowsAfterLastSwing())
                .number(history.millisAfterSwing())
                .integer(attacker.weapon()).text(attacker.handMaterial()).number(attacker.swingsPerSecond())
                .integer(attacker.modeOfPlay()).number(attacker.reach()).number(row.eyeToBoxGap())
                .number(attacker.swingCharge()).number(attacker.fallBlocks()).flag(attacker.grounded())
                .flag(posture.sprinting()).flag(posture.sneaking()).flag(posture.climbing())
                .flag(posture.submerged()).flag(posture.blind()).flag(posture.riding()).flag(attacker.critical())
                .single(row.yaw()).single(row.pitch()).number(row.yawTurn()).number(row.pitchTurn());
    }
}
