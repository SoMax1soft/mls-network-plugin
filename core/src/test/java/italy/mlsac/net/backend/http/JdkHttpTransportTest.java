// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JdkHttpTransportTest {

    @Test
    void sendsHeadersBodyAndContentTypeAndReadsTheReply() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> seen = new AtomicReference<>();
        server.createContext("/api/v1/heartbeat", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            seen.set(exchange.getRequestMethod() + "|" + exchange.getRequestHeaders().getFirst("X-API-Key") + "|"
                    + exchange.getRequestHeaders().getFirst("X-MLSAC-Plugin-Generation") + "|"
                    + exchange.getRequestHeaders().getFirst("Content-Type") + "|" + body);
            byte[] reply = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, reply.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(reply);
            }
        });
        server.start();
        try (WorkerPool pool = new WorkerPool("test", 2)) {
            JdkHttpTransport transport = new JdkHttpTransport(new PooledClients(Duration.ofSeconds(2), pool.executor()));
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/heartbeat");
            HttpReply reply = transport.send(HttpCall.postJson(uri, List.of(new HttpHeader("X-API-Key", "k"),
                    new HttpHeader("X-MLSAC-Plugin-Generation", "2")), "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                    Duration.ofSeconds(5))).join();
            assertEquals(200, reply.status());
            assertEquals("{\"success\":true}", reply.text());
            assertEquals("POST|k|2|application/json; charset=utf-8|{\"a\":1}", seen.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refusedConnectionsAreUnreachableFailures() {
        try (WorkerPool pool = new WorkerPool("test", 2)) {
            JdkHttpTransport transport = new JdkHttpTransport(new FreshClients(Duration.ofSeconds(2), pool.executor()));
            URI closed = URI.create("http://127.0.0.1:9/api/v1/init");
            CompletionException failure = assertThrows(CompletionException.class,
                    () -> transport.send(HttpCall.postJson(closed, List.of(), new byte[0], Duration.ofSeconds(2))).join());
            TransportFailure transportFailure = TransportFailure.of(failure);
            assertTrue(transportFailure.kind().unreachable(), transportFailure.kind().name());
            assertFalse(pool.isShutdown());
        }
    }
}
