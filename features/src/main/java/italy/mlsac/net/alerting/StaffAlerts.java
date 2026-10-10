// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.scoring.AlertStep;
import italy.mlsac.net.scoring.DetectionAudience;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.SuspectRecord;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StaffAlerts implements DetectionAudience {

    private final SubscriberSet alertSubscribers;
    private final SubscriberSet monitorSubscribers;
    private final AlertTexts texts;
    private final SubscriberBroadcast broadcast;

    public StaffAlerts(SubscriberSet alertSubscribers, SubscriberSet monitorSubscribers, AlertTexts texts,
                       SubscriberBroadcast broadcast) {
        this.alertSubscribers = alertSubscribers;
        this.monitorSubscribers = monitorSubscribers;
        this.texts = texts;
        this.broadcast = broadcast;
    }

    @Override
    public void answerSeen(SuspectRecord suspect, ScreenedVerdict verdict) {
        broadcast.quietly(monitorSubscribers,
                texts.monitorLine(verdict.displayName(), suspect.name(), verdict.probability()));
    }

    @Override
    public void stepsCrossed(SuspectRecord suspect, ScreenedVerdict verdict, List<AlertStep> steps, double buffer) {
        Set<String> distinctLines = new LinkedHashSet<>();
        for (AlertStep step : steps) {
            distinctLines.add(texts.stepAlert(suspect.name(), verdict.probability(), buffer, verdict.displayName(),
                    step.percent()));
        }
        distinctLines.forEach(line -> broadcast.alert(alertSubscribers, line));
    }

    @Override
    public void flagRaised(FlagContext flag) {
        broadcast.alert(alertSubscribers,
                texts.flagAlert(flag.playerNick(), flag.probability(), flag.buffer(), flag.offenceLevel()));
        broadcast.quietly(monitorSubscribers,
                texts.monitorLine(AlertTexts.FLAG_MODEL, flag.playerNick(), flag.probability()));
    }
}
