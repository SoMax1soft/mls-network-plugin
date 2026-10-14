// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.integrity.IntegrityAudience;
import italy.mlsac.net.scoring.AlertStep;
import italy.mlsac.net.scoring.DetectionAudience;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.SuspectRecord;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StaffAlerts implements DetectionAudience, IntegrityAudience {

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
        if (!verdict.origin().visibleInGame()) {
            return;
        }
        broadcast.quietly(monitorSubscribers,
                texts.monitorLine(verdict.displayName(), suspect.name(), verdict.probability()));
    }

    @Override
    public void stepsCrossed(SuspectRecord suspect, ScreenedVerdict verdict, List<AlertStep> steps, double buffer) {
        StepAlertTemplate template = StepAlertTemplate.of(verdict.origin());
        Set<String> distinctLines = new LinkedHashSet<>();
        for (AlertStep step : steps) {
            distinctLines.add(texts.stepAlert(template, suspect.name(), verdict.probability(), buffer,
                    verdict.displayName(), step.percent()));
        }
        distinctLines.forEach(line -> template.deliver(broadcast, alertSubscribers, line));
    }

    @Override
    public void badPackets(String playerNick, BadPacketKind kind, int count, int windowSeconds) {
        broadcast.consoleOnly(texts.badPackets(playerNick, kind, count, windowSeconds), ConsoleCopy.DEBUG_ONLY);
    }

    @Override
    public void flagRaised(FlagContext flag) {
        String line = texts.flagAlert(flag.playerNick(), flag.probability(), flag.buffer(), flag.offenceLevel());
        if (!flag.origin().visibleInGame()) {
            broadcast.consoleOnly(line, ConsoleCopy.ALWAYS);
            return;
        }
        broadcast.alert(alertSubscribers, line);
        broadcast.quietly(monitorSubscribers,
                texts.monitorLine(AlertTexts.FLAG_MODEL, flag.playerNick(), flag.probability()));
    }
}
