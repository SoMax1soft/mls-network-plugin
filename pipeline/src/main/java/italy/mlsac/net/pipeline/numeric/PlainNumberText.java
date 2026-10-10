// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

public final class PlainNumberText {
    private PlainNumberText() {
    }

    public static String of(double value) {
        return Double.toString(value);
    }

    public static String of(float value) {
        return Float.toString(value);
    }

    public static String of(long value) {
        return Long.toString(value);
    }

    public static String of(boolean flag) {
        return flag ? "1" : "0";
    }
}
