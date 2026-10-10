// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface SuspectBodies {

    Optional<SuspectBody> bodyOf(UUID suspectId);
}
