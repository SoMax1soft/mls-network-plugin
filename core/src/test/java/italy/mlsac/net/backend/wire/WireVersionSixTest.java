// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class WireVersionSixTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity("2.0.dev", "lobby-1", "net", "127.0.0.1",
            25565, true, Optional.of("pvp"));
    private static final Optional<String> SESSION = Optional.of("1790000000000-k2j3h4g5f6d7s");
    private static final UUID PLAYER = UUID.fromString("10178d1e-9241-38c7-9ee9-c4629980b606");
    private static final WireSeed SEED = new WireSeed(0x0123_4567_89AB_CDEFL);

    private final BackendBodies bodies = new BackendBodies();

    @Test
    void wideOfferWinsAndOldOffersStayAsTheyWere() throws Exception {
        UltraWire both = offer("{\"wire5\":{\"seed\":\"5\"},\"wire6\":{\"seed\":\"6\"}}");
        assertEquals(6, both.version());
        assertEquals(6L, ((ScrambledWire) both).seed().bits());
        assertEquals(6, offer("{\"data\":{\"wire6\":{\"seed\":\"9\"}}}").version());
        assertEquals(5, offer("{\"wire5\":{\"seed\":\"5\"}}").version());
        assertEquals(5, offer("{\"wire5\":{\"seed\":\"5\"},\"wire6\":{\"seed\":\"-1\"}}").version());
        assertEquals(UltraWire.PLAIN, offer("{\"sessionId\":\"s\"}"));
    }

    @Test
    void versionSixBodyUnscramblesToTheSeventyTwoValuePlainRow() throws Exception {
        byte[] wide = wide();
        PredictRequest request = new ScrambledWire(SEED, 6).request(bodies, IDENTITY, SESSION,
                new WindowSubmission(PLAYER, "Aura_00", wide));
        assertEquals(6, request.wireVersion());
        JsonObject body = JsonParser.parseObject(gunzip(request.body()));
        assertEquals(6.0, body.number("wire_version").orElseThrow().doubleValue());
        List<JsonValue> data = body.array("data").orElseThrow().elements();
        assertEquals(60 * 102, data.size());
        double[][] plain = UltraPayload.read(wide).orElseThrow().plainRows(72);
        SlotOrder order = SlotOrder.shuffled(SEED, 102);
        for (int tick = 0; tick < 60; tick++) {
            double[] extended = new double[102];
            for (int slot = 0; slot < 102; slot++) {
                extended[order.sourceOf(slot)] = WireVectors.number(data.get(tick * 102 + slot));
            }
            for (int column = 0; column < 72; column++) {
                assertTrue(WireVectors.same(plain[tick][column], extended[column]), "tick " + tick + " col " + column);
            }
            for (int index = 20; index < DerivedColumns.COUNT; index++) {
                assertTrue(Math.abs(DerivedColumns.phase(extended, index, tick) - extended[72 + index]) <= 2e-6);
            }
            assertEquals(1000.0 + tick, extended[64]);
            assertEquals(1.0, extended[71]);
        }
    }

    @Test
    void wideWindowsLeaveVersionsFourAndFiveByteForByte() {
        WindowSubmission wide = new WindowSubmission(PLAYER, "Aura_00", wide());
        WindowSubmission narrow = new WindowSubmission(PLAYER, "Aura_00", UltraPayload.read(wide()).orElseThrow()
                .layoutFour());
        assertArrayEquals(UltraWire.PLAIN.request(bodies, IDENTITY, SESSION, narrow).body(),
                UltraWire.PLAIN.request(bodies, IDENTITY, SESSION, wide).body());
        assertArrayEquals(new ScrambledWire(SEED).request(bodies, IDENTITY, SESSION, narrow).body(),
                new ScrambledWire(SEED).request(bodies, IDENTITY, SESSION, wide).body());
        assertEquals(4, new ScrambledWire(SEED, 6).request(bodies, IDENTITY, SESSION, narrow).wireVersion());
    }

    @Test
    void initAdvertisesTheHighestUltraWire() {
        String plain = new String(bodies.init(IDENTITY, "<API-KEY>", 0), StandardCharsets.UTF_8);
        String offering = new String(bodies.init(IDENTITY, "<API-KEY>", 0, WireOffer.HIGHEST_VERSION),
                StandardCharsets.UTF_8);
        assertEquals(plain.substring(0, plain.length() - 1) + ",\"ultraWire\":6}", offering);
    }

    private static UltraWire offer(String json) throws Exception {
        return WireOffer.from(Optional.of(JsonParser.parseObject(json)));
    }

    private static byte[] wide() {
        ByteBuffer buffer = ByteBuffer.allocate(20 + 4 * 60 * 72);
        buffer.put("MLSU".getBytes(StandardCharsets.US_ASCII)).putInt(6).putInt(60).putInt(54).putInt(18);
        for (int tick = 0; tick < 60; tick++) {
            for (int column = 0; column < 54; column++) {
                buffer.putFloat(column == 50 ? 1000f + tick : ((tick * 54 + column) % 97) * 0.37f);
            }
        }
        for (int tick = 0; tick < 60; tick++) {
            for (int flag = 0; flag < 18; flag++) {
                buffer.putFloat(flag == 17 ? 1f : (tick + flag) % 2);
            }
        }
        return buffer.array();
    }

    private static String gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
