// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.clock;

import java.time.Instant;

public final class SystemClock implements Clock {

    @Override
    public long wallMillis(String purpose) {
        return System.currentTimeMillis();
    }

    @Override
    public Instant wallInstant(String purpose) {
        return Instant.now();
    }

    @Override
    public long monotonicNanos(String purpose) {
        return System.nanoTime();
    }
}
