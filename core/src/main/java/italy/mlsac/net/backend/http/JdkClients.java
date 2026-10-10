// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

final class JdkClients {

    private JdkClients() {
    }

    static HttpClient build(Duration connectTimeout, Executor executor) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(executor)
                .build();
    }
}
