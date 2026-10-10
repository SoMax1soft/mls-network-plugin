// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.motion.WorldPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class PositionHistory {

    public static final int CAPACITY = 11;

    private final List<WorldPoint> entries = new ArrayList<>(CAPACITY);

    public void append(WorldPoint position) {
        if (entries.size() >= CAPACITY) {
            entries.remove(0);
        }
        entries.add(position);
    }

    public Optional<WorldPoint> newestMatching(Predicate<WorldPoint> wanted) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            WorldPoint entry = entries.get(index);
            if (wanted.test(entry)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<WorldPoint> ticksBack(int ticks) {
        if (entries.size() <= ticks) {
            return Optional.empty();
        }
        return Optional.of(entries.get(entries.size() - 1 - ticks));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
