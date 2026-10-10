// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.logbook.WarnOnce;
import italy.mlsac.net.scoring.AlertStep;
import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.FlagProcessor;
import italy.mlsac.net.scoring.ReportingPolicy;
import italy.mlsac.net.scoring.ScoringRules;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.Verdict;
import italy.mlsac.net.scoring.VerdictScreen;
import italy.mlsac.net.scoring.VerdictWeight;
import italy.mlsac.net.scoring.ViolationLedger;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.FakeStaff;
import italy.mlsac.net.testkit.FakeStaffRoster;
import italy.mlsac.net.testkit.FixedModels;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.testkit.TickQueue;
import italy.mlsac.net.text.MessageBook;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OneLinePerEventTest {

    private static final ScoringRules STEEP = new ScoringRules(0.80, 130.0, 1.5, 0.50, 40.0, 20.0,
            ScoringRules.DEFAULT_STEP_PERCENT);

    private final MessageBook messages = BundledResources.english();
    private final FakeStaffRoster roster = new FakeStaffRoster();
    private final RecordingLog log = new RecordingLog();
    private final SubscriberSet alerts = new SubscriberSet();
    private final SubscriberSet monitors = new SubscriberSet();
    private final TickQueue ticks = new TickQueue();
    private StaffAlerts staffAlerts;
    private Locale previous;

    @BeforeEach
    void setUp() {
        previous = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);
        AlertTexts texts = new AlertTexts(messages);
        AlertSoundResolver sounds = new AlertSoundResolver(name -> true, new WarnOnce(log));
        SubscriberBroadcast broadcast = new SubscriberBroadcast(roster, roster,
                () -> new AlertOutput(true, AlertSound.pluginDefaults()), sounds, texts, log);
        staffAlerts = new StaffAlerts(alerts, monitors, texts, broadcast);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(Locale.Category.FORMAT, previous);
    }

    @Test
    void anAnswerCrossingTwoStepsReachesTheOwnerOnceEvenWhenHeIsTheSuspect() {
        FakeStaff owner = roster.add("smotryaska122", true, true);
        alerts.add(owner.id());
        alerts.add(owner.id());
        SuspectRecord suspect = new SuspectRecord(owner.id(), owner.name(), SuspectKind.JAVA);
        ticks.online.add(owner.id());
        DetectionFlow flow = flow();
        flow.process(suspect, new Verdict("ultra", 0.997));
        ticks.tick();
        assertEquals(1, owner.received().size());
        flow.process(suspect, new Verdict("ultra", 0.997));
        ticks.tick();
        List<String> afterSecond = owner.received().subList(1, owner.received().size());
        assertEquals(2, afterSecond.size());
        assertTrue(afterSecond.get(0).contains("Failed check"));
        assertTrue(afterSecond.get(1).contains("Got VL"));
        assertNoLineTwice(owner.received());
        assertEquals(owner.received().size(), owner.sounds().size());
        assertEquals(owner.received().size(), log.infos.size());
    }

    @Test
    void identicalStepLinesCollapseButDifferentOnesStay() {
        FakeStaff mod = roster.add("Mod", true, false);
        alerts.add(mod.id());
        SuspectRecord suspect = new SuspectRecord(java.util.UUID.randomUUID(), "Cheater", SuspectKind.JAVA);
        ScreenedVerdict verdict = new ScreenedVerdict("ultra", 0.997, VerdictWeight.COUNTED, "Ultra");
        staffAlerts.stepsCrossed(suspect, verdict, List.of(AlertStep.of(0.33), AlertStep.of(0.66),
                AlertStep.of(0.99)), 45.4);
        assertEquals(1, mod.received().size());
        assertEquals(1, mod.sounds().size());
        assertEquals(1, log.infos.size());
    }

    @Test
    void everyPermissionMixGetsEachLineOnce() {
        FakeStaff opOnly = roster.add("OpOnly", false, true);
        FakeStaff staffOnly = roster.add("StaffOnly", true, false);
        FakeStaff both = roster.add("Both", true, true);
        alerts.add(opOnly.id());
        alerts.add(staffOnly.id());
        alerts.add(both.id());
        alerts.add(both.id());
        SuspectRecord suspect = new SuspectRecord(both.id(), both.name(), SuspectKind.JAVA);
        staffAlerts.stepsCrossed(suspect, new ScreenedVerdict("ultra", 0.99, VerdictWeight.COUNTED, "Ultra"),
                List.of(AlertStep.of(0.66), AlertStep.of(0.99)), 45.4);
        for (FakeStaff member : List.of(opOnly, staffOnly, both)) {
            assertEquals(1, member.received().size(), member.name());
        }
    }

    private DetectionFlow flow() {
        VerdictScreen screen = new VerdictScreen(FixedModels::ultraOn, log);
        FlagProcessor flags = new FlagProcessor(new ViolationLedger(), ticks, staffAlerts, flag -> { });
        return new DetectionFlow(() -> STEEP, () -> new ReportingPolicy(false, 0.80), screen, staffAlerts,
                event -> { }, () -> BufferReactions.NONE, ticks, flags, log);
    }

    private static void assertNoLineTwice(List<String> lines) {
        assertEquals(new ArrayList<>(new LinkedHashSet<>(lines)), lines);
    }
}
