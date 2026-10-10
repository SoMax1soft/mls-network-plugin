// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.clock.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScriptedRandom implements RandomSource {

    private final long fixedDraw;
    private final List<Long> bounds = new ArrayList<>();

    public ScriptedRandom(long fixedDraw) {
        this.fixedDraw = fixedDraw;
    }

    @Override
    public synchronized long nextLong(String purpose, long bound) {
        bounds.add(bound);
        return Math.min(fixedDraw, bound - 1);
    }

    @Override
    public double nextDouble(String purpose) {
        return 0.5;
    }

    @Override
    public UUID randomUuid(String purpose) {
        return new UUID(0L, bounds.size());
    }

    public synchronized List<Long> bounds() {
        return List.copyOf(bounds);
    }
}
