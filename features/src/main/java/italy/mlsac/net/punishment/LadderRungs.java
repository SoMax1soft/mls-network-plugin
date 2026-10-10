// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record LadderRungs(NavigableMap<Integer, List<String>> rungs) {

    public LadderRungs {
        TreeMap<Integer, List<String>> copy = new TreeMap<>();
        rungs.forEach((level, commands) -> {
            if (!commands.isEmpty()) {
                copy.put(level, List.copyOf(commands));
            }
        });
        rungs = Collections.unmodifiableNavigableMap(copy);
    }

    public static LadderRungs of(Map<Integer, List<String>> rungs) {
        return new LadderRungs(new TreeMap<>(rungs));
    }

    public static LadderRungs pluginDefaults() {
        return of(Map.of(
                1, List.of("kick {PLAYER} MLSAC Detection (VL:{VL})"),
                2, List.of("{ANIMATION} ban {PLAYER} Cheating (MLSAC VL:{VL})",
                        "broadcast {PLAYER} was punished by MLSAC")));
    }

    public Optional<List<String>> rungFor(int offenceLevel) {
        return Optional.ofNullable(rungs.floorEntry(offenceLevel)).map(Map.Entry::getValue);
    }

    public Optional<Map.Entry<Integer, List<String>>> top() {
        return Optional.ofNullable(rungs.lastEntry());
    }

    public boolean isEmpty() {
        return rungs.isEmpty();
    }
}
