// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.profile;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface ProfileFacts {

    OptionalInt sensitivityPercent(UUID player);

    Optional<String> clientVersion(UUID player);

    int highProbabilityCount(UUID player);
}
