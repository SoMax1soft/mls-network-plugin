// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLException;

public final class JdkHttpTransport implements HttpTransport {

    private final ClientProvision clients;
    private final Set<CompletableFuture<HttpReply>> unanswered = ConcurrentHashMap.newKeySet();

    public JdkHttpTransport(ClientProvision clients) {
        this.clients = Objects.requireNonNull(clients, "clients");
    }

    @Override
    public CompletableFuture<HttpReply> send(HttpCall call) {
        HttpClient client = clients.acquire();
        long started = System.nanoTime();
        CompletableFuture<HttpResponse<InputStream>> exchange;
        try {
            exchange = client.sendAsync(toRequest(call), HttpResponse.BodyHandlers.ofInputStream());
        } catch (RuntimeException rejected) {
            return CompletableFuture.failedFuture(new TransportFailure(FailureKind.OTHER, rejected.toString(), rejected));
        }
        CompletableFuture<HttpReply> reply = new CompletableFuture<>();
        unanswered.add(reply);
        exchange.whenComplete((response, error) -> {
            clients.release();
            if (error != null) {
                reply.completeExceptionally(classify(error));
            } else {
                long headerMillis = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
                completeWithBody(reply, response, call.maxResponseBytes(), headerMillis);
            }
        });
        reply.whenComplete((r, e) -> {
            unanswered.remove(reply);
            if (reply.isCancelled() || e != null) {
                exchange.cancel(true);
            }
        });
        return reply;
    }

    @Override
    public void cancelInFlight() {
        for (CompletableFuture<HttpReply> reply : unanswered) {
            reply.completeExceptionally(new TransportFailure(FailureKind.CANCELLED, "Canceled", null));
        }
        unanswered.clear();
    }

    private static void completeWithBody(CompletableFuture<HttpReply> reply, HttpResponse<InputStream> response,
                                         int maxBytes, long headerMillis) {
        try (InputStream in = response.body()) {
            byte[] body = in.readNBytes(maxBytes);
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            reply.complete(new HttpReply(response.statusCode(), contentType, body, headerMillis));
        } catch (IOException | RuntimeException failure) {
            reply.completeExceptionally(classify(failure));
        }
    }

    private static HttpRequest toRequest(HttpCall call) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(call.uri()).timeout(call.readTimeout());
        for (HttpHeader header : call.headers()) {
            builder.header(header.name(), header.value());
        }
        call.contentType().ifPresent(type -> builder.header("Content-Type", type));
        if (call.method() == HttpMethod.POST) {
            builder.POST(HttpRequest.BodyPublishers.ofByteArray(call.body()));
        } else {
            builder.GET();
        }
        return builder.build();
    }

    static TransportFailure classify(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof TransportFailure failure) {
            return failure;
        }
        FailureKind kind;
        if (cause instanceof CancellationException) {
            kind = FailureKind.CANCELLED;
        } else if (cause instanceof HttpConnectTimeoutException || cause instanceof HttpTimeoutException) {
            kind = FailureKind.TIMEOUT;
        } else if (cause instanceof ConnectException || cause instanceof UnknownHostException
                || cause instanceof UnresolvedAddressException || cause instanceof SSLException) {
            kind = FailureKind.CONNECT;
        } else if (cause instanceof InterruptedIOException || cause instanceof IOException) {
            kind = FailureKind.IO;
        } else {
            kind = FailureKind.OTHER;
        }
        return new TransportFailure(kind, cause.getClass().getSimpleName(), cause);
    }
}
