// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import java.util.UUID;

@FunctionalInterface
public interface LagLevelSource {

    LagLevels lagLevels(UUID suspectId);
}
