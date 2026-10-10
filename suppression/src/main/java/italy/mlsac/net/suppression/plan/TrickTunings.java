// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TrickTunings {

    private final Map<TrickType, TrickTuning> byType;

    private TrickTunings(Map<TrickType, TrickTuning> byType) {
        this.byType = Collections.unmodifiableMap(byType);
    }

    public static TrickTunings fromLegacyEntries(List<LegacyTrickEntry> entries) {
        Map<TrickType, TrickTuning> byType = new EnumMap<>(TrickType.class);
        for (LegacyTrickEntry entry : entries) {
            byType.putIfAbsent(entry.type(), TrickTuning.from(entry));
        }
        return new TrickTunings(byType);
    }

    public static TrickTunings defaults() {
        return fromLegacyEntries(List.of());
    }

    public TrickTuning tuningOf(TrickType type) {
        TrickTuning tuning = byType.get(type);
        return tuning != null ? tuning : TrickTuning.defaultsFor(type);
    }
}
