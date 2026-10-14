// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.alerting.AlertOutput;
import italy.mlsac.net.alerting.AlertSound;
import italy.mlsac.net.alerting.AlertSoundResolver;
import italy.mlsac.net.alerting.AlertTexts;
import italy.mlsac.net.alerting.StaffAlerts;
import italy.mlsac.net.alerting.SubscriberBroadcast;
import italy.mlsac.net.alerting.SubscriberSet;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.features.live.PluginLogbook;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.logbook.WarnOnce;
import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.FlagProcessor;
import italy.mlsac.net.scoring.ReportingPolicy;
import italy.mlsac.net.scoring.ScoringRules;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.VerdictScreen;
import italy.mlsac.net.scoring.ViolationLedger;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FakeStaff;
import italy.mlsac.net.testkit.FakeStaffRoster;
import italy.mlsac.net.testkit.FixedModels;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.testkit.TickQueue;
import italy.mlsac.net.text.MessageBook;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BadPacketConsoleTest {

    private static final Pattern RAW_CODE = Pattern.compile("[&§][0-9a-fk-orxA-FK-ORX#]");

    @Test
    void withDebugOffNothingAboutBadPacketsReachesGameOrConsoleButTheLadderLine() {
        Stand stand = new Stand(false);
        stand.forgeFor(60);
        print("debug off", stand.console);
        assertTrue(stand.console.lines().stream().noneMatch(line -> line.text().contains("Bad packets")
                || line.text().contains("Packet integrity")), stand.console.lines().toString());
        assertTrue(stand.mod.received().isEmpty(), stand.mod.received().toString());
        assertTrue(stand.mod.sounds().isEmpty());
        assertEquals(1, stand.ladder.size());
        assertEquals(1, stand.console.lines().size());
        assertTrue(stand.console.contains("Forger Got VL (VL:1)"));
        assertFalse(stand.console.contains("100%"));
        assertFalse(stand.roster.active(stand.forger).orElseThrow().hasChecks());
        assertTrue(stand.roster.active(stand.forger).orElseThrow().buffer() > 0.0);
    }

    @Test
    void withDebugOnEveryBadPacketLineReachesTheConsoleClean() {
        Stand stand = new Stand(true);
        stand.forgeFor(60);
        print("debug on", stand.console);
        List<String> debugLines = stand.console.texts(LogLevel.DEBUG);
        assertTrue(debugLines.stream().anyMatch(line -> line.contains("forged-teleport-confirm from Forger (1/60s")));
        assertTrue(debugLines.stream().anyMatch(line -> line.endsWith("Forger Bad packets: forged teleport confirm (3/60s)")),
                debugLines.toString());
        assertTrue(debugLines.stream().anyMatch(line -> line.contains("[Bad packets] Forger Failed check (Packet integrity)")),
                debugLines.toString());
        stand.console.lines().forEach(line -> assertFalse(RAW_CODE.matcher(line.text()).find(), line.text()));
        assertTrue(debugLines.stream().noneMatch(line -> line.contains("Aim-Ai") || line.contains("100%")));
        assertEquals(1, stand.ladder.size());
        assertTrue(stand.mod.received().isEmpty(), stand.mod.received().toString());
        assertTrue(stand.mod.sounds().isEmpty());
        assertTrue(stand.console.texts(LogLevel.INFO).stream().noneMatch(line -> line.contains("Bad packets")));
    }

    private static void print(String label, CapturingLogSink console) {
        console.lines().forEach(line -> System.out.println("[" + label + "] " + line.level() + " " + line.text()));
    }

    private static final class Stand {

        private final UUID forger = UUID.nameUUIDFromBytes("Forger".getBytes());
        private final CapturingLogSink console = new CapturingLogSink();
        private final TickQueue ticks = new TickQueue();
        private final SuspectRoster roster = new SuspectRoster(Duration.ofMinutes(15));
        private final FakeStaffRoster staff = new FakeStaffRoster();
        private final FakeStaff mod = staff.add("Mod", true, false);
        private final List<FlagContext> ladder = new ArrayList<>();
        private final List<Runnable> background = new ArrayList<>();
        private final BadPacketWatch watch;
        private long now = 1_000_000L;

        private Stand(boolean debug) {
            Logbook log = new PluginLogbook(TestLogs.capturing(console, new ManualClock(0), debug));
            MessageBook messages = TestConfigs.messages(new ConfigReference(EffectiveConfig.pluginDefaults(
                    TestConfigs.core("language: en"))));
            SubscriberSet subscribers = new SubscriberSet();
            subscribers.add(mod.id());
            AlertTexts texts = new AlertTexts(messages);
            SubscriberBroadcast broadcast = new SubscriberBroadcast(staff, staff,
                    () -> new AlertOutput(true, AlertSound.pluginDefaults()),
                    new AlertSoundResolver(name -> true, new WarnOnce(log)), texts, log);
            StaffAlerts alerts = new StaffAlerts(subscribers, subscribers, texts, broadcast);
            FlagProcessor flags = new FlagProcessor(new ViolationLedger(), ticks, alerts, ladder::add);
            DetectionFlow flow = new DetectionFlow(ScoringRules::pluginDefaults, () -> new ReportingPolicy(true, 0.5),
                    new VerdictScreen(FixedModels::ultraOn, log), alerts, event -> { }, () -> BufferReactions.NONE,
                    ticks, flags, log);
            watch = new BadPacketWatch(new BadPacketTally(), BadPacketSettings::pluginDefaults, () -> true,
                    () -> now, background::add, new BadPacketStrike(roster, flow, alerts), roster, log);
            roster.admit(forger, "Forger", SuspectKind.JAVA, 0L);
            ticks.online.add(forger);
        }

        private void forgeFor(int seconds) {
            for (int second = 0; second < seconds; second++) {
                watch.observed(new BadPacketObserved(forger, BadPacketKind.FORGED_TELEPORT_CONFIRM));
                watch.observed(new BadPacketObserved(forger, BadPacketKind.FORGED_TELEPORT_CONFIRM));
                now += 1_000;
                List<Runnable> due = new ArrayList<>(background);
                background.clear();
                due.forEach(Runnable::run);
                ticks.tick();
            }
        }
    }
}
