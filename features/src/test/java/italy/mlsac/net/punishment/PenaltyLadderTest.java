// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PenaltyLadderTest {

    @Test
    void floorLookup() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.ladder(Map.of(1, List.of("kick {PLAYER}"), 3, List.of("ban {PLAYER}")));
        for (int vl : new int[] {1, 2, 3, 7}) {
            harness.now += 10_000;
            harness.ladder.onFlag(harness.flag("Steve", vl));
        }
        assertEquals(List.of("kick Steve", "kick Steve", "ban Steve", "ban Steve"), harness.console);
        assertTrue(LadderRungs.of(Map.of(2, List.of("x"))).rungFor(1).isEmpty());
    }

    @Test
    void punishingRungsWithinFiveSecondsAreSkipped() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.ladder(Map.of(1, List.of("kick {PLAYER}")));
        assertEquals(RungOutcome.RAN, harness.rungs.run(harness.flag("Steve", 1), List.of("kick {PLAYER}")));
        harness.now += 3_000;
        assertEquals(RungOutcome.SKIPPED_COOLDOWN, harness.rungs.run(harness.flag("Steve", 2), List.of("kick {PLAYER}")));
        harness.now += 3_000;
        assertEquals(RungOutcome.RAN, harness.rungs.run(harness.flag("Steve", 3), List.of("kick {PLAYER}")));
        assertEquals(2, harness.console.size());
    }

    @Test
    void cooldownMeasuredFromLastRunningRungOnly() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rungs.run(harness.flag("Steve", 1), List.of("kick {PLAYER}"));
        harness.now += 4_000;
        harness.rungs.run(harness.flag("Steve", 2), List.of("kick {PLAYER}"));
        harness.now += 2_000;
        assertEquals(RungOutcome.RAN, harness.rungs.run(harness.flag("Steve", 3), List.of("kick {PLAYER}")));
    }

    @Test
    void nonPunishingRungNeverOnCooldownAndNotReported() {
        PenaltyHarness harness = new PenaltyHarness();
        for (int i = 0; i < 3; i++) {
            assertEquals(RungOutcome.RAN, harness.rungs.run(harness.flag("Steve", 1), List.of("broadcast x", "say y")));
        }
        assertEquals(6, harness.console.size());
        assertTrue(harness.punishEvents.isEmpty());
    }

    @Test
    void namespacedTempbanIsPunishingAndReportedAsBan() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rungs.run(harness.flag("Steve", 4), List.of("/essentials:tempban {PLAYER} 1d"));
        assertEquals(List.of("/essentials:tempban Steve 1d"), harness.console);
        PunishEvent event = harness.punishEvents.get(0);
        assertEquals("ban", event.action());
        assertEquals("/essentials:tempban {PLAYER} 1d", event.command());
        assertEquals(4, event.offenceLevel());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{ANIMATION} ban {PLAYER} x|ban", "{KICK} kick {PLAYER}|kick", "mute {PLAYER}|mute",
            "tempmute {PLAYER}|mute", "jail {PLAYER}|jail", "punish {PLAYER}|raw", "ban-ip {PLAYER}|ban",
            "{ANIMATION} say hi|animation", "IPBAN x|ban"})
    void reportedActions(String command, String action) {
        LadderCommand parsed = LadderCommand.parse(command);
        assertTrue(parsed.punishing());
        assertEquals(action, parsed.reportedAction());
    }

    @Test
    void placeholdersUseDotDecimals() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rungs.run(new italy.mlsac.net.scoring.FlagContext(harness.player, "Steve", 3, 0.9567, 20.0),
                List.of("say {PLAYER} <player> {VL} <vl> {PROBABILITY} <probability> {BUFFER} <buffer>"));
        assertEquals(List.of("say Steve Steve 3 3 0.96 0.96 20.00 20.00"), harness.console);
    }

    @Test
    void safeNameCheckKicksDirectly() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rules = new PenaltyRules(LadderRungs.pluginDefaults(), true, "classic_ban", true, false);
        harness.rungs.run(harness.flag("bad.name", 1), List.of("kick {PLAYER} MLSAC"));
        assertTrue(harness.console.isEmpty());
        assertEquals(List.of("MLSAC Detection (VL:1)"), harness.kicks);
        assertEquals("DIRECT_KICK (unsafe name)", harness.history.newestFirst().get(0).command());
        assertEquals("kick", harness.punishEvents.get(0).action());
        assertEquals("DIRECT_KICK", harness.punishEvents.get(0).command());
        assertTrue(harness.handedOver.isEmpty());
        assertEquals(1, harness.log.warnings.size());
    }

    @Test
    void safeNameCheckDoesNotBlockStaffNotices() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rules = new PenaltyRules(LadderRungs.pluginDefaults(), true, "classic_ban", true, false);
        harness.rungs.run(harness.flag("bad.name", 1), List.of("{CUSTOM_ALERT} &c{PLAYER} VL {VL}"));
        assertEquals(1, harness.staffLines.size());
        assertTrue(harness.kicks.isEmpty());
    }

    @Test
    void customAlertGoesToStaffOnly() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rules = new PenaltyRules(LadderRungs.pluginDefaults(), true, "classic_ban", false, true);
        harness.rungs.run(harness.flag("Steve", 2), List.of("{CUSTOM_ALERT} &c{PLAYER} VL {VL}"));
        assertTrue(harness.console.isEmpty());
        assertTrue(harness.punishEvents.isEmpty());
        assertTrue(harness.staffLines.get(0).endsWith("§cSteve VL 2"));
        assertTrue(harness.log.infos.get(0).endsWith("Steve VL 2"));
        assertEquals(List.of("{CUSTOM_ALERT} &c{PLAYER} VL {VL}"), harness.handedOver);
    }

    @Test
    void animationUsesConfiguredTypeOrFallsBack() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rules = new PenaltyRules(LadderRungs.pluginDefaults(), true, "fancy", false, false);
        harness.rungs.run(harness.flag("Steve", 2), List.of("{ANIMATION} ban {PLAYER} Cheating"));
        assertEquals(List.of("classic_ban|ban Steve Cheating"), harness.animations);
        assertEquals(1, harness.log.warnings.size());
        harness.knownAnimations.clear();
        harness.now += 10_000;
        harness.rungs.run(harness.flag("Steve", 2), List.of("{BAN} ban {PLAYER}"));
        assertEquals(List.of("ban Steve"), harness.console);
    }

    @Test
    void animationDisabledOrOfflineDispatchesDirectly() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.rules = new PenaltyRules(LadderRungs.pluginDefaults(), false, "classic_ban", false, false);
        harness.rungs.run(harness.flag("Steve", 2), List.of("{ANIMATION} ban {PLAYER}"));
        harness.rules = PenaltyRules.pluginDefaults();
        harness.online.clear();
        harness.now += 10_000;
        harness.rungs.run(harness.flag("Steve", 2), List.of("{ANIMATION} ban {PLAYER}"));
        assertEquals(List.of("ban Steve", "ban Steve"), harness.console);
        assertTrue(harness.animations.isEmpty());
    }

    @Test
    void oneFailingCommandDoesNotStopTheRung() {
        PenaltyHarness harness = new PenaltyHarness();
        PenaltyServer failing = new PenaltyServer() {
            @Override
            public void dispatchConsoleNextTick(String command) {
                if (command.startsWith("boom")) {
                    throw new IllegalStateException("boom");
                }
                harness.console.add(command);
            }

            @Override
            public boolean online(java.util.UUID player) {
                return true;
            }

            @Override
            public void kickNextTick(java.util.UUID player, String reason) {
            }

            @Override
            public void punishReport(PunishEvent event) {
            }
        };
        CommandRunner runner = new CommandRunner(() -> harness.rules, failing,
                new ServerPenaltyDispatch(failing, harness, harness,
                        italy.mlsac.net.testkit.BundledResources.english(), () -> harness.rules, harness.log),
                harness.history, harness, harness.log, italy.mlsac.net.guard.ConsoleGate.OPEN);
        RungRunner rungs = new RungRunner(runner, new PunishCooldown(), harness, harness.log);
        rungs.run(harness.flag("Steve", 1), List.of("boom", "say ok"));
        assertEquals(List.of("say ok"), harness.console);
        assertEquals(1, harness.log.severes.size());
    }

    @Test
    void maximumPunishmentUsesTopRungWithoutCooldown() {
        PenaltyHarness harness = new PenaltyHarness();
        harness.ladder(Map.of(1, List.of("kick {PLAYER}"), 5, List.of("ban {PLAYER} {VL} {PROBABILITY} {BUFFER}")));
        harness.rungs.run(harness.flag("Steve", 1), List.of("kick {PLAYER}"));
        assertEquals(5, harness.maximum.punish(harness.player, "Steve").orElseThrow());
        assertEquals(List.of("kick Steve", "ban Steve 5 1.00 100.00"), harness.console);
        harness.ladder(Map.of());
        assertFalse(harness.maximum.punish(harness.player, "Steve").isPresent());
    }

    @Test
    void kickHistoryKeepsTenNewestFirst() {
        PenaltyHarness harness = new PenaltyHarness();
        for (int i = 1; i <= 12; i++) {
            harness.rungs.run(harness.flag("Steve", i), List.of("say " + i));
        }
        List<KickLogLine> records = harness.history.newestFirst();
        assertEquals(10, records.size());
        assertEquals("say 12", records.get(0).command());
        assertEquals("say 3", records.get(9).command());
        assertEquals("12:34:56", records.get(0).localTime());
    }
}
