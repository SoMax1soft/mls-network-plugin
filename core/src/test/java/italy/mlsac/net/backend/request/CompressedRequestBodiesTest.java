// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.wire.PackedBody;
import italy.mlsac.net.backend.wire.PlainWindow;
import italy.mlsac.net.backend.wire.ScrambledWindows;
import italy.mlsac.net.backend.wire.WireNumbers;
import italy.mlsac.net.backend.wire.WireSeed;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.HostMachine;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.telemetry.ServerStack;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class CompressedRequestBodiesTest {

    private static final String KEY = "<API-KEY>";
    private static final Optional<String> SESSION = Optional.of("1790000000000-k2j3h4g5f6d7s");
    private static final UUID PLAYER = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
    private static final int ULTRA_WIRE = 6;
    private static final List<String> CONTROL_PLANE = List.of("init", "init+wire", "init+environment", "status",
            "status+clients", "poll", "alert", "punish", "stats/today");

    private final BackendBodies bodies = new BackendBodies();

    private record Kind(String name, byte[] plain) {
    }

    @Test
    void everyBodyKindRoundTripsThroughGzipToTheExactPlainJson() throws Exception {
        for (Kind kind : kinds(identity(longHostLabel()))) {
            PackedBody packed = PackedBody.of(kind.plain(), List.of());
            if (kind.name().equals("stats/today")) {
                assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), kind.plain());
                assertFalse(packed.packed(), kind.name() + " can never grow past the threshold");
                continue;
            }
            assertTrue(packed.packed(), kind.name() + " stayed plain at " + kind.plain().length + " bytes");
            assertTrue(packed.bytes().length < kind.plain().length,
                    kind.name() + " grew to " + packed.bytes().length + " bytes");
            assertArrayEquals(kind.plain(), gunzip(packed.bytes()), kind.name());
        }
    }

    @Test
    void theControlPlaneOfARealServerStaysPlain() {
        for (Kind kind : kinds(identity("lobby-1"))) {
            if (!CONTROL_PLANE.contains(kind.name())) {
                continue;
            }
            assertTrue(kind.plain().length < PackedBody.SMALLEST_WORTH_PACKING,
                    kind.name() + " is " + kind.plain().length + " bytes");
            PackedBody packed = PackedBody.of(kind.plain(), List.of());
            assertFalse(packed.packed(), kind.name());
            assertSame(kind.plain(), packed.bytes());
        }
    }

    @Test
    void bothPredictWiresArePackedAtTheirRealSizes() throws Exception {
        for (Kind kind : kinds(identity("lobby-1"))) {
            if (CONTROL_PLANE.contains(kind.name())) {
                continue;
            }
            PackedBody packed = PackedBody.of(kind.plain(), List.of());
            assertTrue(packed.packed(), kind.name() + " stayed plain at " + kind.plain().length + " bytes");
            assertArrayEquals(kind.plain(), gunzip(packed.bytes()), kind.name());
        }
    }

    private List<Kind> kinds(RequestIdentity identity) {
        WindowSubmission window = new WindowSubmission(PLAYER, "Aura_00", ultraWindow());
        List<Kind> kinds = new ArrayList<>();
        kinds.add(new Kind("init", bodies.init(identity, KEY, 4)));
        kinds.add(new Kind("init+wire", bodies.init(identity, KEY, 4, ULTRA_WIRE)));
        kinds.add(new Kind("init+environment", bodies.init(identity, KEY, 4, ULTRA_WIRE, environment())));
        kinds.add(new Kind("status", bodies.status(identity, SESSION, 4)));
        kinds.add(new Kind("status+clients", bodies.status(identity, SESSION, 4, clients())));
        kinds.add(new Kind("poll", bodies.poll(identity, SESSION)));
        kinds.add(new Kind("alert", bodies.alert(identity, SESSION,
                new AlertReport(PLAYER, "Aura_00", "ultra", 0.93, 0.5))));
        kinds.add(new Kind("punish", bodies.punish(identity, SESSION,
                new PunishReport(PLAYER, "Aura_00", 0.93, 0.5, 3, "kick", "kick Aura_00 cheating"))));
        kinds.add(new Kind("predict", bodies.predict(identity, SESSION, window)));
        kinds.add(new Kind("predict+scrambled", bodies.predictScrambled(identity, SESSION, window, 5,
                WireNumbers.flat(scrambledRows()))));
        kinds.add(new Kind("stats/today", bodies.emptyObject()));
        return kinds;
    }

    private static double[][] scrambledRows() {
        return new ScrambledWindows(new WireSeed(0x9E3779B97F4A7C15L)).scramble(new PlainWindow(plainRows()));
    }

    private static double[][] plainRows() {
        double[][] rows = new double[PlainWindow.TICKS][PlainWindow.WIDTH];
        for (int tick = 0; tick < PlainWindow.TICKS; tick++) {
            for (int column = 0; column < PlainWindow.WIDTH; column++) {
                rows[tick][column] = column < PlainWindow.FEATURES
                        ? (float) ((tick * 97 + column) % 211) * 0.37f : (tick + column) % 2;
            }
        }
        return rows;
    }

    private static byte[] ultraWindow() {
        ByteBuffer buffer = ByteBuffer.allocate(20 + 4 * PlainWindow.TICKS * PlainWindow.WIDTH);
        buffer.put("MLSU".getBytes(StandardCharsets.US_ASCII)).putInt(4).putInt(PlainWindow.TICKS)
                .putInt(PlainWindow.FEATURES).putInt(PlainWindow.FLAGS);
        double[][] rows = plainRows();
        for (double[] row : rows) {
            for (int column = 0; column < PlainWindow.FEATURES; column++) {
                buffer.putFloat((float) row[column]);
            }
        }
        for (double[] row : rows) {
            for (int flag = 0; flag < PlainWindow.FLAGS; flag++) {
                buffer.putFloat((float) row[PlainWindow.FEATURES + flag]);
            }
        }
        return buffer.array();
    }

    private static RequestIdentity identity(String hostLabel) {
        return new RequestIdentity("2.0.dev", hostLabel, "net", "127.0.0.1", 25565, true, Optional.of("pvp"));
    }

    private static String longHostLabel() {
        StringBuilder label = new StringBuilder("lobby-1");
        while (label.length() < 1200) {
            label.append("-shard-").append(label.length());
        }
        return label.toString();
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

    private static ClientDistribution clients() {
        Map<String, Integer> protocols = new LinkedHashMap<>();
        Map<String, Integer> brands = new LinkedHashMap<>();
        protocols.put("765", 3);
        protocols.put("47", 1);
        brands.put("vanilla", 3);
        brands.put("lunarclient", 1);
        return new ClientDistribution(protocols, brands);
    }

    private static byte[] gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return in.readAllBytes();
        }
    }
}
