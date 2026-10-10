// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import italy.mlsac.net.suppression.server.bukkit.delay.ReleaseTimer;
import java.util.ArrayList;
import java.util.List;

final class ManualReleaseTimer implements ReleaseTimer {

    private final List<Runnable> pending = new ArrayList<>();

    @Override
    public void after(long delayMillis, Runnable task) {
        pending.add(task);
    }

    @Override
    public void close() {
        pending.clear();
    }

    void runPending() {
        List<Runnable> due = new ArrayList<>(pending);
        pending.clear();
        due.forEach(Runnable::run);
    }

    int pendingCount() {
        return pending.size();
    }
}
