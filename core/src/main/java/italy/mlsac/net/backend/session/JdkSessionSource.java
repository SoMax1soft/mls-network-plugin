// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.http.JdkHttpTransport;
import italy.mlsac.net.backend.http.PooledClients;
import italy.mlsac.net.backend.http.WorkerPool;
import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.BackendHeaders;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.ServerAddress;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.ServerEnvironment;
import java.time.Duration;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record JdkSessionSource(BackendAddress address, String buildVersion, Supplier<ServerAddress> server,
                               TaskScheduler scheduler, Clock clock, RandomSource random, PluginLog log,
                               IntSupplier connectedPlayers, ReplyInspector replies, ServerEnvironment environment,
                               Supplier<ClientDistribution> clients) implements SessionSource {

    private static final Duration DIAL_LIMIT = Duration.ofSeconds(4);
    private static final String POOL_NAME = "backend";
    private static final String ENCODER_NAME = "encode";

    @Override
    public RunningSession open(CoreSettings core) {
        WorkerPool pool = new WorkerPool(POOL_NAME, WorkerPool.backendSize(Runtime.getRuntime().availableProcessors()));
        WorkerPool encoder = new WorkerPool(ENCODER_NAME, 1);
        String key = core.accessKey().value();
        BackendContext context = new BackendContext(address, RequestIdentity.of(buildVersion, core, server.get()),
                new BackendHeaders(buildVersion, key), new BackendBodies(),
                new JdkHttpTransport(new PooledClients(DIAL_LIMIT, pool.executor())), scheduler, clock, random,
                new BackendConsole(log, clock), connectedPlayers, replies, key, environment, clients,
                encoder.executor());
        return new RunningSession(new BackendSession(context), () -> {
            encoder.close();
            pool.close();
        });
    }
}
