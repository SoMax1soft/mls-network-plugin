// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record SuppressionSettings(boolean repeatLast, List<SuppressionStep> steps, boolean bufferEnabled,
                                  List<BufferRule> bufferRules) {

    public SuppressionSettings {
        steps = List.copyOf(steps);
        bufferRules = List.copyOf(bufferRules);
    }

    public static SuppressionSettings emptyWithDefaultRules() {
        return new SuppressionSettings(false, List.of(), false, DefaultBufferRules.rules());
    }

    public Optional<SuppressionStep> stepForFlag(int flag) {
        Optional<SuppressionStep> exact = steps.stream().filter(step -> step.flag() == flag).findFirst();
        if (exact.isPresent() || !repeatLast) {
            return exact;
        }
        Optional<SuppressionStep> highest = steps.stream().max(Comparator.comparingInt(SuppressionStep::flag));
        return highest.filter(step -> flag > step.flag());
    }
}
