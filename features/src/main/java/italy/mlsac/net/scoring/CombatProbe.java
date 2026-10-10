// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.UUID;

public interface CombatProbe {

    CombatProbe NEVER = player -> false;

    boolean inLegacyCombat(UUID player);
}
