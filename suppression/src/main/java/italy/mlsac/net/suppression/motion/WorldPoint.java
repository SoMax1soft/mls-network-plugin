// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.motion;

public record WorldPoint(String world, double x, double y, double z) {

    public double gapTo(WorldPoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean sameWorld(WorldPoint other) {
        return world.equals(other.world);
    }
}
