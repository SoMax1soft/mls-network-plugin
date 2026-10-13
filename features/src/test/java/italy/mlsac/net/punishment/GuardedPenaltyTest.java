// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import italy.mlsac.net.config.guard.GuardMode;
import italy.mlsac.net.guard.GuardBench;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuardedPenaltyTest {

    private final GuardBench bench = new GuardBench();
    private final PenaltyHarness harness = new PenaltyHarness(bench.guard);

    @Test
    void blockedLadderCommandsNeverReachTheServer() {
        harness.ladder(Map.of(1, List.of("op {PLAYER}", "kick {PLAYER} bye", "kick {PLAYER}\nop {PLAYER}")));
        harness.ladder.onFlag(harness.flag("Steve", 1));
        assertEquals(List.of("kick Steve bye"), harness.console);
        assertEquals(List.of("DENIED_ROOT", "CONTROL_CHARACTER"), bench.refusals);
        assertEquals(1, harness.history.newestFirst().size());
        assertEquals(List.of("kick {PLAYER} bye"), harness.handedOver);
        assertEquals(1, harness.punishEvents.size());
    }

    @Test
    void blockedAnimatedCommandsPlayNoAnimation() {
        harness.ladder(Map.of(2, List.of("{ANIMATION} op {PLAYER}", "{ANIMATION} /ban  {PLAYER} Cheating")));
        harness.ladder.onFlag(harness.flag("Steve", 2));
        assertEquals(List.of("classic_ban|ban Steve Cheating"), harness.animations);
        assertEquals(List.of("DENIED_ROOT"), bench.refusals);
        assertTrue(harness.console.isEmpty());
    }

    @Test
    void staffNoticesAreNotConsoleCommands() {
        harness.ladder(Map.of(1, List.of("{CUSTOM_ALERT} &c{PLAYER} op me")));
        harness.ladder.onFlag(harness.flag("Steve", 1));
        assertEquals(1, harness.staffLines.size());
        assertTrue(bench.refusals.isEmpty());
    }

    @Test
    void maximumPunishmentIsGuardedToo() {
        harness.ladder(Map.of(5, List.of("ban {PLAYER}", "op {PLAYER}")));
        harness.maximum.punish(harness.player, "Steve");
        assertEquals(List.of("ban Steve"), harness.console);
        assertEquals(List.of("DENIED_ROOT"), bench.refusals);
    }

    @Test
    void warnModeStillDispatches() {
        bench.settings = new CommandGuardSettings(true, GuardMode.WARN, Set.of(), 120, 30, 15, 10);
        harness.ladder(Map.of(1, List.of("op {PLAYER}")));
        harness.ladder.onFlag(harness.flag("Steve", 1));
        assertEquals(List.of("op Steve"), harness.console);
        assertEquals(List.of("DENIED_ROOT"), bench.refusals);
    }
}
