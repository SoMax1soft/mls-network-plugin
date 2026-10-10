// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

public record ServerIdentity(String name, String family, boolean interServer, boolean eventsEnabled,
                             double alertEventThreshold) {

    public static final String DEFAULT_NAME = "default";

    public boolean usesDefaultName() {
        return name.isBlank() || DEFAULT_NAME.equals(name);
    }
}
