// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trigger;

import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.InGameAction;
import java.util.List;
import java.util.stream.Collectors;

public record BufferTrigger(TriggerContext context, List<BufferRulePlan> firedRules) {

    public BufferTrigger {
        firedRules = List.copyOf(firedRules);
    }

    public int highestPercent() {
        return firedRules.stream().mapToInt(BufferRulePlan::percent).max().orElse(0);
    }

    public List<InGameAction> actions() {
        return firedRules.stream().flatMap(rule -> rule.actions().stream()).collect(Collectors.toUnmodifiableList());
    }
}
