// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

final class TraceFile {
    private final Path path;
    private TraceRecord header;
    private final Map<UUID, String> names = new HashMap<>();

    TraceFile(Path path) {
        this.path = path;
    }

    TraceRecord header() {
        return header;
    }

    Map<UUID, String> names() {
        return names;
    }

    void forEachEvent(Consumer<TraceEvent> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(path), 1 << 16), StandardCharsets.UTF_8), 1 << 16)) {
            TraceRecord head = null;
            List<TraceRecord> body = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                TraceRecord record = new TraceRecord(JsonReader.parseObject(line));
                String type = record.type();
                if ("H".equals(type)) {
                    header = record;
                    continue;
                }
                if ("P".equals(type)) {
                    names.put(record.player(), record.text("name"));
                    continue;
                }
                boolean topEvent = record.isEvent() && record.depth() == 0;
                if (topEvent || "X".equals(type)) {
                    if (head != null) {
                        consumer.accept(new TraceEvent(head, body));
                    }
                    head = topEvent ? record : null;
                    body = new ArrayList<>();
                    continue;
                }
                if (head != null) {
                    body.add(record);
                }
            }
            if (head != null) {
                consumer.accept(new TraceEvent(head, body));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
