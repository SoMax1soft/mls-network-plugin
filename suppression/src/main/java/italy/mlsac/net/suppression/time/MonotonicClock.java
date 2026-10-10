// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.time;

@FunctionalInterface
public interface MonotonicClock {

    long clockMillis();
}
