// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.wire.PredictRequest;
import italy.mlsac.net.backend.wire.ScrambledWire;
import italy.mlsac.net.backend.wire.WireSeed;
import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.TickRow;
import italy.mlsac.net.pipeline.ultra.FightAssembler;
import italy.mlsac.net.pipeline.ultra.MlsuWire;
import italy.mlsac.net.pipeline.ultra.WireVersion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class WireSixRecordingExportTest {
    private static final WireSeed SEED = new WireSeed(0x5EED_0006_2026_1003L);
    private static final RequestIdentity IDENTITY = new RequestIdentity("2.0.dev", "parity", "net", "127.0.0.1",
            25565, false, Optional.empty());

    @Test
    void recordingsBecomeVersionSixBodies() throws IOException {
        String recordings = System.getProperty("mlsac.wire6.recordings", "");
        Assumptions.assumeFalse(recordings.isEmpty(), "set -Pwire6Recordings=name=folder+name=folder");
        String target = System.getProperty("mlsac.wire6.out", "");
        Path out = Path.of(target.isEmpty() ? "build/wire6-bodies.ndjson" : target);
        Files.createDirectories(out.toAbsolutePath().getParent());
        ScrambledWire wire = new ScrambledWire(SEED, ScrambledWire.WIDE_VERSION);
        BackendBodies bodies = new BackendBodies();
        try (Writer writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (String entry : recordings.split("\\+")) {
                String name = entry.substring(0, entry.indexOf('='));
                Path folder = Path.of(entry.substring(entry.indexOf('=') + 1));
                List<byte[]> windows = windowsOf(folder);
                assertTrue(windows.size() > 0, name + " gives no windows");
                for (int index = 0; index < windows.size(); index++) {
                    PredictRequest request = wire.request(bodies, IDENTITY, Optional.of("parity-session"),
                            new WindowSubmission(new UUID(6, index), name, windows.get(index)));
                    assertEquals(6, request.wireVersion());
                    writer.write("{\"recording\":\"" + name + "\",\"index\":" + index + ",\"seed\":\"" + SEED
                            + "\",\"body\":" + gunzip(request.body()) + "}\n");
                }
            }
        }
    }

    private static List<byte[]> windowsOf(Path folder) throws IOException {
        List<byte[]> windows = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(window -> windows.add(MlsuWire.encode(WireVersion.V6, window)));
        List<String> clickLines = Files.readAllLines(folder.resolve("hits.csv"), StandardCharsets.UTF_8);
        StoredRows clickTable = new StoredRows(clickLines.get(0));
        List<ClickRow> clicks = new ArrayList<>();
        for (String line : clickLines.subList(1, clickLines.size())) {
            if (!line.isEmpty()) {
                ClickRow click = clickTable.line(line).click();
                if (click.timing().row() >= 0) {
                    clicks.add(click);
                }
            }
        }
        clicks.sort((left, right) -> Long.compare(left.timing().row(), right.timing().row()));
        List<String> tickLines = Files.readAllLines(folder.resolve("ticks.csv"), StandardCharsets.UTF_8);
        StoredRows ticks = new StoredRows(tickLines.get(0));
        int nextClick = 0;
        for (String line : tickLines.subList(1, tickLines.size())) {
            if (line.isEmpty()) {
                continue;
            }
            TickRow row = ticks.line(line).tick();
            while (nextClick < clicks.size() && clicks.get(nextClick).timing().row() <= row.timing().row()) {
                if (clicks.get(nextClick).timing().row() == row.timing().row()) {
                    assembler.clickRow(clicks.get(nextClick));
                }
                nextClick++;
            }
            assembler.tickRow(row);
        }
        return windows;
    }

    private static String gunzip(byte[] packed) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
