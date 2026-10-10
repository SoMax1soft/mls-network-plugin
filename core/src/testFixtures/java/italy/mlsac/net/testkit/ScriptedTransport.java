// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.backend.http.FailureKind;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.http.HttpTransport;
import italy.mlsac.net.backend.http.TransportFailure;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ScriptedTransport implements HttpTransport {

    public record Recorded(HttpCall call, String body) {

        public String path() {
            return call.uri().getPath();
        }

        public String url() {
            return call.uri().toString();
        }
    }

    private final Map<String, Deque<Function<HttpCall, CompletableFuture<HttpReply>>>> scripts = new LinkedHashMap<>();
    private final List<Recorded> calls = new ArrayList<>();
    private final List<CompletableFuture<HttpReply>> held = new ArrayList<>();
    private Function<HttpCall, CompletableFuture<HttpReply>> fallback =
            call -> CompletableFuture.completedFuture(HttpReply.of(200, "{\"success\":true}"));

    public synchronized ScriptedTransport reply(String urlSuffix, int status, String body) {
        return script(urlSuffix, call -> CompletableFuture.completedFuture(HttpReply.of(status, body)));
    }

    public synchronized ScriptedTransport fail(String urlSuffix, FailureKind kind) {
        return script(urlSuffix, call -> CompletableFuture.failedFuture(new TransportFailure(kind, kind.name(), null)));
    }

    public synchronized ScriptedTransport hold(String urlSuffix) {
        return script(urlSuffix, call -> {
            CompletableFuture<HttpReply> pending = new CompletableFuture<>();
            held.add(pending);
            return pending;
        });
    }

    public synchronized ScriptedTransport otherwise(int status, String body) {
        fallback = call -> CompletableFuture.completedFuture(HttpReply.of(status, body));
        return this;
    }

    public synchronized ScriptedTransport otherwiseFail(FailureKind kind) {
        fallback = call -> CompletableFuture.failedFuture(new TransportFailure(kind, kind.name(), null));
        return this;
    }

    @Override
    public CompletableFuture<HttpReply> send(HttpCall call) {
        Function<HttpCall, CompletableFuture<HttpReply>> responder;
        synchronized (this) {
            calls.add(new Recorded(call, new String(call.body(), StandardCharsets.UTF_8)));
            responder = next(call.uri().toString(), call.uri().getPath());
        }
        return responder.apply(call);
    }

    @Override
    public synchronized void cancelInFlight() {
        held.forEach(future -> future.completeExceptionally(new TransportFailure(FailureKind.CANCELLED, "Canceled", null)));
        held.clear();
    }

    public synchronized List<Recorded> calls() {
        return List.copyOf(calls);
    }

    public synchronized List<Recorded> callsTo(String urlSuffix) {
        return calls.stream().filter(call -> call.url().endsWith(urlSuffix)).toList();
    }

    public synchronized List<CompletableFuture<HttpReply>> held() {
        return List.copyOf(held);
    }

    private ScriptedTransport script(String urlSuffix, Function<HttpCall, CompletableFuture<HttpReply>> responder) {
        scripts.computeIfAbsent(urlSuffix, key -> new ArrayDeque<>()).add(responder);
        return this;
    }

    private Function<HttpCall, CompletableFuture<HttpReply>> next(String url, String path) {
        for (Map.Entry<String, Deque<Function<HttpCall, CompletableFuture<HttpReply>>>> entry : scripts.entrySet()) {
            boolean matches = url.endsWith(entry.getKey()) || path.endsWith(entry.getKey());
            if (matches && !entry.getValue().isEmpty()) {
                return entry.getValue().poll();
            }
        }
        return fallback;
    }
}
