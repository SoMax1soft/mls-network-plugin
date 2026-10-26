// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface TransactionPings {
    void send(UUID viewer, int transactionId);

    default Object route(UUID viewer) {
        return null;
    }

    default void send(Object route, UUID viewer, int transactionId) {
        send(viewer, transactionId);
    }

    default void openBurst() {
    }

    default void closeBurst() {
    }
}
