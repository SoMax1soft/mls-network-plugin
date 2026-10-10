// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpHeader;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.wire.PackedBody;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.testkit.ScriptedTransport;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class TransitEncodingTest {

    private static final UUID PLAYER = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
    private static final WindowSubmission ULTRA = new WindowSubmission(PLAYER, "Aura_00", ultraWindow());
    private static final String OFFER = "{\"sessionId\":\"s-5\",\"success\":true,\"wire5\":{\"seed\":\"42\"}}";

    private final BackendBodies bodies = new BackendBodies();

    @Test
    void theControlPlaneGoesOutAsPlainJson() {
        SessionHarness h = connected(SessionHarness.init());
        h.scheduler.advance(100);
        h.session.sendAlertNotice(new AlertReport(PLAYER, "Aura_00", "ultra", 0.93, 0.5));
        h.session.sendPunishNotice(new PunishReport(PLAYER, "Aura_00", 0.93, 0.5, 3, "kick", "kick Aura_00"));
        h.session.todayStats().join();
        h.session.probeLatency().join();
        for (String path : List.of("/init", "/heartbeat", "/online", "/events/poll", "/api/v1/events",
                "/stats/today")) {
            List<ScriptedTransport.Recorded> calls = h.transport.callsTo(path);
            assertFalse(calls.isEmpty(), path);
            for (ScriptedTransport.Recorded call : calls) {
                assertTrue(encodings(call.call()).isEmpty(), path + " was encoded");
                assertTrue(call.call().body().length < PackedBody.SMALLEST_WORTH_PACKING, path);
                assertTrue(call.body().startsWith("{"), path);
            }
        }
    }

    @Test
    void versionFourPredictBodiesArePackedAndRoundTripToThePlainJson() throws Exception {
        SessionHarness h = connected(SessionHarness.init());
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        assertEquals(new PredictOutcome.Completed(1), h.session.predict(ULTRA, verdict -> { }).join());
        HttpCall call = lastPredict(h);
        assertEquals(List.of(PackedBody.GZIP), encodings(call));
        assertArrayEquals(bodies.predict(SessionHarness.IDENTITY, Optional.of("s-1"), ULTRA), gunzip(call.body()));
    }

    @Test
    void theApiKeyAndVersionHeadersStayInFrontOfTheEncoding() {
        SessionHarness h = connected(SessionHarness.init());
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        h.session.predict(ULTRA, verdict -> { }).join();
        List<String> names = lastPredict(h).headers().stream().map(HttpHeader::name).toList();
        assertEquals(List.of("X-API-Key", "X-MLSAC-Plugin-Version", "X-MLSAC-Plugin-Generation", "Content-Encoding"),
                names);
    }

    @Test
    void theCallerOnlyClaimsASlotAndTheEncoderBuildsAndSends() throws Exception {
        ArrayDeque<Runnable> encoder = new ArrayDeque<>();
        SessionHarness h = connected(SessionHarness.init(), encoder::add);
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        CompletableFuture<PredictOutcome> outcome = h.session.predict(ULTRA, verdict -> { });
        assertTrue(h.transport.callsTo("/predict-stream").isEmpty());
        assertFalse(outcome.isDone());
        assertEquals(1, encoder.size());
        encoder.poll().run();
        assertEquals(new PredictOutcome.Completed(1), outcome.join());
        assertArrayEquals(bodies.predict(SessionHarness.IDENTITY, Optional.of("s-1"), ULTRA),
                gunzip(lastPredict(h).body()));
    }

    @Test
    void aStoppedEncoderGivesTheSlotBack() {
        SessionHarness h = connected(SessionHarness.init(), task -> {
            throw new RejectedExecutionException("stopped");
        });
        for (int i = 0; i <= PredictChannel.MAX_IN_FLIGHT; i++) {
            assertEquals(new PredictOutcome.Failed("Canceled"), h.session.predict(ULTRA, verdict -> { }).join());
        }
    }

    private static List<HttpHeader> encodings(HttpCall call) {
        return call.headers().stream().filter(header -> header.name().equals(PackedBody.GZIP.name())).toList();
    }

    private static SessionHarness connected(String initReply) {
        return connected(initReply, Runnable::run);
    }

    private static SessionHarness connected(String initReply, Executor encoder) {
        SessionHarness h = new SessionHarness(false, encoder);
        h.transport.reply("/api/v1/init", 200, initReply);
        h.session.start();
        h.scheduler.runPending();
        assertEquals(ApiState.CONNECTED, h.session.status().state());
        return h;
    }

    private static HttpCall lastPredict(SessionHarness h) {
        List<ScriptedTransport.Recorded> calls = h.transport.callsTo("/predict-stream");
        return calls.get(calls.size() - 1).call();
    }

    private static byte[] gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return in.readAllBytes();
        }
    }

    private static byte[] ultraWindow() {
        ByteBuffer buffer = ByteBuffer.allocate(20 + 4 * 60 * 64);
        buffer.put("MLSU".getBytes(StandardCharsets.US_ASCII)).putInt(4).putInt(60).putInt(50).putInt(14);
        for (int index = 0; index < 60 * 64; index++) {
            buffer.putFloat(index % 64 < 50 ? (index % 97) * 0.37f : index % 2);
        }
        return buffer.array();
    }
}
