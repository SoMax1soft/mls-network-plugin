// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ComponentFault {

    private final PluginLog log;
    private final AtomicBoolean reported = new AtomicBoolean();

    public ComponentFault(PluginLog log) {
        this.log = log;
    }

    public void report(Throwable failure) {
        if (reported.compareAndSet(false, true)) {
            log.warn(LogTopic.SUPPRESSION, "damage components cannot be scaled here, base damage is corrected instead",
                    failure);
        }
    }
}
