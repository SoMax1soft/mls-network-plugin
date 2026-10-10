// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.module;

import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.text.MessageDefaults;
import java.util.List;

public interface PluginModule {

    String name();

    void start();

    void stop();

    default List<Subcommand> subcommands() {
        return List.of();
    }

    default MessageDefaults messageDefaults() {
        return MessageDefaults.none();
    }
}
