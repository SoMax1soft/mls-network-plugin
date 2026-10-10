// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import java.util.Locale;

public enum LagKind {
    DELAY,
    MISS,
    SETBACK,
    GROUND,
    ROTATE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
