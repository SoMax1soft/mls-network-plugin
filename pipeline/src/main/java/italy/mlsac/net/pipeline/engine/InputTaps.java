// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import java.util.UUID;

public interface InputTaps {

    InputTaps NONE = new InputTaps() {
        @Override
        public InputTap tapOrNull(UUID player) {
            return null;
        }

        @Override
        public boolean anyActive() {
            return false;
        }

        @Override
        public void closeAll() {
        }
    };

    InputTap tapOrNull(UUID player);

    boolean anyActive();

    void closeAll();

    default void playerLeft(UUID player) {
    }
}
