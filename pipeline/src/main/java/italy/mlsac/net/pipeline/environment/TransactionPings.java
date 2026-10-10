// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface TransactionPings {
    void send(UUID viewer, int transactionId);
}
