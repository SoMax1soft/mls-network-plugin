// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.util.concurrent.CompletableFuture;

public interface HttpTransport {

    CompletableFuture<HttpReply> send(HttpCall call);

    void cancelInFlight();
}
