// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.ArrayList;
import java.util.List;

public record MeanShadeThreshold(int from, String color, boolean bold) {

    public static List<MeanShadeThreshold> defaults() {
        return List.of(new MeanShadeThreshold(0, "f", false), new MeanShadeThreshold(60, "c", false), new MeanShadeThreshold(90, "4", true));
    }

    public static List<MeanShadeThreshold> strictlyRising(List<MeanShadeThreshold> candidates) {
        List<MeanShadeThreshold> kept = new ArrayList<>();
        for (MeanShadeThreshold band : candidates) {
            if (kept.isEmpty() || band.from() > kept.get(kept.size() - 1).from()) {
                kept.add(band);
            }
        }
        return kept.isEmpty() ? defaults() : List.copyOf(kept);
    }

    public static MeanShadeThreshold forValue(List<MeanShadeThreshold> bands, long value) {
        MeanShadeThreshold chosen = bands.get(0);
        for (MeanShadeThreshold band : bands) {
            if (band.from() <= value) {
                chosen = band;
            }
        }
        return chosen;
    }

    public String prefix() {
        return "&" + color + (bold ? "&l" : "");
    }
}
