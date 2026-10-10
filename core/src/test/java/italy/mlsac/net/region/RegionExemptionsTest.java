// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.detection.RegionEntry;
import italy.mlsac.net.config.detection.RegionSettings;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RegionExemptionsTest {

    private final UUID player = UUID.randomUUID();
    private final UUID world = UUID.randomUUID();
    private final CapturingLogSink sink = new CapturingLogSink();

    @Test
    void wholeWorldEntriesWorkWithoutWorldGuard() {
        RegionSettings settings = new RegionSettings(true, List.of(RegionEntry.parse("World_Nether:*")));
        RegionExemptions regions = new RegionExemptions(() -> settings, RegionLookup.NONE, log());
        assertTrue(regions.evaluate(player, new BlockPosition(world, "world_nether", 0, 64, 0), 0));
        assertFalse(regions.evaluate(player, new BlockPosition(world, "world", 0, 64, 0), 1000));
        assertTrue(sink.texts(LogLevel.WARNING).isEmpty());
    }

    @Test
    void onlyTheTopRegionCountsAndAnswersAreCached() {
        AtomicInteger lookups = new AtomicInteger();
        RegionLookup lookup = new RegionLookup() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public Optional<String> topRegion(BlockPosition position) {
                lookups.incrementAndGet();
                return Optional.of("spawn");
            }
        };
        RegionSettings settings = new RegionSettings(true, List.of(RegionEntry.parse("Spawn"),
                RegionEntry.parse("a:b:c")));
        RegionExemptions regions = new RegionExemptions(() -> settings, lookup, log());
        BlockPosition at = new BlockPosition(world, "lobby", 1, 2, 3);
        long cacheMillis = 500;
        assertTrue(regions.evaluate(player, at, 0));
        assertTrue(regions.evaluate(player, at, cacheMillis - 1));
        assertEquals(1, lookups.get());
        regions.evaluate(player, at, cacheMillis);
        assertEquals(2, lookups.get());
        assertTrue(regions.lastKnown(player));
        assertEquals(new RegionEntry.RegionInWorld("a", "b:c"), settings.entries().get(1));
    }

    @Test
    void missingWorldGuardWarnsOnceAndDisabledSettingsExemptNobody() {
        RegionSettings settings = new RegionSettings(true, List.of(RegionEntry.parse("spawn")));
        RegionExemptions regions = new RegionExemptions(() -> settings, RegionLookup.NONE, log());
        regions.evaluate(player, new BlockPosition(world, "w", 0, 0, 0), 0);
        regions.evaluate(player, new BlockPosition(world, "w", 5, 0, 0), 0);
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
        RegionSettings off = new RegionSettings(false, List.of(RegionEntry.parse("w:*")));
        assertFalse(new RegionExemptions(() -> off, RegionLookup.NONE, log())
                .evaluate(player, new BlockPosition(world, "w", 0, 0, 0), 0));
    }

    private ConsoleLog log() {
        return TestLogs.capturing(sink, new ManualClock(0), false);
    }
}
