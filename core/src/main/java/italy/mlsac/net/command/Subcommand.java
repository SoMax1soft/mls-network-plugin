// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import java.util.List;

public interface Subcommand {

    SubcommandName name();

    void execute(CommandActor actor, List<String> args);

    default List<String> complete(CommandActor actor, List<String> args) {
        return List.of();
    }
}
