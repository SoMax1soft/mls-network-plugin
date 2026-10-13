// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import italy.mlsac.net.pipeline.rows.TickColumns;
import italy.mlsac.net.pipeline.rows.TickRow;

public final class TickLine {
    private TickLine() {
    }

    public static void write(TickRow row, RowFields out) {
        TickColumns columns = new TickColumns(row);
        var timing = row.timing();
        var packet = row.packet();
        var step = row.rotation();
        var inputs = row.inputs();
        var self = row.self();
        var target = row.target();
        var lens = row.measurement().lens();
        var aim = row.aim();
        out.integer(timing.row()).integer(timing.segmentOrdinal()).integer(timing.gameTick())
                .integer(timing.nanosIntoRecording()).number(timing.dtMillis()).integer(timing.tickEnds())
                .flag(timing.engagedInFight()).integer(timing.ticksAfterHit()).number(timing.millisAfterHit())
                .flag(packet.rotationPresent()).flag(packet.positionPresent()).flag(packet.grounded())
                .flag(packet.cancelled()).flag(packet.echo()).flag(packet.noPacket())
                .single(packet.yaw()).single(packet.pitch())
                .flag(step.valid()).number(step.yawTurn()).number(step.pitchTurn()).number(step.yawTurnChange())
                .number(step.pitchTurnChange()).number(step.yawTurnJolt()).number(step.pitchTurnJolt())
                .number(step.yawSensStep()).number(step.pitchSensStep()).flag(step.yawSensKnown())
                .flag(step.pitchSensKnown()).number(step.yawStepsTaken()).number(step.pitchStepsTaken())
                .number(step.yawDivisorResidue()).number(step.pitchDivisorResidue())
                .flag(row.move().valid()).number(row.move().x()).number(row.move().y()).number(row.move().z())
                .integer(inputs.attacks()).integer(inputs.hitsOnPlayers()).integer(inputs.swings())
                .integer(inputs.airSwings()).integer(inputs.slotChanges()).integer(inputs.rowsAfterLastSwing())
                .flag(self.valid()).number(self.swingCharge()).number(self.fallBlocks())
                .flag(self.sprinting()).flag(self.sneaking()).integer(self.weapon()).text(self.handMaterial())
                .number(self.swingsPerSecond()).integer(self.modeOfPlay()).number(self.reach())
                .flag(columns.targetValid()).integer(target.source()).flag(target.changed())
                .number(target.lockedOnMillis()).integer(target.inRange())
                .integer(lens.mode().code()).number(lens.ping()).integer(lens.outstanding())
                .flag(columns.crosshairOnBox()).flag(columns.crosshairOnTightBox()).integer(columns.eyeRaysLanded())
                .flag(columns.mainEyeHit()).number(columns.impactOffsetX()).number(columns.impactOffsetY())
                .number(columns.impactOffsetZ()).number(columns.nearMissGap()).number(columns.offsetInHalfWidths())
                .number(columns.yawOffsetScaled()).number(columns.pitchOffsetScaled()).number(columns.inflation())
                .integer(row.occlusion().rayOccluded()).integer(row.occlusion().centerOccluded())
                .number(row.occlusion().exposure()).number(columns.targetDistance())
                .number(aim.angle()).number(aim.lead()).number(aim.verticalLead())
                .number(aim.relative().x()).number(aim.relative().y()).number(aim.relative().z())
                .integer(row.onTarget().ticks()).number(row.onTarget().millis());
    }
}
