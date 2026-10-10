// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OnlinePlayerCount {

    private final Set<UUID> online = ConcurrentHashMap.newKeySet();

    public void seed(Collection<UUID> players) {
        online.addAll(players);
    }

    public void joined(UUID player) {
        online.add(player);
    }

    public void left(UUID player) {
        online.remove(player);
    }

    public int count() {
        return online.size();
    }
}
