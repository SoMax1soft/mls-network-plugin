// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MlsacModelSelection {

    ModelSelection set(UUID player, ModelChoice choice);

    ModelSelection get(UUID player);

    void clear(UUID player);

    Set<ModelChoice> available();

    Optional<FallbackReason> unavailableBecause(ModelChoice choice);
}
