// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.chance.EngineRandom;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ScriptedRandom implements EngineRandom {

    private final Deque<Double> script = new ArrayDeque<>();
    private final double fallback;
    private int draws;

    public ScriptedRandom(double fallback) {
        this.fallback = fallback;
    }

    public ScriptedRandom queue(double... values) {
        for (double value : values) {
            script.addLast(value);
        }
        return this;
    }

    public int draws() {
        return draws;
    }

    public int remaining() {
        return script.size();
    }

    @Override
    public double nextUnit() {
        draws++;
        Double next = script.pollFirst();
        return next == null ? fallback : next;
    }
}
