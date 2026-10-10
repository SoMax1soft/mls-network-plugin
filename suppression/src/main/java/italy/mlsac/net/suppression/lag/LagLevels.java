// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class LagLevels {

    private static final LagLevels NONE = new LagLevels(new EnumMap<>(LagKind.class));

    private final Map<LagKind, LagLevel> levelPerKind;

    private LagLevels(EnumMap<LagKind, LagLevel> levelPerKind) {
        this.levelPerKind = Collections.unmodifiableMap(levelPerKind);
    }

    public static LagLevels none() {
        return NONE;
    }

    public static LagLevels of(Map<LagKind, LagLevel> levels) {
        if (levels.isEmpty()) {
            return NONE;
        }
        return new LagLevels(new EnumMap<>(levels));
    }

    public Optional<LagLevel> levelOf(LagKind kind) {
        return Optional.ofNullable(levelPerKind.get(kind));
    }

    public boolean active(LagKind kind) {
        return levelPerKind.containsKey(kind);
    }

    public boolean isEmpty() {
        return levelPerKind.isEmpty();
    }

    public boolean needsTickWork() {
        return active(LagKind.SETBACK) || active(LagKind.GROUND) || active(LagKind.ROTATE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LagLevels levels && levels.levelPerKind.equals(levelPerKind);
    }

    @Override
    public int hashCode() {
        return levelPerKind.hashCode();
    }

    @Override
    public String toString() {
        return levelPerKind.toString();
    }
}
