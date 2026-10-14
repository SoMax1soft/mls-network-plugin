// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.LookPair;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.occlusion.WallCheckRequest;
import italy.mlsac.net.pipeline.occlusion.OcclusionKind;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.console.DebugTally;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ClickResolver {
    private static final int PENDING_LIMIT = 256;
    private static final double[] EYE_OFFSETS = {1.62, 1.27, 0.4};

    private final UUID player;
    private final long start;
    private final ClientSightRewind lens;
    private final OcclusionQueue occlusion;
    private final RowSink sink;
    private final List<PendingClick> pending = new ArrayList<>();

    ClickResolver(UUID player, long start, ClientSightRewind lens, OcclusionQueue occlusion, RowSink sink) {
        this.player = player;
        this.start = start;
        this.lens = lens;
        this.occlusion = occlusion;
        this.sink = sink;
    }

    void add(PendingClick click) {
        if (pending.size() >= PENDING_LIMIT) {
            pending.remove(0);
            DebugTally.count("drop.pending-click-overflow");
        }
        pending.add(click);
    }

    void resolveWithRow(RowContext row) {
        for (PendingClick click : pending) {
            boolean crosshairVictim = click.target() != null && row.crosshairVictim() != null
                    && row.crosshairVictim().player().equals(click.target().player());
            PlayerSample from = row.eye() != null ? row.eye() : click.backupEyeSample();
            TargetMeasurement measurement = TargetMeasurement.NONE;
            AimRelation aim = AimRelation.NONE;
            if (crosshairVictim) {
                measurement = row.measurement();
                aim = row.aim();
            } else if (click.target() != null && from != null && row.look() != null) {
                measurement = lens.measure(player, from, click.target(), row.now(), row.look());
                aim = aimOf(measurement, from, row.yaw(), row.pitch());
            }
            PlayerSample previousEye = from == row.eye() ? row.previousEye() : null;
            ClickTiming timing = new ClickTiming(row.rowOrdinal(), row.segmentOrdinal(), row.gameTick(),
                    click.nanos() - start, (row.now() - click.nanos()) / 1e6, row.rotationSource());
            emit(click, timing, crosshairVictim, measurement, aim, crosshairVictim ? row.onTarget() : OnTarget.NONE, from,
                    previousEye, row.yaw(), row.pitch(), row.step().yawTurn(), row.step().pitchTurn());
        }
        pending.clear();
    }

    void resolveWithoutRow(long segmentOrdinal, long gameTick, LookPair look, float yaw, float pitch) {
        for (PendingClick click : pending) {
            PlayerSample from = click.backupEyeSample();
            TargetMeasurement measurement = TargetMeasurement.NONE;
            AimRelation aim = AimRelation.NONE;
            if (click.target() != null && from != null && look != null) {
                measurement = lens.measure(player, from, click.target(), click.nanos(), look);
                aim = aimOf(measurement, from, yaw, pitch);
            }
            ClickTiming timing = new ClickTiming(-1, segmentOrdinal, gameTick, click.nanos() - start, -1.0, 0);
            emit(click, timing, false, measurement, aim, OnTarget.NONE, from, null, yaw, pitch, 0.0, 0.0);
        }
        pending.clear();
    }

    private void emit(PendingClick click, ClickTiming timing, boolean crosshairVictim, TargetMeasurement measurement,
            AimRelation aim, OnTarget onTarget, PlayerSample from, PlayerSample previousEye, float yaw, float pitch,
            double yawTurn, double pitchTurn) {
        OcclusionCell cell = new OcclusionCell();
        ClickRow row = new ClickRow(timing,
                new ClickTarget(click.entityNumber(), click.cancelled(), click.target() != null, crosshairVictim),
                measurement, cell, aim, onTarget, click.history(), AttackerState.of(click.own(), click.grounded()),
                eyeToBoxGap(from, previousEye, measurement), yaw, pitch, yawTurn, pitchTurn);
        if (from != null && measurement.valid()) {
            occlusion.enqueue(WallCheckRequest.of(OcclusionKind.CLICK, cell, from, previousEye,
                    measurement.lens().possible(), measurement.lens().tight(), yaw, pitch));
        }
        sink.clickRow(row);
    }

    private static AimRelation aimOf(TargetMeasurement measurement, PlayerSample from, float yaw, float pitch) {
        if (!measurement.valid()) {
            return AimRelation.NONE;
        }
        return AimRelation.between(from.eye(), measurement.lens().tight(), yaw, pitch);
    }

    private static double eyeToBoxGap(PlayerSample from, PlayerSample previousEye, TargetMeasurement measurement) {
        if (from == null || !measurement.valid()) {
            return -1.0;
        }
        Box box = measurement.lens().possible() != null ? measurement.lens().possible() : measurement.lens().tight();
        double nearest = Double.POSITIVE_INFINITY;
        for (PlayerSample origin : previousEye == null ? List.of(from) : List.of(from, previousEye)) {
            for (double offset : EYE_OFFSETS) {
                nearest = Math.min(nearest, box.gapTo(new Vec3(origin.x(), origin.y() + offset, origin.z())));
            }
        }
        return nearest;
    }
}
