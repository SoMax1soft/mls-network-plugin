// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import java.util.List;
import java.util.OptionalLong;

public record LatencyResult(int attempts, int successes, OptionalLong averageMillis) {

    public static LatencyResult of(List<Long> samples, int attempts) {
        long sum = 0;
        int successes = 0;
        for (long sample : samples) {
            if (sample > 0) {
                sum += sample;
                successes++;
            }
        }
        return new LatencyResult(attempts, successes, successes == 0 ? OptionalLong.empty()
                : OptionalLong.of(sum / successes));
    }
}
