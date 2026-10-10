// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.UUID;

@FunctionalInterface
public interface LagExemptions {

    LagExemptions NONE = player -> false;

    boolean exempt(UUID player);
}
