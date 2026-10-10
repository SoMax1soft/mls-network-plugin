// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

import italy.mlsac.net.pipeline.numeric.AngleUnits;
import italy.mlsac.net.pipeline.numeric.AngleWrap;

public final class HitMeasurer {
    private static final double[] EYE_OFFSETS = {1.62, 1.27, 0.4};
    private static final double MINIMUM_CENTRE_DISTANCE = 0.001;
    private static final double MINIMUM_ANGULAR_SIZE = 1e-6;
    private static final int LOOKS = 3;

    private HitMeasurer() {
    }

    public static HitMeasurement measure(Vec3 feet, double eyeLift, LookPair look, Box possible, Box tight) {
        if (possible == null || tight == null || !Double.isFinite(look.yaw()) || !Double.isFinite(look.pitch())) {
            return HitMeasurement.INVALID;
        }
        double yawAtLastPacket = Double.isFinite(look.yawAtLastPacket()) ? look.yawAtLastPacket() : look.yaw();
        double pitchAtLastPacket = Double.isFinite(look.pitchAtLastPacket()) ? look.pitchAtLastPacket() : look.pitch();
        Vec3 centre = tight.centre();
        int eyeRaysLanded = 0;
        boolean strict = false;
        Vec3 hitPoint = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        double bestDistance = 0.0;
        for (int lookIndex = 0; lookIndex < LOOKS; lookIndex++) {
            Vec3 direction = LookDirection.of(lookIndex == 0 ? look.yaw() : yawAtLastPacket,
                    lookIndex == LOOKS - 1 ? pitchAtLastPacket : look.pitch());
            for (double offset : EYE_OFFSETS) {
                double originY = feet.y() + offset;
                if (RayCast.hits(possible, feet.x(), originY, feet.z(), direction)) {
                    eyeRaysLanded++;
                }
                if (hitPoint == null) {
                    Vec3 tightEntry = RayCast.entryOrNull(tight, new Vec3(feet.x(), originY, feet.z()), direction);
                    if (tightEntry != null) {
                        strict = true;
                        hitPoint = tightEntry;
                    }
                } else if (RayCast.hits(tight, feet.x(), originY, feet.z(), direction)) {
                    strict = true;
                }
                double towardX = centre.x() - feet.x();
                double towardY = centre.y() - originY;
                double towardZ = centre.z() - feet.z();
                double length = Math.sqrt(towardX * towardX + towardY * towardY + towardZ * towardZ);
                if (!(length > MINIMUM_CENTRE_DISTANCE)) {
                    continue;
                }
                double cosine = Math.max(-1.0, Math.min(1.0,
                        (direction.x() * towardX + direction.y() * towardY + direction.z() * towardZ) / length));
                double angle = AngleUnits.degrees(Math.acos(cosine));
                if (angle < bestAngle) {
                    bestAngle = angle;
                    bestDistance = length;
                }
            }
        }
        Vec3 primaryEye = new Vec3(feet.x(), feet.y() + eyeLift, feet.z());
        Vec3 primaryDirection = LookDirection.of(look.yaw(), look.pitch());
        Vec3 primaryTight = RayCast.entryOrNull(tight, primaryEye, primaryDirection);
        if (primaryTight != null) {
            hitPoint = primaryTight;
        }
        double offsetInHalfWidths = offsetInHalfWidths(tight, bestAngle, bestDistance);
        double[] axes = axisOffsets(tight, primaryEye, look.yaw(), look.pitch());
        double inflation = Math.max(0.0, possible.sizeX() - tight.sizeX())
                + Math.max(0.0, possible.sizeY() - tight.sizeY())
                + Math.max(0.0, possible.sizeZ() - tight.sizeZ());
        boolean mainEyeHit = RayCast.hits(possible, primaryEye, primaryDirection);
        Vec3 relative = hitPoint == null ? Vec3.ZERO : hitPoint.minus(centre);
        double miss = strict ? 0.0 : closestMiss(feet, tight, look.yaw(), look.pitch(), yawAtLastPacket,
                pitchAtLastPacket);
        double nearMissGap = Double.isInfinite(miss) ? 0.0 : miss;
        return new HitMeasurement(true, eyeRaysLanded > 0, strict, eyeRaysLanded, mainEyeHit, relative, nearMissGap, offsetInHalfWidths,
                axes[0], axes[1], inflation, tight.gapTo(primaryEye));
    }

    private static double closestMiss(Vec3 feet, Box tight, double yaw, double pitch, double yawAtLastPacket,
            double pitchAtLastPacket) {
        double miss = Double.POSITIVE_INFINITY;
        for (int lookIndex = 0; lookIndex < LOOKS; lookIndex++) {
            Vec3 direction = LookDirection.of(lookIndex == 0 ? yaw : yawAtLastPacket,
                    lookIndex == LOOKS - 1 ? pitchAtLastPacket : pitch);
            for (double offset : EYE_OFFSETS) {
                if (miss > 0.0) {
                    miss = Math.min(miss, RayCast.miss(tight, new Vec3(feet.x(), feet.y() + offset, feet.z()),
                            direction));
                }
            }
        }
        return miss;
    }

    private static double offsetInHalfWidths(Box tight, double bestAngle, double bestDistance) {
        if (Double.isInfinite(bestAngle)) {
            return 0.0;
        }
        double half = Math.max(tight.sizeX(), Math.max(tight.sizeY(), tight.sizeZ())) / 2.0;
        double radius = AngleUnits.degrees(Math.atan2(half, bestDistance));
        if (radius <= MINIMUM_ANGULAR_SIZE) {
            return 0.0;
        }
        return bestAngle / radius;
    }

    private static double[] axisOffsets(Box tight, Vec3 eye, double yaw, double pitch) {
        Vec3 toCentre = tight.centre().minus(eye);
        double length = toCentre.length();
        if (length < MINIMUM_CENTRE_DISTANCE) {
            return new double[] {0.0, 0.0};
        }
        double horizontal = toCentre.horizontalLength();
        double yawSize = AngleUnits.degrees(Math.atan2(Math.max(tight.sizeX(), tight.sizeZ()) / 2.0, length));
        double pitchSize = AngleUnits.degrees(Math.atan2(tight.sizeY() / 2.0, length));
        double yawOffset = AngleWrap.REMAINDER.apply(yaw - AngleUnits.degrees(Math.atan2(-toCentre.x(), toCentre.z())));
        double pitchOffset = pitch - AngleUnits.degrees(-Math.atan2(toCentre.y(), horizontal));
        double yawPart = yawSize <= MINIMUM_ANGULAR_SIZE ? 0.0 : yawOffset / yawSize;
        double pitchPart = pitchSize <= MINIMUM_ANGULAR_SIZE ? 0.0 : pitchOffset / pitchSize;
        return new double[] {yawPart, pitchPart};
    }
}
