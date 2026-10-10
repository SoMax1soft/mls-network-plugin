// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.suspect;

import italy.mlsac.net.suppression.effect.EffectBook;
import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TimedAction;
import italy.mlsac.net.suppression.trigger.RuleLatches;
import java.util.List;

public final class SuspectState {

    private final PresenceClock presence = new PresenceClock();
    private final EffectBook book = new EffectBook();
    private RuleLatches latches;
    private volatile EffectSnapshot published = EffectSnapshot.empty();

    SuspectState(RuleLatches latches) {
        this.latches = latches;
    }

    public EffectSnapshot snapshot() {
        return published;
    }

    public synchronized EffectSnapshot record(TimedAction action, long now) {
        long playerTime = presence.playerTime(now);
        book.pruneEndedBy(playerTime);
        book.add(action.effectFrom(playerTime));
        return publish();
    }

    public synchronized List<BufferRulePlan> evaluate(double buffer, long now, SuppressionPlan plan) {
        return latches.evaluate(buffer, presence.playerTime(now), plan);
    }

    public synchronized void rearmBelow(double buffer) {
        latches.rearmBelow(buffer);
    }

    public synchronized List<Integer> firedPercents() {
        return latches.firedPercents();
    }

    synchronized void replaceLatches(RuleLatches fresh) {
        latches = fresh;
    }

    public synchronized boolean online() {
        return presence.online();
    }

    synchronized EffectSnapshot leave(long now) {
        presence.leave(now);
        return publish();
    }

    synchronized EffectSnapshot rejoin(long now) {
        presence.rejoin(now);
        book.pruneEndedBy(presence.playerTime(now));
        return publish();
    }

    synchronized long offlineFor(long now) {
        return presence.offlineFor(now);
    }

    synchronized EffectSnapshot sweep(long now) {
        book.pruneEndedBy(presence.playerTime(now));
        return publish();
    }

    private EffectSnapshot publish() {
        published = book.snapshot(presence.timeShift());
        return published;
    }
}
