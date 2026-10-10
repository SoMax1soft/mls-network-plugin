// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

import java.util.function.DoubleBinaryOperator;

public record Box(Vec3 low, Vec3 high) {
    static final double THINNEST_HALF = 0.05;
    static final double SHORTEST = 0.1;

    public Box(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
        this(new Vec3(lowX, lowY, lowZ), new Vec3(highX, highY, highZ));
    }

    public static Box at(Vec3 spot) {
        return new Box(spot, spot);
    }

    public static Box player(double x, double y, double z, double width, double height) {
        return at(new Vec3(x, y, z)).grownBySize(width, height);
    }

    public Box union(Box other) {
        return new Box(pairwise(low, other.low, Math::min), pairwise(high, other.high, Math::max));
    }

    public Box grownBySize(double width, double height) {
        double half = Math.max(THINNEST_HALF, width / 2.0);
        return new Box(new Vec3(low.x() - half, low.y(), low.z() - half),
                new Vec3(high.x() + half, high.y() + Math.max(SHORTEST, height), high.z() + half));
    }

    public Box raisedTop(double amount) {
        return new Box(low, new Vec3(high.x(), high.y() + amount, high.z()));
    }

    public double sizeX() {
        return high.x() - low.x();
    }

    public double sizeY() {
        return high.y() - low.y();
    }

    public double sizeZ() {
        return high.z() - low.z();
    }

    public Vec3 centre() {
        return new Vec3((low.x() + high.x()) / 2.0, (low.y() + high.y()) / 2.0, (low.z() + high.z()) / 2.0);
    }

    public boolean contains(Vec3 spot) {
        return contains(spot.x(), spot.y(), spot.z());
    }

    public boolean contains(double x, double y, double z) {
        return within(x, low.x(), high.x()) && within(y, low.y(), high.y()) && within(z, low.z(), high.z());
    }

    public double gapTo(Vec3 spot) {
        Vec3 outside = new Vec3(outside(spot.x(), low.x(), high.x()), outside(spot.y(), low.y(), high.y()),
                outside(spot.z(), low.z(), high.z()));
        return outside.length();
    }

    public Vec3 clamp(Vec3 spot) {
        return pairwise(low, pairwise(high, spot, Math::min), Math::max);
    }

    public boolean sameBounds(Box other) {
        return bitsEqual(low, other.low) && bitsEqual(high, other.high);
    }

    private static Vec3 pairwise(Vec3 first, Vec3 second, DoubleBinaryOperator pick) {
        return new Vec3(pick.applyAsDouble(first.x(), second.x()), pick.applyAsDouble(first.y(), second.y()),
                pick.applyAsDouble(first.z(), second.z()));
    }

    private static boolean within(double value, double from, double to) {
        return value >= from && value <= to;
    }

    private static double outside(double value, double from, double to) {
        if (value < from) {
            return from - value;
        }
        return value > to ? value - to : 0.0;
    }

    private static boolean bitsEqual(Vec3 first, Vec3 second) {
        return Double.doubleToRawLongBits(first.x()) == Double.doubleToRawLongBits(second.x())
                && Double.doubleToRawLongBits(first.y()) == Double.doubleToRawLongBits(second.y())
                && Double.doubleToRawLongBits(first.z()) == Double.doubleToRawLongBits(second.z());
    }
}
