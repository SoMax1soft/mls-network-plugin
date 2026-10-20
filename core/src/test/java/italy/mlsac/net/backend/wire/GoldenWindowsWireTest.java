// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonValue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class GoldenWindowsWireTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity("2.0.dev", "lobby", "net", "127.0.0.1",
            25565, false, Optional.empty());

    @Test
    void everyGoldenWindowDecodesBackToItsVersionFourRows() throws Exception {
        List<byte[]> windows = goldenWindows();
        Assumptions.assumeTrue(!windows.isEmpty(), "golden traces not available");
        WireSeed seed = new WireSeed(0xC0FFEE1234567890L);
        ScrambledWire wire = new ScrambledWire(seed);
        SlotOrder order = SlotOrder.shuffled(seed, ScrambledWindows.SLOTS);
        BackendBodies bodies = new BackendBodies();
        int badCanaries = 0;
        for (byte[] payload : windows) {
            double[][] plain = PlainWindow.fromUltraPayload(payload).orElseThrow().rows();
            byte[] body = wire.request(bodies, IDENTITY, Optional.of("golden"),
                    new WindowSubmission(UUID.randomUUID(), "Golden", payload)).body();
            List<JsonValue> data = JsonParser.parseObject(gunzip(body)).array("data").orElseThrow().elements();
            for (int tick = 0; tick < PlainWindow.TICKS; tick++) {
                double[] extended = new double[ScrambledWindows.SLOTS];
                for (int slot = 0; slot < ScrambledWindows.SLOTS; slot++) {
                    extended[order.sourceOf(slot)] = WireVectors.number(data.get(tick * ScrambledWindows.SLOTS + slot));
                }
                for (int column = 0; column < PlainWindow.WIDTH; column++) {
                    assertTrue(WireVectors.same(plain[tick][column], extended[column]), "tick " + tick + " column "
                            + column);
                    float sent = payloadFloat(payload, tick, column);
                    float decoded = (float) extended[column];
                    assertTrue(Float.isFinite(sent) ? Float.floatToIntBits(sent) == Float.floatToIntBits(decoded)
                            || (sent == 0.0f && decoded == 0.0f) : Float.isNaN(decoded),
                            "v4 float " + sent + " decoded as " + decoded);
                }
                for (int index = 20; index < DerivedColumns.COUNT; index++) {
                    if (!(Math.abs(DerivedColumns.phase(extended, index, tick)
                            - extended[PlainWindow.WIDTH + index]) <= 2e-6)) {
                        badCanaries++;
                    }
                }
            }
        }
        assertEquals(0, badCanaries);
        assertTrue(windows.size() > 400, "expected the Ultra golden windows, found " + windows.size());
    }

    private static float payloadFloat(byte[] payload, int tick, int column) {
        int index = column < PlainWindow.FEATURES ? tick * PlainWindow.FEATURES + column
                : PlainWindow.TICKS * PlainWindow.FEATURES + tick * PlainWindow.FLAGS + column - PlainWindow.FEATURES;
        return ByteBuffer.wrap(payload, 20 + 4 * index, 4).getFloat();
    }

    private static List<byte[]> goldenWindows() throws Exception {
        List<byte[]> windows = new ArrayList<>();
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            Path folder = current.resolve("golden/traces/synthetic");
            if (Files.isDirectory(folder)) {
                try (Stream<Path> files = Files.list(folder)) {
                    for (Path trace : files.filter(file -> file.getFileName().toString().startsWith("ultra_")).sorted()
                            .toList()) {
                        collect(trace, windows);
                    }
                }
                return windows;
            }
            current = current.getParent();
        }
        return windows;
    }

    private static void collect(Path trace, List<byte[]> into) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(trace)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"n\":\"ng.window\"")) {
                    JsonObject record = JsonParser.parseObject(line);
                    into.add(Base64.getDecoder().decode(record.string("b64").orElseThrow()));
                }
            }
        }
    }

    private static String gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
