// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import java.util.UUID;

final class JointTaps implements InputTaps {
    private final InputTaps first;
    private final InputTaps second;

    JointTaps(InputTaps first, InputTaps second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public InputTap tapOrNull(UUID player) {
        InputTap one = first.tapOrNull(player);
        InputTap other = second.tapOrNull(player);
        if (one == null) {
            return other;
        }
        return other == null ? one : new JointTap(one, other);
    }

    @Override
    public boolean anyActive() {
        return first.anyActive() || second.anyActive();
    }

    @Override
    public void closeAll() {
        first.closeAll();
        second.closeAll();
    }

    @Override
    public void playerLeft(UUID player) {
        first.playerLeft(player);
        second.playerLeft(player);
    }
}
