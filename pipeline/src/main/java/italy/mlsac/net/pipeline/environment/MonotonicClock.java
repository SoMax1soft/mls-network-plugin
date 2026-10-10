// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

public interface MonotonicClock {
    long nanos(MonotonicPurpose purpose);
}
