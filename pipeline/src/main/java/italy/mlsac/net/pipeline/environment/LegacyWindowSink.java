// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface LegacyWindowSink {
    void accept(UUID player, byte[] window);

    default void encodeAndAccept(UUID player, WindowEncoding encoding) {
        accept(player, encoding.encode());
    }
}
