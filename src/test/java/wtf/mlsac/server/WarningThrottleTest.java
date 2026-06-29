package wtf.mlsac.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningThrottleTest {

    @Test
    void respectsCooldownWindow() {
        WarningThrottle throttle = new WarningThrottle(5, 1000);
        // Timestamps are real wall-clock millis at runtime, so the first call always clears the
        // window (now - lastFireTime(0) is huge). Use a base offset >= interval to mirror that.
        long base = 10_000L;
        assertTrue(throttle.shouldFire(base));
        assertFalse(throttle.shouldFire(base + 999), "still inside the cooldown window");
        assertTrue(throttle.shouldFire(base + 1000), "cooldown elapsed");
    }

    @Test
    void stopsAfterMaxWarnings() {
        WarningThrottle throttle = new WarningThrottle(2, 0);
        assertTrue(throttle.shouldFire(0));
        assertTrue(throttle.shouldFire(1));
        assertFalse(throttle.shouldFire(2), "quota exhausted");
        assertFalse(throttle.shouldFire(100));
    }

    @Test
    void resetRestoresQuota() {
        WarningThrottle throttle = new WarningThrottle(1, 0);
        assertTrue(throttle.shouldFire(0));
        assertFalse(throttle.shouldFire(1));
        throttle.reset();
        assertTrue(throttle.shouldFire(2));
    }
}
