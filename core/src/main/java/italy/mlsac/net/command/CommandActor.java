// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import java.util.Optional;
import java.util.UUID;

public interface CommandActor {

    String name();

    Optional<UUID> actorUuid();

    boolean hasPermission(String node);

    boolean op();

    void send(String coloredText);

    default boolean isPlayer() {
        return actorUuid().isPresent();
    }
}
