// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record FlagLadder(LadderRepeat repeat, List<LadderStep> steps) {

    public FlagLadder {
        steps = steps.stream().sorted(Comparator.comparingInt(LadderStep::flag))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<LadderStep> stepFor(int flag) {
        for (LadderStep step : steps) {
            if (step.flag() == flag) {
                return Optional.of(step);
            }
        }
        if (repeat != LadderRepeat.REPEAT_LAST || steps.isEmpty()) {
            return Optional.empty();
        }
        LadderStep highest = steps.get(steps.size() - 1);
        return flag > highest.flag() ? Optional.of(highest) : Optional.empty();
    }
}
