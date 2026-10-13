// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.restore.RestoreNotes;
import java.util.function.Supplier;

public final class RestoreLogbook implements RestoreNotes {

    private final PluginLog log;

    public RestoreLogbook(PluginLog log) {
        this.log = log;
    }

    @Override
    public void note(Supplier<String> line) {
        log.debug(LogTopic.DATA, line);
    }

    @Override
    public void trouble(String line) {
        log.warn(LogTopic.DATA, line);
    }
}
