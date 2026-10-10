// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

public final class FreshClients implements ClientProvision {

    private final Duration connectTimeout;
    private final Executor executor;

    public FreshClients(Duration connectTimeout, Executor executor) {
        this.connectTimeout = connectTimeout;
        this.executor = executor;
    }

    @Override
    public HttpClient acquire() {
        return JdkClients.build(connectTimeout, executor);
    }

    @Override
    public void release() {
    }
}
