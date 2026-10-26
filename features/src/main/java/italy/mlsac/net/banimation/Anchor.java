// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Locale;

public enum Anchor {
    VICTIM,
    ACTOR;

    static Anchor named(String name) {
        return "ACTOR".equals(name.trim().toUpperCase(Locale.ROOT)) ? ACTOR : VICTIM;
    }
}
