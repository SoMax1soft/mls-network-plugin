// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record DustColor(int red, int green, int blue, double size) {

    public static DustColor clamped(int red, int green, int blue, double size) {
        return new DustColor(channel(red), channel(green), channel(blue), size);
    }

    private static int channel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
