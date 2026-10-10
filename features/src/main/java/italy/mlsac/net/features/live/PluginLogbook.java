// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.logbook.Logbook;

public final class PluginLogbook implements Logbook {

    private final PluginLog log;

    public PluginLogbook(PluginLog log) {
        this.log = log;
    }

    @Override
    public void info(String line) {
        log.info(LogTopic.GENERAL, line);
    }

    @Override
    public void warn(String line) {
        log.warn(LogTopic.GENERAL, line);
    }

    @Override
    public void severe(String line, Throwable cause) {
        log.error(LogTopic.GENERAL, line, cause);
    }

    @Override
    public void debug(String line) {
        log.debug(LogTopic.GENERAL, () -> line);
    }
}
