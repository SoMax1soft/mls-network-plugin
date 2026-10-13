// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Locale;

public record RootOwner(String command, String owner) {

    public static final String MINECRAFT = "minecraft";
    public static final String SERVER = "server";
    public static final String COMMANDS_FILE = "commands.yml";

    public RootOwner {
        command = command == null ? "" : command.toLowerCase(Locale.ROOT);
        owner = owner == null ? SERVER : owner;
    }

    public String ownerKey() {
        return owner.toLowerCase(Locale.ROOT);
    }
}
