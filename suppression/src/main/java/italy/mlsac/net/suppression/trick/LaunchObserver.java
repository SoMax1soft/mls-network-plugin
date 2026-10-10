// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import java.util.UUID;

@FunctionalInterface
public interface LaunchObserver {

    void launched(UUID suspectId);
}
