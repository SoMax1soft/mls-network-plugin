// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.penalty.AnimationStateChanged;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimatingPlayers {

    private final Set<UUID> playing = ConcurrentHashMap.newKeySet();

    public void changed(AnimationStateChanged change) {
        if (change.playing()) {
            playing.add(change.player());
        } else {
            playing.remove(change.player());
        }
    }

    public boolean animating(UUID player) {
        return playing.contains(player);
    }

    public void clear() {
        playing.clear();
    }
}
