// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;
import java.util.UUID;

public record SuspectRow(UUID player, String name, double average, List<Double> lastScores) {

    public SuspectRow {
        lastScores = List.copyOf(lastScores);
    }

    public List<Double> lastTen() {
        return lastScores.subList(Math.max(0, lastScores.size() - 10), lastScores.size());
    }
}
