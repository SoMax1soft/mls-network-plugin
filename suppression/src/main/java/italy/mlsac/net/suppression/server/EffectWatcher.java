// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import java.util.UUID;

@FunctionalInterface
public interface EffectWatcher {

    void effectsChanged(UUID suspectId, EffectSnapshot snapshot);
}
