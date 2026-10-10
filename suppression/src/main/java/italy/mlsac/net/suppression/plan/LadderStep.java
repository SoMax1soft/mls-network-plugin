// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record LadderStep(int flag, List<PlannedAction> actions) {

    public LadderStep {
        actions = List.copyOf(actions);
    }

    public List<StagePlan> stages() {
        Map<Integer, StageBuilder> byStage = new TreeMap<>();
        for (PlannedAction action : actions) {
            action.placeInto(byStage.computeIfAbsent(action.stage(), stage -> new StageBuilder()));
        }
        List<StagePlan> stages = new ArrayList<>();
        byStage.values().forEach(builder -> stages.add(builder.build()));
        return List.copyOf(stages);
    }
}
