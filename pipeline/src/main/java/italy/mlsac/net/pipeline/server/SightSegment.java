// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.pipeline.geometry.Vec3;

public final class SightSegment {

    static final double SHORTEST_LENGTH = 0.001;
    static final double END_SLACK = 0.001;

    private final Vec3 from;
    private final double spanX;
    private final double spanY;
    private final double spanZ;
    private final double length;

    private SightSegment(Vec3 from, double spanX, double spanY, double spanZ) {
        this.from = from;
        this.spanX = spanX;
        this.spanY = spanY;
        this.spanZ = spanZ;
        this.length = Math.sqrt(spanX * spanX + spanY * spanY + spanZ * spanZ);
    }

    public static SightSegment between(Vec3 from, Vec3 to) {
        return new SightSegment(from, to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
    }

    public static boolean finite(Vec3 from, Vec3 to) {
        return Double.isFinite(from.x()) && Double.isFinite(from.y()) && Double.isFinite(from.z())
                && Double.isFinite(to.x()) && Double.isFinite(to.y()) && Double.isFinite(to.z());
    }

    public static int chunkIndex(double blockCoordinate) {
        return ((int) Math.floor(blockCoordinate)) >> 4;
    }

    public Vec3 from() {
        return from;
    }

    public double length() {
        return length;
    }

    public boolean tooShort() {
        return length < SHORTEST_LENGTH;
    }

    public double directionX() {
        return spanX / length;
    }

    public double directionY() {
        return spanY / length;
    }

    public double directionZ() {
        return spanZ / length;
    }

    public boolean cutAt(double hitX, double hitY, double hitZ) {
        double offsetX = hitX - from.x();
        double offsetY = hitY - from.y();
        double offsetZ = hitZ - from.z();
        double hitDistance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        return hitDistance < length - END_SLACK;
    }
}
