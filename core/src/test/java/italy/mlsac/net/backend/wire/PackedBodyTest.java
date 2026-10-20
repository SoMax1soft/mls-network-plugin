// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.http.HttpHeader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class PackedBodyTest {

    @Test
    void bodiesBelowTheThresholdAreSentAsTheyAre() {
        byte[] plain = json(PackedBody.SMALLEST_WORTH_PACKING - 1);
        PackedBody body = PackedBody.of(plain, List.of());
        assertFalse(body.packed());
        assertSame(plain, body.bytes());
        assertTrue(body.headers().isEmpty());
    }

    @Test
    void bodiesAtTheThresholdArePackedAndRoundTrip() throws Exception {
        byte[] plain = json(PackedBody.SMALLEST_WORTH_PACKING);
        PackedBody body = PackedBody.of(plain, List.of());
        assertTrue(body.packed());
        assertEquals(List.of(PackedBody.GZIP), body.headers());
        assertTrue(body.bytes().length < plain.length);
        assertArrayEquals(plain, gunzip(body.bytes()));
    }

    @Test
    void theGzipHeaderIsTheOneScrambledWireAlreadySends() {
        assertEquals("Content-Encoding", PackedBody.GZIP.name());
        assertEquals("gzip", PackedBody.GZIP.value());
    }

    @Test
    void existingHeadersAreKeptInFrontOfTheEncodingHeader() {
        HttpHeader marker = new HttpHeader("X-MLSAC-Test", "1");
        PackedBody body = PackedBody.of(json(4096), List.of(marker));
        assertEquals(List.of(marker, PackedBody.GZIP), body.headers());
    }

    @Test
    void anAlreadyEncodedBodyIsNotPackedTwice() {
        byte[] gzipped = GzipBody.compress(json(16_384));
        PackedBody body = PackedBody.of(gzipped, List.of(PackedBody.GZIP));
        assertFalse(body.packed());
        assertSame(gzipped, body.bytes());
        assertEquals(List.of(PackedBody.GZIP), body.headers());
    }

    @Test
    void anEncodingHeaderOfAnyCaseStopsASecondPass() {
        byte[] gzipped = GzipBody.compress(json(16_384));
        PackedBody body = PackedBody.of(gzipped, List.of(new HttpHeader("content-encoding", "gzip")));
        assertFalse(body.packed());
        assertSame(gzipped, body.bytes());
    }

    @Test
    void anIncompressibleBodyIsSentAsItIs() {
        byte[] noise = new byte[8192];
        new Random(20261006L).nextBytes(noise);
        PackedBody body = PackedBody.of(noise, List.of());
        assertFalse(body.packed());
        assertSame(noise, body.bytes());
        assertTrue(body.headers().isEmpty());
    }

    private static byte[] json(int length) {
        StringBuilder text = new StringBuilder(length);
        text.append("{\"serverName\":\"");
        while (text.length() < length - 2) {
            text.append('a');
        }
        text.append("\"}");
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] gunzip(byte[] packed) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return in.readAllBytes();
        }
    }
}
