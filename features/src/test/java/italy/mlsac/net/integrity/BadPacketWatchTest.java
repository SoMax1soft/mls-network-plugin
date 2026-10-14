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
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.logbook.WarnOnce;
import italy.mlsac.net.scoring.AlertEvent;
import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.DetectionAudience;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.FlagProcessor;
import italy.mlsac.net.scoring.ReportingPolicy;
import italy.mlsac.net.scoring.ScoringRules;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.VerdictScreen;
import italy.mlsac.net.scoring.ViolationLedger;
import italy.mlsac.net.testkit.FakeStaff;
import italy.mlsac.net.testkit.FakeStaffRoster;
import italy.mlsac.net.testkit.FixedModels;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TickQueue;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageBook;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BadPacketWatchTest {

    private final UUID cheater = UUID.nameUUIDFromBytes("Cheater".getBytes());
    private final RecordingLog log = new RecordingLog();
    private final TickQueue ticks = new TickQueue();
    private final ViolationLedger ledger = new ViolationLedger();
    private final SuspectRoster roster = new SuspectRoster(Duration.ofMinutes(15));
    private final FakeStaffRoster staff = new FakeStaffRoster();
    private final SubscriberSet alertSubscribers = new SubscriberSet();
    private final List<AlertEvent> backendAlerts = new ArrayList<>();
    private final List<FlagContext> ladderSteps = new ArrayList<>();
    private final List<Runnable> background = new ArrayList<>();
    private final FixedModels models = FixedModels.ultraOn();
    private final MessageBook messages = messagesIn("en");
    private BadPacketSettings settings = BadPacketSettings.pluginDefaults();
    private boolean detectionEnabled = true;
    private long now = 1_000_000L;
    private final FakeStaff mod = staff.add("Mod", true, false);
    private final BadPacketWatch watch = watch();

    @Test
    void oneOrTwoForgedConfirmsAreOnlyLogged() {
        admit();
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        now += 10_000;
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        runBackground();
        assertTrue(mod.received().isEmpty());
        assertEquals(0.0, record().buffer());
        assertEquals(2, log.debugs.size());
        assertTrue(log.debugs.get(1).contains("forged-teleport-confirm from Cheater (2/60s, alert at 3)"));
    }

    @Test
    void thirdForgedConfirmWithinTheWindowAlertsAndStepsTheBufferLikeAVerdict() {
        admit();
        for (int packet = 0; packet < 3; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
            now += 20_000;
        }
        runBackground();
        assertTrue(mod.received().isEmpty());
        assertTrue(mod.sounds().isEmpty());
        assertEquals(1, consoleStrikes().size());
        assertTrue(consoleStrikes().get(0).endsWith("Cheater Bad packets: forged teleport confirm (3/60s)"),
                consoleStrikes().get(0));
        assertEquals(9.0, record().buffer(), 1e-9);
        assertTrue(backendAlerts.isEmpty());
    }

    @Test
    void eventsSpreadWiderThanTheWindowNeverAlert() {
        admit();
        for (int packet = 0; packet < 10; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
            now += 31_000;
        }
        runBackground();
        assertTrue(mod.received().isEmpty());
        assertEquals(0.0, record().buffer());
    }

    @Test
    void sustainedForgeryStrikesOncePerIntervalAndReachesTheLadder() {
        admit();
        for (int second = 0; second < 60; second++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
            now += 1_000;
            runBackground();
            ticks.tick();
        }
        assertEquals(6, consoleStrikes().size());
        assertTrue(mod.received().isEmpty());
        assertTrue(mod.sounds().isEmpty());
        assertFalse(record().hasChecks());
        assertTrue(log.infos.stream().anyMatch(line -> line.endsWith("Cheater Got VL (VL:1) (Buffer: 20.0)")
                || line.endsWith("Cheater Got VL (VL:1) (Buffer: 20,0)")), log.infos.toString());
        assertEquals(1, ladderSteps.size());
        assertEquals(1, ladderSteps.get(0).offenceLevel());
        assertEquals(1.0, ladderSteps.get(0).probability());
        assertTrue(backendAlerts.isEmpty());
    }

    @Test
    void integrityVerdictCountsInEveryModelModeEvenWithTheModelSwitchedOff() {
        models.ultra = false;
        models.disabled.add("integrity");
        models.onlyAlert.add("integrity");
        admit();
        for (int packet = 0; packet < 3; packet++) {
            observe(BadPacketKind.TICK_END_SPAM);
        }
        runBackground();
        assertEquals(9.0, record().buffer(), 1e-9);
        assertTrue(consoleStrikes().get(0).endsWith("Cheater Bad packets: tick-end spam (3/60s)"));
    }

    @Test
    void playersOutsideTheRosterNeverStrike() {
        for (int packet = 0; packet < 5; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        }
        runBackground();
        assertTrue(mod.received().isEmpty());
        assertTrue(roster.active(cheater).isEmpty());
    }

    @Test
    void detectionOffOrBadPacketsOffIgnoresEverything() {
        admit();
        detectionEnabled = false;
        for (int packet = 0; packet < 5; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        }
        detectionEnabled = true;
        settings = new BadPacketSettings(false, 3, 60, 10, 30, 200);
        for (int packet = 0; packet < 5; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        }
        runBackground();
        assertTrue(mod.received().isEmpty());
        assertTrue(log.debugs.isEmpty());
    }

    @Test
    void configuredThresholdAndWindowAreHonoured() {
        settings = new BadPacketSettings(true, 5, 10, 0, 30, 200);
        admit();
        for (int packet = 0; packet < 4; packet++) {
            observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
            now += 3_000;
        }
        runBackground();
        assertTrue(consoleStrikes().isEmpty());
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        runBackground();
        assertTrue(consoleStrikes().isEmpty());
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        runBackground();
        assertEquals(1, consoleStrikes().size());
        assertTrue(consoleStrikes().get(0).endsWith("(5/10s)"), consoleStrikes().get(0));
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        runBackground();
        assertEquals(2, consoleStrikes().size());
        assertTrue(mod.received().isEmpty());
    }

    @Test
    void departureForgetsTheTally() {
        admit();
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        watch.left(cheater);
        observe(BadPacketKind.FORGED_TELEPORT_CONFIRM);
        runBackground();
        assertTrue(mod.received().isEmpty());
    }

    @Test
    void everyBundledLanguageHasTheBadPacketTexts() {
        for (LanguageId language : LanguageId.values()) {
            AlertTexts texts = new AlertTexts(messagesIn(language.id()));
            for (BadPacketKind kind : BadPacketKind.values()) {
                String line = texts.strip(texts.badPackets("Cheater", kind, 4, 60));
                assertFalse(line.contains("No text"), language + " " + line);
                assertTrue(line.contains("Cheater") && line.contains("4/60"), language + " " + line);
            }
        }
    }

    private BadPacketWatch watch() {
        alertSubscribers.add(mod.id());
        AlertTexts texts = new AlertTexts(messages);
        SubscriberBroadcast broadcast = new SubscriberBroadcast(staff, staff,
                () -> new AlertOutput(false, AlertSound.pluginDefaults()),
                new AlertSoundResolver(name -> true, new WarnOnce(log)), texts, log);
        StaffAlerts alerts = new StaffAlerts(alertSubscribers, new SubscriberSet(), texts, broadcast);
        DetectionAudience audience = alerts;
        FlagProcessor flags = new FlagProcessor(ledger, ticks, audience, ladderSteps::add);
        DetectionFlow flow = new DetectionFlow(ScoringRules::pluginDefaults, () -> new ReportingPolicy(true, 0.5),
                new VerdictScreen(() -> models, log), audience, backendAlerts::add, () -> BufferReactions.NONE, ticks,
                flags, log);
        return new BadPacketWatch(new BadPacketTally(), () -> settings, () -> detectionEnabled, () -> now,
                background::add, new BadPacketStrike(roster, flow, alerts), roster, log);
    }

    private void admit() {
        roster.admit(cheater, "Cheater", SuspectKind.JAVA, 0L);
        ticks.online.add(cheater);
    }

    private SuspectRecord record() {
        return roster.active(cheater).orElseThrow();
    }

    private void observe(BadPacketKind kind) {
        watch.observed(new BadPacketObserved(cheater, kind));
    }

    private void runBackground() {
        List<Runnable> due = new ArrayList<>(background);
        background.clear();
        due.forEach(Runnable::run);
    }

    private List<String> consoleStrikes() {
        return log.debugs.stream().filter(line -> line.contains("Bad packets:") && !line.startsWith("[Detection]"))
                .toList();
    }

    private static MessageBook messagesIn(String language) {
        return TestConfigs.messages(new ConfigReference(EffectiveConfig.pluginDefaults(
                TestConfigs.core("language: " + language))));
    }
}
