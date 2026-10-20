// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import java.util.UUID;

public interface PlayerModelRoutes {

    PlayerModelRoutes SERVER_DEFAULT = new PlayerModelRoutes() {
        @Override
        public ModelRoute routeOf(UUID player) {
            return ModelRoute.SERVER_DEFAULT;
        }

        @Override
        public boolean anyPlayerOn(ModelRoute route) {
            return false;
        }
    };

    ModelRoute routeOf(UUID player);

    boolean anyPlayerOn(ModelRoute route);
}
