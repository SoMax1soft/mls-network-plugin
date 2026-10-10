// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.notice.TrickAnnouncements;
import italy.mlsac.net.suppression.plan.InGameActionSink;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TimedAction;
import italy.mlsac.net.suppression.plan.TrickAction;
import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.UUID;

final class OnlineActions implements InGameActionSink {

    private final Round round;
    private final ActionReport report;
    private final TrickAnnouncements announcements;

    OnlineActions(Round round, ActionReport report) {
        this.round = round;
        this.report = report;
        this.announcements = round.scene().broadcast().forOneEvent();
    }

    @Override
    public void timed(TimedAction action) {
        if (round.exempt()) {
            return;
        }
        UUID suspect = round.suspect();
        var state = round.scene().registry().suspectStateOf(suspect, round.plan());
        round.scene().watcher().effectsChanged(suspect, state.record(action, round.now()));
        report.add(action.token());
    }

    @Override
    public void trick(TrickAction action) {
        boolean allowed = !round.exempt() && !round.scene().standing().paused(round.suspect())
                && round.repeat().admit(action.type());
        if (allowed && performed(action)) {
            report.add(action.type().profile().reportWord());
            announcements.announce(action.type(), round.tuningOf(action), round.context());
        }
    }

    private boolean performed(TrickAction action) {
        ActionScene scene = round.scene();
        return scene.bodies().bodyOf(round.suspect())
                .flatMap(body -> scene.tricks().effectFor(action.type())
                        .map(effect -> effect.perform(body, round.tuningOf(action))))
                .orElse(false);
    }

    record Round(ActionScene scene, SuppressionPlan plan, TriggerContext context, TrickRepeat repeat, long now) {

        UUID suspect() {
            return context.suspectId();
        }

        boolean exempt() {
            return scene.standing().exempt(suspect());
        }

        TrickTuning tuningOf(TrickAction action) {
            return plan.tricks().tuningOf(action.type());
        }
    }
}
