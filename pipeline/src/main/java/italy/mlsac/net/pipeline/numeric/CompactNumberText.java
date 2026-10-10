// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

public final class CompactNumberText {
    private static final double INTEGRAL_LIMIT = 1e15;

    private CompactNumberText() {
    }

    public static String of(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        if (value == Math.rint(value) && Math.abs(value) < INTEGRAL_LIMIT) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    public static String of(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    public static String of(long value) {
        return Long.toString(value);
    }

    public static String of(boolean value) {
        return value ? "1" : "0";
    }
}
