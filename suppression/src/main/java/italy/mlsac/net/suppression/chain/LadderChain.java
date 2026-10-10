// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.chain;

import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.plan.StagePlan;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.List;

public final class LadderChain {

    private final TriggerContext context;
    private final List<ChainStage> stages;
    private int next;

    private LadderChain(TriggerContext context, List<ChainStage> stages) {
        this.context = context;
        this.stages = stages;
    }

    public static LadderChain start(TriggerContext context, LadderStep step, long beganAt) {
        List<StagePlan> plans = step.stages();
        List<ChainStage> stages = new ArrayList<>(plans.size());
        long offset = beganAt;
        for (int i = 0; i < plans.size(); i++) {
            StagePlan plan = plans.get(i);
            stages.add(new ChainStage(i + 1, plans.size(), offset, plan));
            offset += plan.lastsMillis();
        }
        return new LadderChain(context, List.copyOf(stages));
    }

    public TriggerContext context() {
        return context;
    }

    public List<ChainStage> stages() {
        return stages;
    }

    List<DueStage> takeDue(long now) {
        List<DueStage> due = new ArrayList<>();
        while (next < stages.size() && stages.get(next).startAt() <= now) {
            due.add(new DueStage(context, stages.get(next)));
            next++;
        }
        return due;
    }

    boolean finished() {
        return next >= stages.size();
    }

    int pendingCommands() {
        int count = 0;
        for (int i = next; i < stages.size(); i++) {
            count += stages.get(i).plan().commands().size();
        }
        return count;
    }

    void dropPending() {
        next = stages.size();
    }
}
