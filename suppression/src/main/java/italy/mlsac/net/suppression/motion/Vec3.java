// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.motion;

public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalized() {
        double length = length();
        return length < 1.0E-9 ? ZERO : new Vec3(x / length, y / length, z / length);
    }

    public Vec3 scaled(double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public Vec3 withY(double newY) {
        return new Vec3(x, newY, z);
    }
}
