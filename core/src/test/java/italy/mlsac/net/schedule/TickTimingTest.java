// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.schedule.bukkit.BukkitTickTiming;
import italy.mlsac.net.schedule.folia.FoliaTickTiming;
import org.junit.jupiter.api.Test;

class TickTimingTest {

    @Test
    void bukkitKeepsZeroDelayAndForcesPositivePeriod() {
        BukkitTickTiming timing = new BukkitTickTiming();
        assertEquals(0, timing.delayTicks(-5));
        assertEquals(0, timing.delayTicks(0));
        assertEquals(7, timing.delayTicks(7));
        assertEquals(1, timing.everyTicks(0));
        assertEquals(20, timing.everyTicks(20));
    }

    @Test
    void foliaNeedsAtLeastOneTickAndConvertsAsyncToMillis() {
        FoliaTickTiming timing = new FoliaTickTiming();
        assertEquals(1, timing.delayTicks(0));
        assertEquals(1, timing.delayTicks(-3));
        assertEquals(40, timing.delayTicks(40));
        assertEquals(1, timing.everyTicks(0));
        assertEquals(50, timing.asyncMillis(0));
        assertEquals(50, timing.asyncMillis(-1));
        assertEquals(30_000, timing.asyncMillis(600));
    }
}
