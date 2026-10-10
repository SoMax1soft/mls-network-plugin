// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.penalty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public record PenaltyLadder(SortedMap<Integer, List<String>> rungs) {

    public PenaltyLadder {
        TreeMap<Integer, List<String>> copy = new TreeMap<>();
        rungs.forEach((level, commands) -> copy.put(level, List.copyOf(commands)));
        rungs = Collections.unmodifiableSortedMap(copy);
    }

    public static PenaltyLadder pluginDefaults() {
        TreeMap<Integer, List<String>> rungs = new TreeMap<>();
        rungs.put(1, List.of("kick {PLAYER} MLSAC Detection (VL:{VL})"));
        rungs.put(2, List.of("{ANIMATION} ban {PLAYER} Cheating (MLSAC VL:{VL})",
                "broadcast {PLAYER} was punished by MLSAC"));
        return new PenaltyLadder(rungs);
    }

    public Optional<List<String>> rungFor(int offenceLevel) {
        SortedMap<Integer, List<String>> atOrBelow =
                offenceLevel == Integer.MAX_VALUE ? rungs : rungs.headMap(offenceLevel + 1);
        return atOrBelow.isEmpty() ? Optional.empty() : Optional.of(atOrBelow.get(atOrBelow.lastKey()));
    }

    public Optional<Map.Entry<Integer, List<String>>> topRung() {
        return rungs.isEmpty() ? Optional.empty() : Optional.of(Map.entry(rungs.lastKey(), rungs.get(rungs.lastKey())));
    }

    public boolean isEmpty() {
        return rungs.isEmpty();
    }
}
