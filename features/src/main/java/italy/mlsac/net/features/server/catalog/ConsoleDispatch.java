// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.features.live.ConsoleCommands;
import org.bukkit.Bukkit;

public final class ConsoleDispatch implements ConsoleCommands {

    private final PluginLog log;

    public ConsoleDispatch(PluginLog log) {
        this.log = log;
    }

    @Override
    public void dispatch(String command) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (RuntimeException failure) {
            log.error(LogTopic.PENALTY, "Console command failed: " + command, failure);
        }
    }
}
