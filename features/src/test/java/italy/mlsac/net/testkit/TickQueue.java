// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.scoring.PlayerThread;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TickQueue implements PlayerThread {

    public final Set<UUID> online = new HashSet<>();
    private final List<Runnable> pending = new ArrayList<>();

    @Override
    public boolean online(UUID player) {
        return online.contains(player);
    }

    @Override
    public boolean runNextTick(UUID player, Runnable task) {
        if (!online.contains(player)) {
            return false;
        }
        pending.add(task);
        return true;
    }

    public void tick() {
        List<Runnable> due = new ArrayList<>(pending);
        pending.clear();
        due.forEach(Runnable::run);
    }

    public int pendingCount() {
        return pending.size();
    }
}
