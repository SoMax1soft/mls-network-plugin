// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public interface AnimationListener {

    AnimationListener NONE = new AnimationListener() {
        @Override
        public void started(UUID player, BooleanSupplier advance) {
        }

        @Override
        public void ended(UUID player) {
        }
    };

    void started(UUID player, BooleanSupplier advance);

    void ended(UUID player);
}
