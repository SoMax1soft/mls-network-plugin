// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ActorKind {
    WARDEN(2.6);

    private final double headHeight;

    ActorKind(double headHeight) {
        this.headHeight = headHeight;
    }

    public double headHeight() {
        return headHeight;
    }

    static Optional<ActorKind> named(String name) {
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(kind -> kind.name().equals(wanted)).findFirst();
    }
}
