// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Objects;
import java.util.UUID;

public record PunishedPlayer(String name, UUID id) {

    public PunishedPlayer {
        Objects.requireNonNull(id, "id");
        name = name == null ? "" : name;
    }
}
