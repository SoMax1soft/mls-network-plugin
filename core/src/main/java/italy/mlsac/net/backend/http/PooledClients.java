// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public final class PooledClients implements ClientProvision {

    private static final long IDLE_RECYCLE_NANOS = Duration.ofSeconds(50).toNanos();

    private final Duration connectTimeout;
    private final Executor executor;
    private final AtomicInteger active = new AtomicInteger();
    private HttpClient client;
    private long lastUse;

    public PooledClients(Duration connectTimeout, Executor executor) {
        this.connectTimeout = connectTimeout;
        this.executor = executor;
    }

    @Override
    public synchronized HttpClient acquire() {
        long now = System.nanoTime();
        if (client == null || (active.get() == 0 && now - lastUse >= IDLE_RECYCLE_NANOS)) {
            client = JdkClients.build(connectTimeout, executor);
        }
        active.incrementAndGet();
        lastUse = now;
        return client;
    }

    @Override
    public synchronized void release() {
        active.decrementAndGet();
        lastUse = System.nanoTime();
    }
}
