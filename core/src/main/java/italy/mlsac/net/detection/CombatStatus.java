// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public interface CombatStatus {

    CombatStatus NONE = player -> false;

    boolean inLegacyCombat(UUID player);
}
