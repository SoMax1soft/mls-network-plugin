// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;

final class ShutdownSteps {

    void run(PluginLog log, String step, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            log.error(LogTopic.GENERAL, "Disable step '" + step + "' failed: " + failure, failure);
        }
    }
}
