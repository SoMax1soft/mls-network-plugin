// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.platform.ServerSoftware;
import italy.mlsac.net.platform.VersionResolution;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.HostMachine;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.telemetry.ServerStack;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class TelemetryBodiesTest {

    private static final String SESSION = "1790000000000-k2j3h4g5f6d7s";
    private static final int ULTRA_WIRE = 6;

    private final BackendBodies bodies = new BackendBodies();
    private final RequestIdentity lobby = new RequestIdentity("2.0.dev", "lobby-1", "net", "127.0.0.1", 25565, true,
            Optional.of("pvp"));

    @Test
    void initCarriesTheStaticEnvironment() {
        String body = text(bodies.init(lobby, "<API-KEY>", 3, ULTRA_WIRE, environment()));
        assertTrue(body.startsWith("{\"pluginVersion\":\"2.0.dev\""), body);
        assertTrue(body.endsWith(",\"ultraWire\":6,\"environment\":{\"serverCore\":\"Paper\","
                + "\"serverVersion\":\"git-Paper-497 (MC: 1.20.4)\",\"apiVersion\":\"1.20.4-R0.1-SNAPSHOT\","
                + "\"serverSoftware\":\"Paper\",\"gameVersion\":\"1.20.4\",\"compatibility\":\"LEGACY_MODERN\","
                + "\"folia\":false,\"osName\":\"Linux\",\"osVersion\":\"5.15.0-91-generic\",\"osArch\":\"amd64\","
                + "\"javaVersion\":\"17.0.9\",\"javaVendor\":\"Eclipse Adoptium\",\"cpuCores\":8,"
                + "\"maxHeapMb\":8192}}"), body);
    }

    @Test
    void heartbeatCarriesTheClientVersionDistribution() {
        String body = text(bodies.status(lobby, Optional.of(SESSION), 4, distribution()));
        assertTrue(body.endsWith(",\"onlinePlayers\":4,\"onlineCount\":4,\"clientVersions\":{"
                + "\"protocols\":{\"765\":3,\"47\":1},\"brands\":{\"vanilla\":3,\"lunarclient\":1}}}"), body);
    }

    @Test
    void initWithoutTelemetryIsUnchanged() {
        byte[] withoutArgument = bodies.init(lobby, "<API-KEY>", 3, ULTRA_WIRE);
        byte[] unknown = bodies.init(lobby, "<API-KEY>", 3, ULTRA_WIRE, ServerEnvironment.UNKNOWN);
        assertArrayEquals(withoutArgument, unknown);
        assertFalse(text(unknown).contains("environment"));
    }

    @Test
    void heartbeatWithoutTelemetryIsUnchanged() {
        byte[] withoutArgument = bodies.status(lobby, Optional.of(SESSION), 4);
        byte[] empty = bodies.status(lobby, Optional.of(SESSION), 4, ClientDistribution.EMPTY);
        assertArrayEquals(withoutArgument, empty);
        assertFalse(text(empty).contains("clientVersions"));
    }

    @Test
    void partialEnvironmentOmitsTheFieldsThePlatformCannotSupply() {
        ServerStack stack = new ServerStack(Optional.of("CraftBukkit"), Optional.empty(), Optional.empty(),
                Optional.of("Spigot/Bukkit"), Optional.of("1.8.8"), Optional.of("LEGACY"), Optional.of(false));
        HostMachine host = new HostMachine(Optional.of("Windows 11"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), OptionalInt.empty(), OptionalLong.empty());
        String body = text(bodies.init(lobby, "<API-KEY>", 0, ULTRA_WIRE, new ServerEnvironment(stack, host)));
        assertTrue(body.endsWith(",\"environment\":{\"serverCore\":\"CraftBukkit\","
                + "\"serverSoftware\":\"Spigot/Bukkit\",\"gameVersion\":\"1.8.8\",\"compatibility\":\"LEGACY\","
                + "\"folia\":false,\"osName\":\"Windows 11\"}}"), body);
        assertFalse(body.contains("serverVersion"));
        assertFalse(body.contains("javaVersion"));
        assertFalse(body.contains("cpuCores"));
        assertFalse(body.contains("maxHeapMb"));
    }

    @Test
    void halfEmptyDistributionSendsOnlyTheHalfThatIsKnown() {
        String withBrandsOnly = text(bodies.status(lobby, Optional.of(SESSION), 1,
                new ClientDistribution(Map.of(), Map.of("vanilla", 1))));
        assertTrue(withBrandsOnly.endsWith(",\"clientVersions\":{\"brands\":{\"vanilla\":1}}}"), withBrandsOnly);
        String withProtocolsOnly = text(bodies.status(lobby, Optional.of(SESSION), 1,
                new ClientDistribution(Map.of("765", 1), Map.of())));
        assertTrue(withProtocolsOnly.endsWith(",\"clientVersions\":{\"protocols\":{\"765\":1}}}"),
                withProtocolsOnly);
    }

    @Test
    void theDistributionStaysSmallOnTheWire() {
        Map<String, Integer> protocols = new LinkedHashMap<>();
        Map<String, Integer> brands = new LinkedHashMap<>();
        for (int index = 0; index < 8; index++) {
            protocols.put(Integer.toString(700 + index), 64);
            brands.put("brand-with-a-very-long-nam" + index, 64);
        }
        byte[] body = bodies.status(lobby, Optional.of(SESSION), 512, new ClientDistribution(protocols, brands));
        byte[] plain = bodies.status(lobby, Optional.of(SESSION), 512);
        assertTrue(body.length - plain.length < 512, "telemetry added " + (body.length - plain.length) + " bytes");
    }

    @Test
    void environmentIsBuiltFromThePlatformProfile() {
        PlatformProfile platform = new PlatformProfile(ServerSoftware.PAPER,
                VersionResolution.recognised("1.20.4-R0.1-SNAPSHOT", GameVersion.of(1, 20, 4)), true, 17);
        ServerStack stack = ServerStack.of(platform, Optional.of("Paper"), Optional.of("git-Paper-497"),
                Optional.of("1.20.4-R0.1-SNAPSHOT"));
        assertEquals(Optional.of("Paper"), stack.software());
        assertEquals(Optional.of("1.20.4"), stack.gameVersion());
        assertEquals(Optional.of("LEGACY_MODERN"), stack.compatibility());
        assertEquals(Optional.of(false), stack.folia());
        assertTrue(new ServerEnvironment(stack, HostMachine.read()).known());
    }

    @Test
    void blankPlatformAnswersAreDroppedNotSentAsEmptyStrings() {
        ServerStack stack = ServerStack.of(null, Optional.of("   "), Optional.empty(), Optional.of(""));
        assertFalse(stack.known());
        assertFalse(new ServerEnvironment(stack, HostMachine.UNKNOWN).known());
        assertArrayEquals(bodies.init(lobby, "<API-KEY>", 0, ULTRA_WIRE),
                bodies.init(lobby, "<API-KEY>", 0, ULTRA_WIRE, new ServerEnvironment(stack, HostMachine.UNKNOWN)));
    }

    private static ServerEnvironment environment() {
        ServerStack stack = new ServerStack(Optional.of("Paper"), Optional.of("git-Paper-497 (MC: 1.20.4)"),
                Optional.of("1.20.4-R0.1-SNAPSHOT"), Optional.of("Paper"), Optional.of("1.20.4"),
                Optional.of("LEGACY_MODERN"), Optional.of(false));
        HostMachine host = new HostMachine(Optional.of("Linux"), Optional.of("5.15.0-91-generic"),
                Optional.of("amd64"), Optional.of("17.0.9"), Optional.of("Eclipse Adoptium"), OptionalInt.of(8),
                OptionalLong.of(8192L));
        return new ServerEnvironment(stack, host);
    }

    private static ClientDistribution distribution() {
        Map<String, Integer> protocols = new LinkedHashMap<>();
        protocols.put("765", 3);
        protocols.put("47", 1);
        Map<String, Integer> brands = new LinkedHashMap<>();
        brands.put("vanilla", 3);
        brands.put("lunarclient", 1);
        return new ClientDistribution(protocols, brands);
    }

    private static String text(byte[] body) {
        return new String(body, StandardCharsets.UTF_8);
    }
}
