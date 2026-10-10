// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface LagSubjects {

    Optional<LagSubject> subjectOf(UUID suspectId);
}
