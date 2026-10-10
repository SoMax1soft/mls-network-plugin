// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

import italy.mlsac.net.pipeline.numeric.AngleUnits;
import italy.mlsac.net.pipeline.numeric.AngleWrap;

public record AimRelation(double angle, double lead, double verticalLead, Vec3 relative) {
    public static final AimRelation NONE = new AimRelation(0.0, 0.0, 0.0, Vec3.ZERO);

    private static final double DEGENERATE_LENGTH = 1e-6;

    public static AimRelation between(Vec3 eye, Box tight, double yaw, double pitch) {
        Vec3 toCentre = tight.centre().minus(eye);
        double length = toCentre.length();
        if (length < DEGENERATE_LENGTH) {
            return new AimRelation(0.0, 0.0, 0.0, toCentre);
        }
        Vec3 direction = LookDirection.of(yaw, pitch);
        double cosine = Math.max(-1.0, Math.min(1.0, direction.dot(toCentre) / length));
        double angle = AngleUnits.degrees(Math.acos(cosine));
        double lead = AngleWrap.REMAINDER.apply(yaw - AngleUnits.degrees(Math.atan2(-toCentre.x(), toCentre.z())));
        double verticalLead = pitch - AngleUnits.degrees(-Math.atan2(toCentre.y(), toCentre.horizontalLength()));
        return new AimRelation(angle, lead, verticalLead, toCentre);
    }
}
