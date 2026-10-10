// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.UUID;

public interface AnimationStage {

    boolean knows(String animation);

    void play(UUID player, String animation, String command);
}
