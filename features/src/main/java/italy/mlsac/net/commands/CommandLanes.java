// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import italy.mlsac.net.command.CommandActor;
import java.util.List;

public interface CommandLanes {

    void inBackground(Runnable task);

    void reply(CommandActor actor, List<String> lines);
}
