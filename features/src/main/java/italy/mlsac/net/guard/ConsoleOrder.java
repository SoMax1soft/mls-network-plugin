// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Objects;
import java.util.Optional;

public record ConsoleOrder(String command, CommandOrigin origin, Optional<PunishedPlayer> target) {

    public ConsoleOrder {
        command = command == null ? "" : command;
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(target, "target");
    }

    public static ConsoleOrder against(String command, CommandOrigin origin, PunishedPlayer target) {
        return new ConsoleOrder(command, origin, Optional.of(target));
    }

    public static ConsoleOrder untargeted(String command, CommandOrigin origin) {
        return new ConsoleOrder(command, origin, Optional.empty());
    }
}
