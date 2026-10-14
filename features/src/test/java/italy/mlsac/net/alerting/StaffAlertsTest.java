// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.logbook.WarnOnce;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import italy.mlsac.net.scoring.AlertStep;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.VerdictOrigin;
import italy.mlsac.net.scoring.VerdictWeight;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.FakeStaff;
import italy.mlsac.net.testkit.FakeStaffRoster;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffAlertsTest {

    private final MessageBook messages = BundledResources.english();
    private final FakeStaffRoster roster = new FakeStaffRoster();
    private final RecordingLog log = new RecordingLog();
    private final SubscriberSet alerts = new SubscriberSet();
    private final SubscriberSet monitors = new SubscriberSet();
    private AlertOutput output = new AlertOutput(true, AlertSound.pluginDefaults());
    private Set<String> knownSounds = Set.of(AlertSound.DEFAULT_TYPE);
    private StaffAlerts staffAlerts;
    private Locale previous;

    @BeforeEach
    void setUp() {
        previous = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);
        AlertTexts texts = new AlertTexts(messages);
        AlertSoundResolver sounds = new AlertSoundResolver(name -> knownSounds.contains(name), new WarnOnce(log));
        SubscriberBroadcast broadcast = new SubscriberBroadcast(roster, roster, () -> output, sounds, texts, log);
        staffAlerts = new StaffAlerts(alerts, monitors, texts, broadcast);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(Locale.Category.FORMAT, previous);
    }

    @Test
    void stepAlertTextVector() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        SuspectRecord suspect = new SuspectRecord(UUID.randomUUID(), "Cheater", SuspectKind.JAVA);
        staffAlerts.stepsCrossed(suspect, new ScreenedVerdict("fast", 0.953, VerdictWeight.COUNTED, "Mini"),
                List.of(AlertStep.of(0.33)), 13.2);
        String expected = messages.prefixed("alert-format", TemplateValues.of("{PLAYER}", "Cheater")
                .and("{PROBABILITY}", "95.3%").and("{BUFFER}", "13.2").and("{VL}", "0").and("{MODEL}", "Mini")
                .and("{STEP_PERCENT}", "33"));
        assertEquals(List.of(expected), mod.received());
        assertTrue(mod.received().get(0).contains("[Mini]"));
        assertEquals(List.of(AlertSound.DEFAULT_TYPE), mod.sounds());
        assertEquals(1, log.infos.size());
        assertFalse(log.infos.get(0).contains("§"));
    }

    @Test
    void integrityStepAlertGoesOnlyToTheDebugConsoleWithoutAFakeProbability() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        monitors.add(mod.id());
        SuspectRecord suspect = new SuspectRecord(UUID.randomUUID(), "Forger", SuspectKind.JAVA);
        ScreenedVerdict verdict = new ScreenedVerdict("integrity", 1.0, VerdictWeight.COUNTED, "Bad packets",
                VerdictOrigin.PACKET_INTEGRITY);
        staffAlerts.answerSeen(suspect, verdict);
        staffAlerts.stepsCrossed(suspect, verdict, List.of(AlertStep.of(0.33)), 16.1);
        staffAlerts.badPackets("Forger", italy.mlsac.net.detection.BadPacketKind.TICK_END_SPAM, 3, 60);
        assertTrue(mod.received().isEmpty(), mod.received().toString());
        assertTrue(mod.sounds().isEmpty());
        assertTrue(log.infos.isEmpty(), log.infos.toString());
        assertEquals(2, log.debugs.size());
        assertTrue(log.debugs.get(0).endsWith("[Bad packets] Forger Failed check (Packet integrity) (Buffer: 16.1)"),
                log.debugs.get(0));
        assertTrue(log.debugs.get(1).endsWith("Forger Bad packets: tick-end spam (3/60s)"), log.debugs.get(1));
        log.debugs.forEach(line -> assertFalse(line.contains("§") || line.contains("Aim-Ai") || line.contains("100%"),
                line));
    }

    @Test
    void integrityFlagIsConsoleOnlyEvenWithTheConsoleCopyOff() {
        output = new AlertOutput(false, AlertSound.pluginDefaults());
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        monitors.add(mod.id());
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "Forger", 1, 1.0, 20.0,
                VerdictOrigin.PACKET_INTEGRITY));
        assertTrue(mod.received().isEmpty(), mod.received().toString());
        assertTrue(mod.sounds().isEmpty());
        assertEquals(1, log.infos.size());
        assertTrue(log.infos.get(0).endsWith("Forger Got VL (VL:1) (Buffer: 20.0)"), log.infos.get(0));
        assertFalse(log.infos.get(0).contains("100%"));
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "Cheater", 1, 0.97, 20.0));
        assertEquals(2, mod.received().size());
        assertEquals(1, log.infos.size());
    }

    @Test
    void modelStepAlertsKeepTheirTemplateAndConsoleCopy() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        SuspectRecord suspect = new SuspectRecord(UUID.randomUUID(), "Cheater", SuspectKind.JAVA);
        staffAlerts.stepsCrossed(suspect, new ScreenedVerdict("ultra", 0.97, VerdictWeight.COUNTED, "Ultra",
                VerdictOrigin.MODEL_ANSWER), List.of(AlertStep.of(0.33)), 13.2);
        assertTrue(messages.strip(mod.received().get(0)).contains("[Ultra] Cheater Failed check (Aim-Ai: 97%)"));
        assertEquals(1, log.infos.size());
        assertTrue(log.debugs.isEmpty());
        output = new AlertOutput(false, AlertSound.pluginDefaults());
        staffAlerts.stepsCrossed(suspect, new ScreenedVerdict("ultra", 0.97, VerdictWeight.COUNTED, "Ultra"),
                List.of(AlertStep.of(0.66)), 26.4);
        assertEquals(1, log.infos.size());
        assertTrue(log.debugs.isEmpty());
    }

    @Test
    void flagAlertCarriesVlUnknownModelAndExtraMonitorLine() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        monitors.add(mod.id());
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "Cheater", 3, 0.97, 20.0));
        assertEquals(2, mod.received().size());
        assertTrue(mod.received().get(0).contains("VL:§f3"));
        assertTrue(mod.received().get(1).contains("[Unknown]"));
        assertTrue(mod.received().get(1).contains("97%"));
    }

    @Test
    void subscriberWithoutStaffPermissionReceivesNothingButHearsTheSound() {
        FakeStaff player = roster.add("Player", false, false);
        alerts.add(player.id());
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "Cheater", 1, 0.97, 20.0));
        assertTrue(player.received().isEmpty());
        assertEquals(1, player.sounds().size());
    }

    @Test
    void operatorWithoutPermissionReceivesAlerts() {
        FakeStaff op = roster.add("Op", false, true);
        alerts.add(op.id());
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "Cheater", 1, 0.97, 20.0));
        assertEquals(1, op.received().size());
    }

    @Test
    void monitorLinesUseTruncatedColouredPercent() {
        FakeStaff mod = roster.add("Mod", true, false);
        monitors.add(mod.id());
        SuspectRecord suspect = new SuspectRecord(UUID.randomUUID(), "Cheater", SuspectKind.JAVA);
        staffAlerts.answerSeen(suspect, new ScreenedVerdict("ultra", 0.999, VerdictWeight.COUNTED, "Ultra"));
        String expected = messages.text("monitor-format", TemplateValues.of("{MODEL}", "Ultra")
                .and("{PLAYER}", "Cheater").and("{PROBABILITY_COLORED}", "&#FF0000" + "99%"));
        assertEquals(List.of(expected), mod.received());
        assertEquals("&#90EE90" + "45%", MonitorShade.colored(0.45));
        assertEquals("&#00FF00" + "39%", MonitorShade.colored(0.399));
    }

    @Test
    void unknownSoundFallsBackOnceAndMissingFallbackIsSilent() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        output = new AlertOutput(false, new AlertSound(true, "NOPE", 1f, 1f));
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "A", 1, 0.9, 20));
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "A", 1, 0.9, 20));
        assertEquals(List.of(AlertSound.DEFAULT_TYPE, AlertSound.DEFAULT_TYPE), mod.sounds());
        assertEquals(1, log.warnings.size());
        knownSounds = Set.of();
        setUp();
        staffAlerts.flagRaised(new FlagContext(UUID.randomUUID(), "A", 1, 0.9, 20));
        assertEquals(2, mod.sounds().size());
    }

    @Test
    void subscriptionToggles() {
        SubscriberSet set = new SubscriberSet();
        UUID id = UUID.randomUUID();
        assertTrue(set.toggle(id));
        assertFalse(set.toggle(id));
        assertTrue(set.toggle(id));
        FakeStaff mod = roster.add("Mod", true, false);
        StaffEnrollment enrollment = new StaffEnrollment(alerts, monitors, roster);
        enrollment.joinDelayElapsed(mod.id());
        assertTrue(alerts.contains(mod.id()));
        FakeStaff op = roster.add("Op", false, true);
        enrollment.joinDelayElapsed(op.id());
        assertFalse(alerts.contains(op.id()));
        monitors.add(mod.id());
        enrollment.left(mod.id());
        assertFalse(alerts.contains(mod.id()));
        assertFalse(monitors.contains(mod.id()));
    }

    @Test
    void crossServerAlertsSanitiseDedupeAndDropReports() {
        FakeStaff mod = roster.add("Mod", true, false);
        FakeStaff player = roster.add("Player", false, false);
        CrossServerFeed feed = new CrossServerFeed(new AlertTexts(messages), model -> model.equals("fast") ? "Mini" : model,
                roster, roster, log);
        RemoteEvent event = new RemoteEvent("e1", Optional.of("alert"), Optional.of("fast"), Optional.empty(),
                Optional.of("  lob\u0001by  "), Optional.of("Bad\u007FGuy"), java.util.OptionalDouble.empty(),
                java.util.OptionalDouble.of(2.6), 0.95, 13.2, false);
        feed.receive(event);
        feed.receive(event);
        assertEquals(1, mod.received().size());
        assertTrue(player.received().isEmpty());
        String line = mod.received().get(0);
        assertTrue(line.contains("lobby"));
        assertTrue(line.contains("BadGuy"));
        assertTrue(line.contains("[Mini]"));
        assertEquals(1, log.infos.size());
        feed.receive(new RemoteEvent("e2", Optional.of("player_report"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), java.util.OptionalDouble.empty(), java.util.OptionalDouble.empty(),
                0, 0, false));
        feed.receive(new RemoteEvent("e3", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), java.util.OptionalDouble.empty(), java.util.OptionalDouble.empty(),
                0, 0, true));
        assertEquals(1, mod.received().size());
    }

    @Test
    void crossServerDefaultsAndRounding() {
        RemoteServerFlag alert = RemoteServerFlag.from(new RemoteEvent("x", Optional.empty(), Optional.empty(),
                Optional.of("  "), Optional.of(""), Optional.empty(), java.util.OptionalDouble.of(2.5),
                java.util.OptionalDouble.of(9), 0.5, 1, false)).orElseThrow();
        assertEquals("alert", alert.type());
        assertEquals("unknown", alert.model());
        assertEquals("alert", alert.action());
        assertEquals("unknown", alert.hostLabel());
        assertEquals("Unknown", alert.playerNick());
        assertEquals(3, alert.offenceLevel());
        RemoteServerFlag longOne = RemoteServerFlag.from(new RemoteEvent("y", Optional.of("a".repeat(40)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                java.util.OptionalDouble.empty(), java.util.OptionalDouble.empty(), 0, 0, false)).orElseThrow();
        assertEquals(24, longOne.type().length());
    }
}
