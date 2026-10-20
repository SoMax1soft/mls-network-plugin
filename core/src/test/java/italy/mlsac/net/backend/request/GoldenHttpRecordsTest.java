// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpHeader;
import italy.mlsac.net.backend.wire.PackedBody;
import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonSyntaxException;
import italy.mlsac.net.json.JsonValue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

class GoldenHttpRecordsTest {

    private static final String API_ROOT = "/api/v1/";
    private static final String TRANSIT_GOLDEN = "/golden/http-transit.tsv";
    private static final String TRANSIT_HEADER = "#file\tendpoint\trecords\tplain\tsent\tgzipped\tsha256";

    private final BackendBodies bodies = new BackendBodies();
    private final Map<String, Transit> transit = new TreeMap<>();
    private int plainRecords;
    private int gzippedRecords;

    private static final class Transit {

        private final MessageDigest chain = digest();
        private int records;
        private long plainBytes;
        private long sentBytes;
        private int gzipped;

        void add(byte[] plain, PackedBody sent) {
            records++;
            plainBytes += plain.length;
            sentBytes += sent.bytes().length;
            if (sent.packed()) {
                gzipped++;
            }
            chain.update((byte) (sent.packed() ? 'g' : 'p'));
            chain.update(sent.bytes());
        }

        String row(String file, String endpoint) {
            return file + '\t' + endpoint + '\t' + records + '\t' + plainBytes + '\t' + sentBytes + '\t' + gzipped
                    + '\t' + hex(chain.digest());
        }
    }

    @Test
    void everyGoldenRequestIsReproducedByteForByte() throws IOException, JsonSyntaxException {
        Optional<Path> traces = locateTraces();
        assumeTrue(traces.isPresent(), "golden traces not available next to this project");
        int checked = 0;
        try (Stream<Path> files = Files.list(traces.get())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".jsonl.gz")).sorted().toList()) {
                checked += checkFile(file);
            }
        }
        assertTrue(checked > 900, "expected all golden http records, checked " + checked);
        assertTrue(plainRecords > 0, "no golden request stayed uncompressed");
        assertTrue(gzippedRecords > 0, "no golden request was compressed");
        assertEquals(checked, plainRecords + gzippedRecords);
        Path written = writeTransit();
        assertEquals(expectedTransit(), Files.readAllLines(written, StandardCharsets.UTF_8),
                "transit encoding changed - review " + written.toAbsolutePath()
                        + " and copy it over core/src/test/resources" + TRANSIT_GOLDEN);
    }

    private int checkFile(Path file) throws IOException, JsonSyntaxException {
        int checked = 0;
        String name = file.getFileName().toString();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"t\":\"O\",\"n\":\"http\"")) {
                    checkRecord(name, JsonParser.parseObject(line));
                    checked++;
                }
            }
        }
        return checked;
    }

    private void checkRecord(String trace, JsonObject record) throws JsonSyntaxException {
        String url = record.string("url").orElseThrow();
        String body = record.string("body").orElseThrow();
        JsonObject parsed = JsonParser.parseObject(body);
        String path = url.substring(url.indexOf(API_ROOT) + API_ROOT.length());
        String base = url.substring(0, url.indexOf(API_ROOT));
        String context = trace + " s=" + record.number("s").map(JsonNumber::literal).orElse("?");
        assertEquals(url, BackendAddress.of(base, "").uri(Endpoint.PRIMARY, path).toString(), context);
        assertEquals("application/json; charset=utf-8", record.string("ct").orElseThrow(), context);
        byte[] plain = rebuild(path, parsed);
        assertEquals(body, new String(plain, StandardCharsets.UTF_8), context);
        assertEquals(headersOf(record), expectedHeaders(path, parsed, record), context);
        checkTransit(trace, path, plain, context);
    }

    private void checkTransit(String trace, String path, byte[] plain, String context) {
        PackedBody sent = PackedBody.of(plain, List.of());
        if (plain.length < PackedBody.SMALLEST_WORTH_PACKING) {
            assertFalse(sent.packed(), context + " small body was compressed");
            assertArrayEquals(plain, sent.bytes(), context);
            assertTrue(sent.headers().isEmpty(), context);
            plainRecords++;
        } else {
            assertTrue(sent.packed(), context + " large body stayed plain");
            assertEquals(List.of(PackedBody.GZIP), sent.headers(), context);
            assertTrue(sent.bytes().length < plain.length, context);
            assertArrayEquals(plain, gunzip(sent.bytes()), context + " did not round-trip");
            gzippedRecords++;
        }
        transit.computeIfAbsent(trace + '\u0000' + path, key -> new Transit()).add(plain, sent);
        transit.computeIfAbsent(trace + '\u0000' + "*", key -> new Transit()).add(plain, sent);
    }

    private Path writeTransit() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# bodies below " + PackedBody.SMALLEST_WORTH_PACKING
                + " bytes stay plain, larger ones are gzipped; sha256 chains the sent bytes in trace order");
        lines.add(TRANSIT_HEADER);
        for (Map.Entry<String, Transit> entry : transit.entrySet()) {
            int split = entry.getKey().indexOf('\u0000');
            lines.add(entry.getValue().row(entry.getKey().substring(0, split),
                    entry.getKey().substring(split + 1)));
        }
        Path out = Path.of("build", "reports", "golden", "http-transit.tsv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", lines) + '\n', StandardCharsets.UTF_8);
        return out;
    }

    private static List<String> expectedTransit() throws IOException {
        try (InputStream in = GoldenHttpRecordsTest.class.getResourceAsStream(TRANSIT_GOLDEN)) {
            assertTrue(in != null, "missing golden resource " + TRANSIT_GOLDEN);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        }
    }

    private byte[] rebuild(String path, JsonObject body) {
        RequestIdentity identity = new RequestIdentity(body.string("pluginVersion").orElseThrow(),
                body.string("serverName").orElseThrow(), body.string("serverFamily").orElseThrow(),
                body.string("serverIp").orElseThrow(), intOf(body, "serverPort"),
                body.bool("interServer").orElseThrow(), body.string("preset"));
        Optional<String> session = body.string("sessionId");
        return switch (path) {
            case "init" -> bodies.init(identity, body.string("apiKey").orElseThrow(), intOf(body, "onlinePlayers"));
            case "heartbeat", "online", "reportstats" -> bodies.status(identity, session, intOf(body, "onlinePlayers"));
            case "events/poll" -> bodies.poll(identity, session);
            case "events" -> event(identity, session, body);
            case "predict-stream" -> bodies.predict(identity, session, new WindowSubmission(
                    UUID.fromString(body.string("playerUuid").orElseThrow()), body.string("playerName").orElseThrow(),
                    Base64.getDecoder().decode(body.string("playerData").orElseThrow())));
            default -> throw new AssertionError("unexpected endpoint " + path);
        };
    }

    private byte[] event(RequestIdentity identity, Optional<String> session, JsonObject body) {
        UUID player = UUID.fromString(body.string("playerUuid").orElseThrow());
        String name = body.string("playerName").orElseThrow();
        double probability = doubleOf(body, "probability");
        double buffer = doubleOf(body, "buffer");
        if ("alert".equals(body.string("type").orElseThrow())) {
            return bodies.alert(identity, session, new AlertReport(player, name, body.string("model").orElseThrow(),
                    probability, buffer));
        }
        return bodies.punish(identity, session, new PunishReport(player, name, probability, buffer, intOf(body, "vl"),
                body.string("action").orElseThrow(), body.string("command").orElseThrow()));
    }

    private static List<HttpHeader> expectedHeaders(String path, JsonObject body, JsonObject record) {
        String version = body.string("pluginVersion").orElseThrow();
        if (path.equals("init")) {
            return new BackendHeaders(version, "unused").withoutKey();
        }
        String key = headersOf(record).get(0).value();
        return new BackendHeaders(version, key).withKey();
    }

    private static List<HttpHeader> headersOf(JsonObject record) {
        List<HttpHeader> headers = new ArrayList<>();
        for (JsonValue pair : record.array("headers").orElseThrow().elements()) {
            JsonArray entry = (JsonArray) pair;
            headers.add(new HttpHeader(((JsonString) entry.get(0)).value(),
                    ((JsonString) entry.get(1)).value()));
        }
        return headers;
    }

    private static int intOf(JsonObject body, String name) {
        return Integer.parseInt(body.number(name).orElseThrow().literal());
    }

    private static double doubleOf(JsonObject body, String name) {
        return body.number(name).orElseThrow().doubleValue();
    }

    private static byte[] gunzip(byte[] packed) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return in.readAllBytes();
        } catch (IOException broken) {
            throw new AssertionError("gzipped body could not be read back", broken);
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException missing) {
            throw new AssertionError("SHA-256 is part of every JRE", missing);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
        }
        return text.toString();
    }

    private static Optional<Path> locateTraces() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++) {
            Path candidate = current.resolve("golden").resolve("traces").resolve("synthetic");
            if (Files.isDirectory(candidate)) {
                return Optional.of(candidate);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }
}
