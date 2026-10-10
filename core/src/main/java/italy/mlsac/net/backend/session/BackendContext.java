// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpHeader;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.http.HttpTransport;
import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.BackendHeaders;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.wire.PackedBody;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.ServerEnvironment;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record BackendContext(BackendAddress address, RequestIdentity identity, BackendHeaders headers,
                             BackendBodies bodies, HttpTransport transport, TaskScheduler scheduler, Clock clock,
                             RandomSource random, BackendConsole console, IntSupplier connectedPlayers,
                             ReplyInspector replies, String accessKey, ServerEnvironment environment,
                             Supplier<ClientDistribution> clients, Executor encoder) {

    public static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    public CompletableFuture<HttpReply> post(Endpoint endpoint, String path, boolean withKey, byte[] body) {
        return post(endpoint, path, withKey, body, List.of());
    }

    public CompletableFuture<HttpReply> post(Endpoint endpoint, String path, boolean withKey, byte[] body,
                                             List<HttpHeader> extraHeaders) {
        PackedBody packed = PackedBody.of(body, extraHeaders);
        List<HttpHeader> sent = new ArrayList<>(withKey ? headers.withKey() : headers.withoutKey());
        sent.addAll(packed.headers());
        HttpCall call = HttpCall.postJson(address.uri(endpoint, path), sent, packed.bytes(), READ_TIMEOUT);
        return transport.send(call);
    }
}
