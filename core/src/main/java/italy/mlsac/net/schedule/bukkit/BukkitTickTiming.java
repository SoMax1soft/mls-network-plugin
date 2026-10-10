// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule.bukkit;

public final class BukkitTickTiming {

    public long delayTicks(long requested) {
        return Math.max(0L, requested);
    }

    public long everyTicks(long requested) {
        return Math.max(1L, requested);
    }
}
