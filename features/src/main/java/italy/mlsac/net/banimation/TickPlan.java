// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;

public record TickPlan(List<AnimationCue> cues, boolean finished) {

    public TickPlan {
        cues = List.copyOf(cues);
    }

    static TickPlan finishedNow() {
        return new TickPlan(List.of(), true);
    }
}
