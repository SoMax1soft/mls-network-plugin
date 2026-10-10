// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trigger;

import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRulePlan;

final class RuleLatch {

    private static final long NEVER = Long.MIN_VALUE;

    private final BufferRulePlan rule;
    private final double threshold;
    private boolean fired;
    private long lastFiredAt = NEVER;

    RuleLatch(BufferRulePlan rule, double threshold, boolean fired) {
        this.rule = rule;
        this.threshold = threshold;
        this.fired = fired;
    }

    BufferRulePlan rule() {
        return rule;
    }

    boolean fired() {
        return fired;
    }

    boolean cross(double buffer, long playerTime, BufferFiring firing) {
        if (buffer < threshold) {
            fired = false;
            return false;
        }
        if (fired) {
            return false;
        }
        fired = true;
        if (!firing.fires() || !cooledDown(playerTime)) {
            return false;
        }
        lastFiredAt = playerTime;
        return true;
    }

    void rearmBelow(double buffer) {
        if (buffer < threshold) {
            fired = false;
        }
    }

    private boolean cooledDown(long playerTime) {
        return lastFiredAt == NEVER || playerTime - lastFiredAt >= rule.quietPeriodMillis();
    }
}
