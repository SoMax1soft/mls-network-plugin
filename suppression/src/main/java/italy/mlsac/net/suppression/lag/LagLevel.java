// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import java.util.Locale;

public enum LagLevel {
    LIGHT,
    MEDIUM,
    HEAVY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public LagLevel stronger(LagLevel other) {
        return other != null && other.ordinal() > ordinal() ? other : this;
    }
}
