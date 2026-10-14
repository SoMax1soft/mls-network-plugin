// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import italy.mlsac.net.logbook.Logbook;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class DetectionFlow {

    private final Supplier<ScoringRules> rules;
    private final Supplier<ReportingPolicy> reporting;
    private final VerdictScreen screen;
    private final StepLadder steps = new StepLadder();
    private final DetectionAudience audience;
    private final DetectionReports reports;
    private final Supplier<BufferReactions> reactions;
    private final PlayerThread players;
    private final FlagProcessor flags;
    private final Logbook log;

    public DetectionFlow(Supplier<ScoringRules> rules, Supplier<ReportingPolicy> reporting, VerdictScreen screen,
                         DetectionAudience audience, DetectionReports reports, Supplier<BufferReactions> reactions,
                         PlayerThread players, FlagProcessor flags, Logbook log) {
        this.rules = rules;
        this.reporting = reporting;
        this.screen = screen;
        this.audience = audience;
        this.reports = reports;
        this.reactions = reactions;
        this.players = players;
        this.flags = flags;
        this.log = log;
    }

    public void process(SuspectRecord suspect, Verdict verdict) {
        Optional<ScreenedVerdict> screened = screen.screen(suspect.name(), verdict);
        if (screened.isEmpty()) {
            return;
        }
        synchronized (suspect) {
            apply(suspect, screened.get(), rules.get());
        }
    }

    private void apply(SuspectRecord suspect, ScreenedVerdict verdict, ScoringRules current) {
        double buffer = verdict.onlyAlert() ? suspect.buffer()
                : verdict.origin().count(suspect, verdict,
                        before -> BufferFormula.next(before, verdict.probability(), current));
        audience.answerSeen(suspect, verdict);
        if (fireSteps(suspect, verdict, buffer, current) && reporting.get().reports(verdict.probability())
                && !IntegrityVerdict.concerns(verdict.model())) {
            reports.alert(new AlertEvent(suspect.id(), suspect.name(), verdict.model(), verdict.probability(), buffer));
        }
        BufferReactions currentReactions = reactions.get();
        AnswerMoment moment = new AnswerMoment(suspect, verdict, buffer);
        if (!verdict.onlyAlert()) {
            currentReactions.bufferMoved(moment);
        }
        currentReactions.answerAccepted(moment);
        if (!verdict.onlyAlert() && current.flags(buffer)) {
            scheduleFlag(suspect, verdict.probability(), verdict.origin());
            double reset = current.resetBuffer();
            suspect.resetBuffer(reset);
            currentReactions.bufferReset(suspect, reset);
        }
    }

    private boolean fireSteps(SuspectRecord suspect, ScreenedVerdict verdict, double buffer, ScoringRules current) {
        double flagThreshold = current.flagThreshold();
        if (!(flagThreshold > 0.0)) {
            return false;
        }
        List<AlertStep> crossed = new ArrayList<>();
        for (AlertStep step : steps.steps(current.stepPercent())) {
            if (suspect.latches().crossed(step.latchName(), buffer, step.threshold(flagThreshold))) {
                crossed.add(step);
            }
        }
        if (crossed.isEmpty()) {
            return false;
        }
        audience.stepsCrossed(suspect, verdict, List.copyOf(crossed), buffer);
        return true;
    }

    private void scheduleFlag(SuspectRecord suspect, double probability, VerdictOrigin origin) {
        boolean scheduled = players.runNextTick(suspect.id(), () -> flags.process(suspect, probability, origin));
        if (!scheduled) {
            log.warn("[Detection] " + suspect.name() + " left before the flag could be handled");
        }
    }
}
