// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.turn;

import java.util.concurrent.atomic.AtomicLong;

public final class TurnEpoch {
    private final AtomicLong value = new AtomicLong();

    public long value() {
        return value.get();
    }

    public void advance() {
        value.incrementAndGet();
    }
}
