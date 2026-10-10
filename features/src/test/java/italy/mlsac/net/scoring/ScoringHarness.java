// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import italy.mlsac.net.testkit.FixedModels;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.testkit.TickQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ScoringHarness implements DetectionAudience, DetectionReports, FlagPenalties {

    final RecordingLog log = new RecordingLog();
    final TickQueue ticks = new TickQueue();
    final ViolationLedger ledger = new ViolationLedger();
    final List<String> monitor = new ArrayList<>();
    final List<Long> steps = new ArrayList<>();
    final List<FlagContext> flagAlerts = new ArrayList<>();
    final List<FlagContext> penalties = new ArrayList<>();
    final List<AlertEvent> alertEvents = new ArrayList<>();
    FixedModels models;
    ScoringRules rules = ScoringRules.pluginDefaults();
    ReportingPolicy reporting = new ReportingPolicy(true, 0.80);
    BufferReactions reactions = BufferReactions.NONE;
    final DetectionFlow flow;
    final SuspectRecord suspect;

    ScoringHarness(FixedModels models) {
        this.models = models;
        VerdictScreen screen = new VerdictScreen(() -> this.models, log);
        FlagProcessor flags = new FlagProcessor(ledger, ticks, this, this);
        flow = new DetectionFlow(() -> rules, () -> reporting, screen, this, this, () -> reactions, ticks, flags, log);
        UUID id = UUID.nameUUIDFromBytes("Suspect".getBytes());
        suspect = new SuspectRecord(id, "Suspect", SuspectKind.JAVA);
        ticks.online.add(id);
    }

    void answer(String model, double probability) {
        flow.process(suspect, new Verdict(model, probability));
        ticks.tick();
    }

    @Override
    public void answerSeen(SuspectRecord record, ScreenedVerdict verdict) {
        monitor.add(verdict.displayName() + ":" + verdict.probability());
    }

    @Override
    public void stepsCrossed(SuspectRecord record, ScreenedVerdict verdict, List<AlertStep> crossed, double buffer) {
        crossed.forEach(step -> steps.add(step.key()));
    }

    @Override
    public void flagRaised(FlagContext flag) {
        flagAlerts.add(flag);
    }

    @Override
    public void alert(AlertEvent event) {
        alertEvents.add(event);
    }

    @Override
    public void onFlag(FlagContext flag) {
        penalties.add(flag);
    }
}
