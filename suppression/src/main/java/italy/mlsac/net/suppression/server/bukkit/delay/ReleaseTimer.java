// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.delay;

public interface ReleaseTimer {

    void after(long delayMillis, Runnable task);

    void close();
}
