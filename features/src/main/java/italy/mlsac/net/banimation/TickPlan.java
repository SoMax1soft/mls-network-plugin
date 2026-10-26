// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;

public record TickPlan(List<AnimationCue> cues, boolean finished) {

    public TickPlan {
        cues = List.copyOf(cues);
    }

    TickPlan withoutVictimCues() {
        return new TickPlan(cues.stream().filter(cue -> !(cue instanceof AnimationCue.MovedTo
                || cue instanceof AnimationCue.TitleShown || cue instanceof AnimationCue.VictimHidden
                || cue instanceof AnimationCue.ItemDropped)).toList(), finished);
    }

    static TickPlan finishedNow() {
        return new TickPlan(List.of(), true);
    }
}
