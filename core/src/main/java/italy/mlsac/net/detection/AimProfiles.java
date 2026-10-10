// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.OptionalInt;
import java.util.UUID;

public interface AimProfiles {

    AimProfiles NONE = player -> OptionalInt.empty();

    OptionalInt sensitivityPercent(UUID player);
}
