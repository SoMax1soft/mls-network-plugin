// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;
import java.util.Optional;

public record AnimationScript(String name, int lengthTicks, boolean holdVictimStill, boolean lightningAtEnd,
                              boolean unequipVictim, int dropEveryTicks, int levitationAmplifier,
                              Optional<MovementPath> movement, List<ScriptStage> stages) {

    public AnimationScript {
        stages = List.copyOf(stages);
    }

    public int effectiveLevitation() {
        return levitationAmplifier < 0 ? 1 : levitationAmplifier;
    }
}
