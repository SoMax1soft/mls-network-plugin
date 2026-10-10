// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trigger;

import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import java.util.ArrayList;
import java.util.List;

public final class RuleLatches {

    private final List<RuleLatch> latches;

    private RuleLatches(List<RuleLatch> latches) {
        this.latches = latches;
    }

    public static RuleLatches armed(SuppressionPlan plan) {
        return build(plan, Double.NEGATIVE_INFINITY);
    }

    public static RuleLatches forCurrentBuffer(SuppressionPlan plan, double currentBuffer) {
        return build(plan, currentBuffer);
    }

    private static RuleLatches build(SuppressionPlan plan, double currentBuffer) {
        List<RuleLatch> latches = new ArrayList<>();
        for (BufferRulePlan rule : plan.bufferRules().tracked()) {
            double threshold = plan.thresholdOf(rule);
            latches.add(new RuleLatch(rule, threshold, currentBuffer >= threshold));
        }
        return new RuleLatches(latches);
    }

    public List<BufferRulePlan> evaluate(double buffer, long playerTime, SuppressionPlan plan) {
        BufferFiring firing = plan.bufferRules().firing();
        List<BufferRulePlan> fired = new ArrayList<>();
        for (RuleLatch latch : latches) {
            if (latch.cross(buffer, playerTime, firing)) {
                fired.add(latch.rule());
            }
        }
        return fired;
    }

    public void rearmBelow(double buffer) {
        latches.forEach(latch -> latch.rearmBelow(buffer));
    }

    public List<Integer> firedPercents() {
        List<Integer> percents = new ArrayList<>();
        for (RuleLatch latch : latches) {
            if (latch.fired()) {
                percents.add(latch.rule().percent());
            }
        }
        return percents;
    }
}
