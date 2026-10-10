// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuppressionBodyTest {

    private static final UUID PLAYER = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000016fb1");
    private static final RequestIdentity IDENTITY = new RequestIdentity("2.0.dev", "lobby", "net", "127.0.0.1", 25565,
            false, Optional.empty());

    private static JsonObject body(Optional<String> session, SuppressionReport report) throws Exception {
        return JsonParser.parseObject(new String(new BackendBodies().suppression(IDENTITY, session, report),
                StandardCharsets.UTF_8));
    }

    @Test
    void aSuppressionIsAnEventOfItsOwnType() throws Exception {
        JsonObject body = body(Optional.of("s-1"), new SuppressionReport(PLAYER, "Steve", 0.97, 62.5,
                SuppressionReport.Trigger.BUFFER, 60, List.of("lag heavy [delay,skip] 6s", "-30% dmg 5s")));
        assertEquals("suppress", body.string("type").orElseThrow());
        assertEquals("s-1", body.string("sessionId").orElseThrow());
        assertEquals(PLAYER.toString(), body.string("playerUuid").orElseThrow());
        assertEquals("Steve", body.string("playerName").orElseThrow());
        assertEquals("buffer", body.string("trigger").orElseThrow());
        assertEquals(60.0, body.number("level").orElseThrow().doubleValue());
        assertEquals(62.5, body.number("buffer").orElseThrow().doubleValue());
        assertEquals(List.of("lag heavy [delay,skip] 6s", "-30% dmg 5s"), body.get("actions").orElseThrow().toPlain());
        assertFalse(body.has("model"));
    }

    @Test
    void aStageCarriesItsPosition() throws Exception {
        JsonObject body = body(Optional.empty(), new SuppressionReport(PLAYER, "Steve", 0.95, 41.0,
                SuppressionReport.Trigger.STAGE, 2, List.of("-20% dmg 30s")));
        assertEquals("stage", body.string("trigger").orElseThrow());
        assertEquals(2.0, body.number("level").orElseThrow().doubleValue());
        assertFalse(body.has("sessionId"));
    }
}
