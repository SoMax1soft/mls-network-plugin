// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.OptionalInt;

public record TrackingRange(int blocks) {

    static final int SPIGOT_DEFAULT = 48;
    static final int MARGIN = 2;
    static final int MINIMUM = 8;

    public static TrackingRange fromSpigotSetting(OptionalInt playersRange) {
        return new TrackingRange(Math.max(MINIMUM, playersRange.orElse(SPIGOT_DEFAULT) - MARGIN));
    }

    public static TrackingRange whenUnreadable() {
        return new TrackingRange(SPIGOT_DEFAULT - MARGIN);
    }

    public boolean covers(double dx, double dz) {
        return dx * dx + dz * dz <= (double) blocks * blocks;
    }
}
