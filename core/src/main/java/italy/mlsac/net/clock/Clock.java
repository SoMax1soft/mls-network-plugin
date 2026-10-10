// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.clock;

import java.time.Instant;

public interface Clock {

    long wallMillis(String purpose);

    long monotonicNanos(String purpose);

    default Instant wallInstant(String purpose) {
        return Instant.ofEpochMilli(wallMillis(purpose));
    }

    default long monotonicMillis(String purpose) {
        return monotonicNanos(purpose) / 1_000_000L;
    }
}
