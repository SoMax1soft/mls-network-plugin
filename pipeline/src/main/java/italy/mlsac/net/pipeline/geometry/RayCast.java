// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

public final class RayCast {
    private static final double REACH = 8.0;
    private static final double PARALLEL_LIMIT = 1e-12;
    private static final int SEARCH_STEPS = 80;
    private static final int AXES = 3;
    private static final double MISSED = Double.NEGATIVE_INFINITY;

    private RayCast() {
    }

    public static Vec3 entryOrNull(Box box, Vec3 origin, Vec3 direction) {
        if (box.contains(origin)) {
            return origin;
        }
        double distance = entryDistance(box, origin.x(), origin.y(), origin.z(), direction);
        return distance == MISSED ? null : origin.alongRay(direction, distance);
    }

    public static boolean hits(Box box, Vec3 origin, Vec3 direction) {
        return hits(box, origin.x(), origin.y(), origin.z(), direction);
    }

    public static boolean hits(Box box, double originX, double originY, double originZ, Vec3 direction) {
        return box.contains(originX, originY, originZ)
                || entryDistance(box, originX, originY, originZ, direction) != MISSED;
    }

    private static double entryDistance(Box box, double originX, double originY, double originZ, Vec3 direction) {
        double near = 0.0;
        double far = REACH;
        for (int axis = 0; axis < AXES; axis++) {
            double start = axis == 0 ? originX : axis == 1 ? originY : originZ;
            double step = component(direction, axis);
            double lowBound = component(box.low(), axis);
            double highBound = component(box.high(), axis);
            if (Math.abs(step) < PARALLEL_LIMIT) {
                if (!(start >= lowBound && start <= highBound)) {
                    return MISSED;
                }
                continue;
            }
            double first = (lowBound - start) / step;
            double second = (highBound - start) / step;
            boolean flipped = first > second;
            near = Math.max(near, flipped ? second : first);
            far = Math.min(far, flipped ? first : second);
            if (near > far) {
                return MISSED;
            }
        }
        return near;
    }

    private static double component(Vec3 vector, int axis) {
        return switch (axis) {
            case 0 -> vector.x();
            case 1 -> vector.y();
            default -> vector.z();
        };
    }

    public static double miss(Box box, Vec3 origin, Vec3 direction) {
        double low = 0.0;
        double high = REACH;
        for (int step = 0; step < SEARCH_STEPS; step++) {
            double near = low + (high - low) / 3.0;
            double far = high - (high - low) / 3.0;
            if (box.gapTo(origin.alongRay(direction, near)) <= box.gapTo(origin.alongRay(direction, far))) {
                high = far;
            } else {
                low = near;
            }
        }
        return box.gapTo(origin.alongRay(direction, (low + high) / 2.0));
    }
}
