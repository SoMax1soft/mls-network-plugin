// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonValue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ScrambledWireTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity("2.0.dev", "lobby-1", "net", "127.0.0.1",
            25565, true, Optional.of("pvp"));
    private static final Optional<String> SESSION = Optional.of("1790000000000-k2j3h4g5f6d7s");
    private static final UUID PLAYER = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
    private static final List<String> FIELD_ORDER = List.of("pluginVersion", "pluginGeneration",
            "interserverEventsSupported", "serverName", "serverFamily", "family", "serverIp", "serverPort",
            "interServer", "preset", "sessionId", "wire_version", "data", "playerUuid", "playerName");

    private final BackendBodies bodies = new BackendBodies();

    @Test
    void bodyIsGzippedJsonThatDecodesToTheVersionFourRows() throws Exception {
        JsonObject set = WireVectors.sets(WireVectors.load()).get(0);
        JsonObject first = (JsonObject) set.array("windows").orElseThrow().get(0);
        double[][] plain = WireVectors.rows(first.get("plain_rows").orElseThrow());
        double[][] expectedSent = WireVectors.rows(first.get("transmitted_rows").orElseThrow());
        WireSeed seed = WireSeed.parse(set.string("seed").orElseThrow()).orElseThrow();
        byte[] payload = mlsu(plain);
        assertArrayEquals(flatten(plain), flatten(PlainWindow.fromUltraPayload(payload).orElseThrow().rows()));

        PredictRequest request = new ScrambledWire(seed).request(bodies, IDENTITY, SESSION,
                new WindowSubmission(PLAYER, "Aura_00", payload));
        assertEquals(5, request.wireVersion());
        assertEquals("Content-Encoding", request.extraHeaders().get(0).name());
        assertEquals("gzip", request.extraHeaders().get(0).value());
        JsonObject body = JsonParser.parseObject(gunzip(request.body()));
        assertEquals(FIELD_ORDER, new ArrayList<>(body.members().keySet()));
        assertEquals(5.0, body.number("wire_version").orElseThrow().doubleValue());
        List<JsonValue> data = body.array("data").orElseThrow().elements();
        assertEquals(PlainWindow.TICKS * ScrambledWindows.SLOTS, data.size());

        SlotOrder order = SlotOrder.shuffled(seed, ScrambledWindows.SLOTS);
        int unequalCanaries = 0;
        for (int tick = 0; tick < PlainWindow.TICKS; tick++) {
            double[] extended = new double[ScrambledWindows.SLOTS];
            for (int slot = 0; slot < ScrambledWindows.SLOTS; slot++) {
                double sent = WireVectors.number(data.get(tick * ScrambledWindows.SLOTS + slot));
                assertTrue(WireVectors.same(expectedSent[tick][slot], sent), "tick " + tick + " slot " + slot);
                extended[order.sourceOf(slot)] = sent;
            }
            for (int column = 0; column < PlainWindow.WIDTH; column++) {
                assertTrue(WireVectors.same(plain[tick][column], extended[column]), "tick " + tick + " col " + column);
            }
            for (int index = 20; index < DerivedColumns.COUNT; index++) {
                double recomputed = DerivedColumns.phase(extended, index, tick);
                if (!(Math.abs(recomputed - extended[PlainWindow.WIDTH + index]) <= 2e-6)) {
                    unequalCanaries++;
                }
            }
        }
        assertEquals(0, unequalCanaries);
    }

    @Test
    void legacyPayloadsKeepTheVersionFourBody() {
        WindowSubmission legacy = new WindowSubmission(PLAYER, "Aura_00", new byte[] {1, 2, 3, 4});
        PredictRequest request = new ScrambledWire(new WireSeed(42L)).request(bodies, IDENTITY, SESSION, legacy);
        assertEquals(4, request.wireVersion());
        assertTrue(request.extraHeaders().isEmpty());
        assertArrayEquals(bodies.predict(IDENTITY, SESSION, legacy), request.body());
    }

    @Test
    void plainWireIsTheUnchangedVersionFourBody() {
        WindowSubmission window = new WindowSubmission(PLAYER, "Aura_00", new byte[20 + 4 * 60 * 64]);
        PredictRequest request = UltraWire.PLAIN.request(bodies, IDENTITY, SESSION, window);
        assertArrayEquals(bodies.predict(IDENTITY, SESSION, window), request.body());
        assertTrue(request.extraHeaders().isEmpty());
    }

    @Test
    void theJitterStreamContinuesAcrossWindowsAndANewSessionStartsOver() throws Exception {
        JsonObject set = WireVectors.sets(WireVectors.load()).get(1);
        JsonObject first = (JsonObject) set.array("windows").orElseThrow().get(0);
        PlainWindow window = new PlainWindow(WireVectors.rows(first.get("plain_rows").orElseThrow()));
        WireSeed seed = WireSeed.parse(set.string("seed").orElseThrow()).orElseThrow();
        ScrambledWindows session = new ScrambledWindows(seed);
        double[][] opening = session.scramble(window);
        double[][] again = session.scramble(window);
        assertFalse(sameRows(opening, again));
        assertTrue(sameRows(opening, new ScrambledWindows(seed).scramble(window)));
    }

    @Test
    void initRepliesOfferTheSeed() throws Exception {
        assertInstanceOf(ScrambledWire.class, offer("{\"sessionId\":\"s\",\"wire5\":{\"seed\":\"18446744073709551615\"}}"));
        assertEquals(-1L, ((ScrambledWire) offer("{\"wire5\":{\"seed\":\"18446744073709551615\"}}")).seed().bits());
        assertInstanceOf(ScrambledWire.class, offer("{\"data\":{\"wire5\":{\"seed\":\"7\"}}}"));
        assertEquals(UltraWire.PLAIN, offer("{\"sessionId\":\"s\"}"));
        assertEquals(UltraWire.PLAIN, offer("{\"wire5\":{\"seed\":\"18446744073709551616\"}}"));
        assertEquals(UltraWire.PLAIN, offer("{\"wire5\":{\"seed\":\"-1\"}}"));
        assertEquals(UltraWire.PLAIN, offer("{\"wire5\":{\"seed\":12}}"));
        assertEquals(UltraWire.PLAIN, WireOffer.from(Optional.empty()));
    }

    @Test
    void reportsTheBodySizesOfARealGoldenWindow() throws Exception {
        Optional<byte[]> golden = firstGoldenWindow();
        Assumptions.assumeTrue(golden.isPresent(), "golden traces not available");
        WindowSubmission window = new WindowSubmission(PLAYER, "Aura_00", golden.get());
        int plainBytes = UltraWire.PLAIN.request(bodies, IDENTITY, SESSION, window).body().length;
        PredictRequest scrambled = new ScrambledWire(new WireSeed(0x9E3779B97F4A7C15L)).request(bodies, IDENTITY,
                SESSION, window);
        int jsonBytes = gunzip(scrambled.body()).getBytes(StandardCharsets.UTF_8).length;
        String report = "golden window ultra_aura_vs_legit #1: v4 body " + plainBytes + " B, v5 body " + jsonBytes
                + " B json / " + scrambled.body().length + " B gzip" + System.lineSeparator();
        Path out = Path.of("build", "reports", "wire5-size.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report, StandardCharsets.UTF_8);
        assertEquals(5, scrambled.wireVersion());
        assertTrue(scrambled.body().length < plainBytes * 2, report);
    }

    private static UltraWire offer(String json) throws Exception {
        return WireOffer.from(Optional.of(JsonParser.parseObject(json)));
    }

    private static Optional<byte[]> firstGoldenWindow() throws Exception {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            Path trace = current.resolve("golden/traces/synthetic/ultra_aura_vs_legit.jsonl.gz");
            if (Files.isRegularFile(trace)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(Files.newInputStream(trace)), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("\"n\":\"ng.window\"")) {
                            return Optional.of(Base64.getDecoder().decode(
                                    JsonParser.parseObject(line).string("b64").orElseThrow()));
                        }
                    }
                }
                return Optional.empty();
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private static byte[] mlsu(double[][] rows) {
        ByteBuffer buffer = ByteBuffer.allocate(20 + 4 * PlainWindow.TICKS * PlainWindow.WIDTH);
        buffer.put("MLSU".getBytes(StandardCharsets.US_ASCII)).putInt(4).putInt(PlainWindow.TICKS)
                .putInt(PlainWindow.FEATURES).putInt(PlainWindow.FLAGS);
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

    private static double[] flatten(double[][] rows) {
        double[] flat = new double[rows.length * PlainWindow.WIDTH];
        for (int tick = 0; tick < rows.length; tick++) {
            System.arraycopy(rows[tick], 0, flat, tick * PlainWindow.WIDTH, PlainWindow.WIDTH);
        }
        return flat;
    }

    private static boolean sameRows(double[][] left, double[][] right) {
        for (int tick = 0; tick < left.length; tick++) {
            for (int slot = 0; slot < left[tick].length; slot++) {
                if (!WireVectors.same(left[tick][slot], right[tick][slot])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
