// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

public record AvgColorBand(int from, String color, boolean bold) {

    public String prefix() {
        return bold ? "&" + color + "&l" : "&" + color;
    }
}
