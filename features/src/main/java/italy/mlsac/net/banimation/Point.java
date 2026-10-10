// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record Point(double x, double y, double z) {

    public Point plus(double dx, double dy, double dz) {
        return new Point(x + dx, y + dy, z + dz);
    }

    public Point plus(Point other) {
        return plus(other.x, other.y, other.z);
    }

    public Point times(double factor) {
        return new Point(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
