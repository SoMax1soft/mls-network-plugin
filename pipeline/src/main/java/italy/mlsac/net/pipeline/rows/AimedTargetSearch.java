// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.geometry.LookDirection;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.numeric.AngleUnits;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.SampleBoard;
import italy.mlsac.net.pipeline.player.SampleVisitor;

final class AimedTargetSearch implements SampleVisitor {
    private static final double NEAR_FIELD_BLOCKS = 6.0;
    private static final double SEARCH_RADIUS = 8.0;
    private static final double OVERLAP_RADIUS = 1e-6;
    private static final double HALF_VIEW_CONE = 90.0;

    private final AttackMemory memory;
    private PlayerSample viewer;
    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private double facingX;
    private double facingY;
    private double facingZ;
    private long now;
    private int crowd;
    private PlayerSample recentVictim;
    private PlayerSample nearestToCrosshair;
    private double nearestAngle;

    AimedTargetSearch(AttackMemory memory) {
        this.memory = memory;
    }

    TargetChoice run(PlayerSample viewer, float yaw, float pitch, SampleBoard board, long now) {
        if (viewer == null) {
            return TargetChoice.NONE;
        }
        begin(viewer, LookDirection.of(yaw, pitch), now);
        if (Double.isFinite(eyeX) && Double.isFinite(eyeZ)) {
            board.visitNear(viewer.world(), eyeX, eyeZ, SEARCH_RADIUS, this);
        } else {
            for (PlayerSample other : board.everyone()) {
                visit(other);
            }
        }
        TargetChoice outcome = outcome();
        this.viewer = null;
        recentVictim = null;
        nearestToCrosshair = null;
        return outcome;
    }

    private void begin(PlayerSample viewer, Vec3 facing, long now) {
        this.viewer = viewer;
        this.eyeX = viewer.x();
        this.eyeY = viewer.y() + viewer.eyeLift();
        this.eyeZ = viewer.z();
        this.facingX = facing.x();
        this.facingY = facing.y();
        this.facingZ = facing.z();
        this.now = now;
        this.crowd = 0;
        this.recentVictim = null;
        this.nearestToCrosshair = null;
        this.nearestAngle = Double.POSITIVE_INFINITY;
    }

    @Override
    public void visit(PlayerSample other) {
        if (!sharesWorldWith(other)) {
            return;
        }
        double towardX = other.x() - eyeX;
        double towardY = (other.y() + other.height() / 2.0) - eyeY;
        double towardZ = other.z() - eyeZ;
        double reach = Math.sqrt(towardX * towardX + towardY * towardY + towardZ * towardZ);
        if (reach <= NEAR_FIELD_BLOCKS) {
            crowd++;
        }
        if (reach > SEARCH_RADIUS || reach < OVERLAP_RADIUS) {
            return;
        }
        if (recentVictim == null && memory.remembers(other.player(), now)) {
            recentVictim = other;
            return;
        }
        double cosine = Math.max(-1.0, Math.min(1.0,
                (facingX * towardX + facingY * towardY + facingZ * towardZ) / reach));
        compareByAngle(other, AngleUnits.degrees(Math.acos(cosine)));
    }

    private boolean sharesWorldWith(PlayerSample other) {
        return !other.player().equals(viewer.player()) && other.freshAt(now) && other.world().equals(viewer.world());
    }

    private void compareByAngle(PlayerSample other, double angle) {
        if (angle > HALF_VIEW_CONE) {
            return;
        }
        boolean closer = nearestToCrosshair == null || angle < nearestAngle
                || (angle == nearestAngle && other.entityNumber() < nearestToCrosshair.entityNumber());
        if (closer) {
            nearestToCrosshair = other;
            nearestAngle = angle;
        }
    }

    private TargetChoice outcome() {
        if (recentVictim != null) {
            return new TargetChoice(recentVictim, TargetChoice.FROM_LAST_ATTACK, crowd);
        }
        if (nearestToCrosshair != null) {
            return new TargetChoice(nearestToCrosshair, TargetChoice.FROM_VIEW, crowd);
        }
        return new TargetChoice(null, TargetChoice.FROM_NOTHING, crowd);
    }
}
