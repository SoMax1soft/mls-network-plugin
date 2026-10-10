// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoreTimeSourcesTest {

    private static final Instant PRECISE = Instant.parse("2026-09-30T17:53:46.312405363Z");

    @Test
    void sessionInstantsKeepThePlatformPrecision() {
        CoreTimeSources sources = new CoreTimeSources(new PreciseClock(), new NoRandom());
        assertEquals(PRECISE, sources.instant(WallPurpose.SESSION_START));
        assertEquals("2026-09-30T17:53:46.312405363Z", sources.instant(WallPurpose.SESSION_SAVED).toString());
        assertEquals(PRECISE.toEpochMilli(), sources.instant(WallPurpose.SESSION_START).toEpochMilli());
    }

    private static final class PreciseClock implements Clock {

        @Override
        public long wallMillis(String purpose) {
            return PRECISE.toEpochMilli();
        }

        @Override
        public Instant wallInstant(String purpose) {
            return PRECISE;
        }

        @Override
        public long monotonicNanos(String purpose) {
            return 0L;
        }
    }

    private static final class NoRandom implements RandomSource {

        @Override
        public long nextLong(String purpose, long bound) {
            return 0L;
        }

        @Override
        public double nextDouble(String purpose) {
            return 0.0;
        }

        @Override
        public UUID randomUuid(String purpose) {
            return new UUID(0L, 0L);
        }
    }
}
