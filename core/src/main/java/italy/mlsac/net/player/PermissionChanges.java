// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.UUID;
import java.util.function.Consumer;

public interface PermissionChanges {

    PermissionChanges NONE = changed -> () -> { };

    Closing listen(Consumer<UUID> changed);

    interface Closing {
        void close();
    }
}
