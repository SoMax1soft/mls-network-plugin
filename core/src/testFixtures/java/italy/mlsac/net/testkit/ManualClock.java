// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.clock.Clock;
import java.util.ArrayList;
import java.util.List;

public final class ManualClock implements Clock {

    private long wallMillis;
    private long monotonicNanos;
    private final List<String> reads = new ArrayList<>();

    public ManualClock(long wallMillis) {
        this.wallMillis = wallMillis;
    }

    @Override
    public synchronized long wallMillis(String purpose) {
        reads.add(purpose);
        return wallMillis;
    }

    @Override
    public synchronized long monotonicNanos(String purpose) {
        reads.add(purpose);
        return monotonicNanos;
    }

    public synchronized void advanceMillis(long millis) {
        wallMillis += millis;
        monotonicNanos += millis * 1_000_000L;
    }

    public synchronized List<String> reads() {
        return List.copyOf(reads);
    }

    public synchronized List<String> reads(String purpose) {
        return reads.stream().filter(purpose::equals).toList();
    }
}
