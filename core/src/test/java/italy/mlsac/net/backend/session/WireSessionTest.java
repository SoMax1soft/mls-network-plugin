// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.testkit.ScriptedTransport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class WireSessionTest {

    private static final WindowSubmission ULTRA = new WindowSubmission(UUID.randomUUID(), "Aura_00", ultraWindow());
    private static final String OFFER = "{\"sessionId\":\"s-5\",\"success\":true,\"wire5\":{\"seed\":\"42\"}}";
    private static final String UNKNOWN = "{\"success\":false,\"error\":{\"code\":\"SESSION_UNKNOWN\"},\"reinit\":true}";

    @Test
    void withoutAnOfferTheBodyStaysVersionFour() throws Exception {
        SessionHarness h = connected(SessionHarness.init());
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        h.session.predict(ULTRA, verdict -> { }).join();
        HttpCall call = lastPredict(h);
        JsonObject body = JsonParser.parseObject(gunzip(call.body()));
        assertTrue(body.string("playerData").isPresent());
        assertTrue(body.get("wire_version").isEmpty());
    }

    @Test
    void unknownSessionDropsTheWindowAndOpensANewSessionOnce() throws Exception {
        SessionHarness h = connected(OFFER);
        h.transport.reply("/predict-stream", 409, UNKNOWN);
        assertInstanceOf(PredictOutcome.Failed.class, h.session.predict(ULTRA, verdict -> { }).join());
        assertEquals(1, h.transport.callsTo("/predict-stream").size());
        assertInstanceOf(PredictOutcome.Refused.class, h.session.predict(ULTRA, verdict -> { }).join());
        assertEquals(1, h.transport.callsTo("/predict-stream").size());
        h.transport.reply("/api/v1/init", 200, "{\"sessionId\":\"s-6\",\"success\":true,\"wire5\":{\"seed\":\"43\"}}");
        h.scheduler.advance(20 * 60);
        assertEquals(2, h.transport.callsTo("/init").size());
        assertEquals(ApiState.CONNECTED, h.session.status().state());
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        assertEquals(new PredictOutcome.Completed(1), h.session.predict(ULTRA, verdict -> { }).join());
        JsonObject body = JsonParser.parseObject(gunzip(lastPredict(h).body()));
        assertEquals("s-6", body.string("sessionId").orElseThrow());
    }

    @Test
    void otherConflictsKeepTheSession() {
        SessionHarness h = connected(OFFER);
        h.transport.reply("/predict-stream", 409, "{\"error\":{\"code\":\"ULTRA_DISABLED\"}}");
        assertInstanceOf(PredictOutcome.Failed.class, h.session.predict(ULTRA, verdict -> { }).join());
        assertEquals(ApiState.CONNECTED, h.session.status().state());
    }

    private static SessionHarness connected(String initReply) {
        SessionHarness h = new SessionHarness(false);
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

    private static String gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
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
