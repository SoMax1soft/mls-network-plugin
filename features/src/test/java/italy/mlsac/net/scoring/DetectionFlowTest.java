// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.FixedModels;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionFlowTest {

    @Test
    void workedExampleProducesTheListedStepsAndFlags() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOn());
        List<Double> afterUpdate = new ArrayList<>();
        List<List<Long>> stepsPerAnswer = new ArrayList<>();
        List<Integer> flagsPerAnswer = new ArrayList<>();
        List<Double> afterAnswer = new ArrayList<>();
        harness.reactions = new BufferReactions() {
            @Override
            public void bufferMoved(AnswerMoment moment) {
                afterUpdate.add(moment.buffer());
            }

            @Override
            public void answerAccepted(AnswerMoment moment) {
            }

            @Override
            public void bufferReset(SuspectRecord suspect, double buffer) {
            }
        };
        for (int answer = 1; answer <= 8; answer++) {
            int stepsBefore = harness.steps.size();
            int flagsBefore = harness.flagAlerts.size();
            harness.answer("ultra", 1.0);
            stepsPerAnswer.add(List.copyOf(harness.steps.subList(stepsBefore, harness.steps.size())));
            flagsPerAnswer.add(harness.flagAlerts.size() - flagsBefore);
            afterAnswer.add(harness.suspect.buffer());
        }
        assertEquals(List.of(9.0, 18.0, 27.0, 36.0, 45.0, 29.0, 38.0, 47.0), rounded(afterUpdate));
        assertEquals(List.of(List.of(), List.of(330L), List.of(660L), List.of(), List.of(990L), List.of(), List.of(),
                List.of(990L)), stepsPerAnswer);
        assertEquals(List.of(0, 0, 0, 0, 1, 0, 0, 1), flagsPerAnswer);
        assertEquals(List.of(9.0, 18.0, 27.0, 36.0, 20.0, 29.0, 38.0, 20.0), rounded(afterAnswer));
        assertEquals(List.of(1, 2), harness.flagAlerts.stream().map(FlagContext::offenceLevel).toList());
        assertEquals(20.0, harness.flagAlerts.get(0).buffer());
        assertEquals(1.0, harness.flagAlerts.get(0).probability());
        assertEquals(harness.flagAlerts, harness.penalties);
    }

    private static List<Double> rounded(List<Double> values) {
        return values.stream().map(value -> Math.round(value * 1e6) / 1e6).toList();
    }

    @Test
    void stepAlertEventsAreGatedByReportingPolicy() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOn());
        harness.reporting = new ReportingPolicy(true, 0.95);
        harness.answer("ultra", 0.94);
        harness.answer("ultra", 0.94);
        harness.answer("ultra", 0.94);
        assertTrue(harness.steps.size() >= 1);
        assertEquals(0, harness.alertEvents.size());
        harness.reporting = new ReportingPolicy(true, 0.80);
        harness.answer("ultra", 1.0);
        assertEquals(1, harness.alertEvents.size());
        AlertEvent event = harness.alertEvents.get(0);
        assertEquals("ultra", event.model());
        assertEquals(harness.suspect.buffer(), event.buffer());
        harness.reporting = new ReportingPolicy(false, 0.0);
        harness.answer("ultra", 1.0);
        harness.answer("ultra", 1.0);
        assertEquals(1, harness.alertEvents.size());
    }

    @Test
    void onlyAlertModelNeverTouchesBufferOrHistory() {
        FixedModels models = FixedModels.ultraOff();
        models.onlyAlert.add("experimental");
        ScoringHarness harness = new ScoringHarness(models);
        long versionBefore = harness.suspect.historyRevision();
        for (int i = 0; i < 100; i++) {
            harness.answer("experimental", 1.0);
        }
        assertEquals(0.0, harness.suspect.buffer());
        assertEquals(0, harness.flagAlerts.size());
        assertEquals(versionBefore, harness.suspect.historyRevision());
        assertEquals(100, harness.monitor.size());
    }

    @Test
    void onlyAlertIsIgnoredForFastAndPro() {
        FixedModels models = FixedModels.ultraOff();
        models.onlyAlert.add("fast");
        models.onlyAlert.add("pro");
        ScoringHarness harness = new ScoringHarness(models);
        for (int i = 0; i < 5; i++) {
            harness.answer("fast", 1.0);
        }
        for (int i = 0; i < 5; i++) {
            harness.answer("pro", 1.0);
        }
        assertEquals(2, harness.flagAlerts.size());
    }

    @Test
    void ultraExclusivity() {
        ScoringHarness ultra = new ScoringHarness(FixedModels.ultraOn());
        ultra.answer("pro", 1.0);
        assertEquals(0, ultra.monitor.size());
        ultra.answer("ultra", 1.0);
        assertEquals(1, ultra.monitor.size());
        ScoringHarness legacy = new ScoringHarness(FixedModels.ultraOff());
        legacy.answer("ultra", 1.0);
        assertEquals(0, legacy.monitor.size());
        legacy.answer("pro", 1.0);
        assertEquals(1, legacy.monitor.size());
    }

    @Test
    void outOfRangeProbabilitiesAreRejectedWithWarning() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOn());
        harness.answer("ultra", 1.2);
        harness.answer("ultra", -0.1);
        harness.answer("ultra", Double.NaN);
        assertEquals(3, harness.log.warnings.size());
        assertEquals("[Detection] Dropped an answer for Suspect: probability 1.2 cannot be a probability, the payload"
                + " is damaged or forged", harness.log.warnings.get(0));
        assertEquals(0, harness.monitor.size());
    }

    @Test
    void disabledModelIsDroppedSilently() {
        FixedModels models = FixedModels.ultraOff();
        models.disabled.add("pro");
        ScoringHarness harness = new ScoringHarness(models);
        harness.answer("pro", 1.0);
        assertEquals(0, harness.monitor.size());
        assertEquals(0, harness.log.warnings.size());
    }

    @Test
    void offlineAtSchedulingSkipsFlagButStillResets() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOn());
        harness.rules = new ScoringRules(0.8, 45, 1.5, 0.5, 40, 20, 0.33);
        for (int i = 0; i < 4; i++) {
            harness.answer("ultra", 1.0);
        }
        harness.ticks.online.clear();
        harness.answer("ultra", 1.0);
        assertEquals(List.of("[Detection] Suspect left before the flag could be handled"), harness.log.warnings);
        assertEquals(20.0, harness.suspect.buffer());
        assertEquals(0, harness.ledger.level(harness.suspect.id()));
    }

    @Test
    void flagSkippedWhenPlayerLeavesBeforeTheTick() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOn());
        for (int i = 0; i < 4; i++) {
            harness.answer("ultra", 1.0);
        }
        harness.flow.process(harness.suspect, new Verdict("ultra", 1.0));
        harness.ticks.online.clear();
        harness.ticks.tick();
        assertEquals(0, harness.flagAlerts.size());
    }

    @Test
    void historiesFollowCountedAnswers() {
        ScoringHarness harness = new ScoringHarness(FixedModels.ultraOff());
        for (int i = 0; i < 60; i++) {
            harness.answer(i % 2 == 0 ? " Pro " : "", 0.9);
        }
        SuspectSnapshot snapshot = harness.suspect.snapshot();
        assertEquals(10, snapshot.shortHistory().size());
        assertEquals(50, snapshot.lastScores().size());
        assertEquals(20, snapshot.modelHistory().size());
        assertEquals("unknown", snapshot.modelHistory().get(19).model());
        assertEquals("pro", snapshot.modelHistory().get(18).model());
        assertEquals(60, snapshot.highProbabilityCount());
        assertEquals(0.9, snapshot.average(), 1e-12);
    }
}
