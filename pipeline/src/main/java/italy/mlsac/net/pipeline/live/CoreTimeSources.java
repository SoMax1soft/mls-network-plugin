// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.UuidPurpose;
import italy.mlsac.net.pipeline.environment.UuidSource;
import italy.mlsac.net.pipeline.environment.WallClock;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CoreTimeSources implements MonotonicClock, WallClock, UuidSource {

    private final Clock clock;
    private final RandomSource random;

    public CoreTimeSources(Clock clock, RandomSource random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public long nanos(MonotonicPurpose purpose) {
        return clock.monotonicNanos(purpose.tag());
    }

    @Override
    public Instant instant(WallPurpose purpose) {
        return clock.wallInstant(purpose.tag());
    }

    @Override
    public String localStamp(WallPurpose purpose, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(instant(purpose));
    }

    @Override
    public UUID next(UuidPurpose purpose) {
        return random.randomUuid(purpose.tag());
    }
}
