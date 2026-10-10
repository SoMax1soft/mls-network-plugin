// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record BufferRules(BufferFiring firing, List<BufferRulePlan> rules) {

    public BufferRules {
        rules = rules.stream().sorted(Comparator.comparingInt(BufferRulePlan::percent))
                .collect(Collectors.toUnmodifiableList());
    }

    public static BufferRules defaults(BufferFiring firing) {
        return new BufferRules(firing, DefaultBufferRules.rules());
    }

    public List<BufferRulePlan> tracked() {
        return rules.stream().filter(BufferRulePlan::enabled).collect(Collectors.toUnmodifiableList());
    }
}
