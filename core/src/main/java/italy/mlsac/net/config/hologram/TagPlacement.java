// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

public record TagPlacement(double extraHeight, boolean showSelf) {

    static TagPlacement stock() {
        return new TagPlacement(0.0, false);
    }
}
