// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public record NametagDisplay(boolean background, int backgroundRgb, int backgroundOpacity, boolean shadow,
                             boolean visibleThroughWalls, double scale, Billboard billboard) {

    public static NametagDisplay pluginDefaults() {
        return new NametagDisplay(true, 0x000000, 25, true, false, 1.0, Billboard.CENTER);
    }

    public int plateColour() {
        if (!background) {
            return 0;
        }
        int alpha = (int) Math.round(backgroundOpacity * 255 / 100.0);
        return (alpha << 24) | (backgroundRgb & 0xFFFFFF);
    }

    public byte flags() {
        return (byte) ((shadow ? 0x01 : 0) | (visibleThroughWalls ? 0x02 : 0));
    }
}
