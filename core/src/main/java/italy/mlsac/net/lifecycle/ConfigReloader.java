// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle;

import java.util.concurrent.CompletableFuture;

public interface ConfigReloader {

    CompletableFuture<ReloadOutcome> reload(ReloadKind kind);
}
