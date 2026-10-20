// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.testkit.TestConfigs;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackendBodiesTest {

    private static final String SESSION = "1790000000000-k2j3h4g5f6d7s";
    private final BackendBodies bodies = new BackendBodies();
    private final RequestIdentity lobby = new RequestIdentity("2.0.dev", "lobby-1", "net", "127.0.0.1", 25565, true,
            Optional.of("pvp"));

    @Test
    void initBodyMatchesSpecExample() {
        String expected = "{\"pluginVersion\":\"2.0.dev\",\"pluginGeneration\":2,\"interserverEventsSupported\":true,"
                + "\"serverName\":\"lobby-1\",\"serverFamily\":\"net\",\"family\":\"net\",\"serverIp\":\"127.0.0.1\","
                + "\"serverPort\":25565,\"interServer\":true,\"preset\":\"pvp\",\"apiKey\":\"<API-KEY>\","
                + "\"onlinePlayers\":0,\"onlineCount\":0}";
        assertEquals(expected, text(bodies.init(lobby, "<API-KEY>", 0)));
    }

    @Test
    void heartbeatBodyMatchesSpecExample() {
        String expected = "{\"pluginVersion\":\"2.0.dev\",\"pluginGeneration\":2,\"interserverEventsSupported\":true,"
                + "\"serverName\":\"lobby-1\",\"serverFamily\":\"net\",\"family\":\"net\",\"serverIp\":\"127.0.0.1\","
                + "\"serverPort\":25565,\"interServer\":true,\"preset\":\"pvp\",\"sessionId\":\"" + SESSION + "\","
                + "\"onlinePlayers\":2,\"onlineCount\":2}";
        assertEquals(expected, text(bodies.status(lobby, Optional.of(SESSION), 2)));
    }

    @Test
    void pollBodyHasNoOnlineFields() {
        RequestIdentity noPreset = new RequestIdentity("2.0.dev", "lobby-1", "net", "127.0.0.1", 25565, true,
                Optional.empty());
        String expected = "{\"pluginVersion\":\"2.0.dev\",\"pluginGeneration\":2,\"interserverEventsSupported\":true,"
                + "\"serverName\":\"lobby-1\",\"serverFamily\":\"net\",\"family\":\"net\",\"serverIp\":\"127.0.0.1\","
                + "\"serverPort\":25565,\"interServer\":true,\"sessionId\":\"" + SESSION + "\"}";
        assertEquals(expected, text(bodies.poll(noPreset, Optional.of(SESSION))));
    }

    @Test
    void alertAndPunishEventsMatchGoldenExamples() {
        RequestIdentity golden = new RequestIdentity("2.0.dev", "synthetic-ultra_aura_vs_legit", "golden",
                "127.0.0.1", 25565, true, Optional.of("pvp"));
        UUID player = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
        String base = "{\"pluginVersion\":\"2.0.dev\",\"pluginGeneration\":2,\"interserverEventsSupported\":true,"
                + "\"serverName\":\"synthetic-ultra_aura_vs_legit\",\"serverFamily\":\"golden\",\"family\":\"golden\","
                + "\"serverIp\":\"127.0.0.1\",\"serverPort\":25565,\"interServer\":true,\"preset\":\"pvp\","
                + "\"sessionId\":\"syn-ultra_aura_vs_legit\",";
        String alert = base + "\"type\":\"alert\",\"playerUuid\":\"" + player + "\",\"playerId\":\"" + player + "\","
                + "\"playerName\":\"Aura_00\",\"model\":\"ultra\",\"probability\":0.951,\"buffer\":45.099999999999994,"
                + "\"vl\":0,\"violationLevel\":0,\"action\":\"alert\",\"command\":\"\"}";
        String punish = base + "\"type\":\"punish\",\"playerUuid\":\"" + player + "\",\"playerId\":\"" + player + "\","
                + "\"playerName\":\"Aura_00\",\"model\":\"unknown\",\"probability\":0.951,\"buffer\":25.0,\"vl\":1,"
                + "\"violationLevel\":1,\"action\":\"kick\",\"command\":\"{KICK} kick {PLAYER} MLSAC detection\"}";
        Optional<String> session = Optional.of("syn-ultra_aura_vs_legit");
        assertEquals(alert, text(bodies.alert(golden, session,
                new AlertReport(player, "Aura_00", "ultra", 0.951, 45.099999999999994))));
        assertEquals(punish, text(bodies.punish(golden, session,
                new PunishReport(player, "Aura_00", 0.951, 25.0, 1, "kick", "{KICK} kick {PLAYER} MLSAC detection"))));
    }

    @Test
    void predictBodyEncodesWindowWithPaddedBase64() {
        UUID player = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
        String body = text(bodies.predict(lobby, Optional.of(SESSION),
                new WindowSubmission(player, "Aura_00", new byte[] {'M', 'L', 'S', 'U', 0})));
        assertTrue(body.endsWith(",\"sessionId\":\"" + SESSION + "\",\"playerData\":\"TUxTVQA=\",\"playerUuid\":\""
                + player + "\",\"playerName\":\"Aura_00\"}"));
    }

    @Test
    void identityDefaultsFollowTheSpec() {
        CoreSettings blank = TestConfigs.core("server-identity:\n  name: '   '\n  family: ''\nremote-config:\n  preset: ''\n");
        RequestIdentity identity = RequestIdentity.of("2.0.dev", blank, new ServerAddress("  ", 25565));
        String body = text(bodies.status(identity, Optional.empty(), 0));
        assertTrue(body.contains("\"serverName\":\"default\",\"serverFamily\":\"default\",\"family\":\"default\","
                + "\"serverIp\":\"unknown\""));
        assertFalse(body.contains("\"preset\""));
        CoreSettings sentinel = TestConfigs.core("remote-config:\n  preset: none\n");
        String withSentinel = text(bodies.status(RequestIdentity.of("2.0.dev", sentinel, new ServerAddress("", 1)),
                Optional.empty(), 0));
        assertTrue(withSentinel.contains("\"preset\":\"none\""));
    }

    private static String text(byte[] body) {
        return new String(body, StandardCharsets.UTF_8);
    }
}
