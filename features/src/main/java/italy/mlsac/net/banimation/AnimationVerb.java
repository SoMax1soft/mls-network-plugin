// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.command.CommandActor;
import java.util.List;

interface AnimationVerb {

    void run(CommandActor invoker, List<String> rest);

    default List<String> suggest(CommandActor invoker, List<String> rest) {
        return List.of();
    }
}
