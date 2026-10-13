// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.detection.RestoreAnswer;
import italy.mlsac.net.detection.RestoreLabel;
import italy.mlsac.net.detection.RestoreOutcome;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.PlatformText;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.UuidPurpose;
import italy.mlsac.net.pipeline.environment.UuidSource;
import italy.mlsac.net.pipeline.environment.WallClock;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import italy.mlsac.net.pipeline.history.PlayerNameSanitizer;
import italy.mlsac.net.pipeline.tables.CompactLine;
import italy.mlsac.net.pipeline.tables.CsvText;
import italy.mlsac.net.pipeline.tables.SampleHeaders;
import italy.mlsac.net.pipeline.tables.SessionFacts;
import italy.mlsac.net.pipeline.tables.SessionNote;
import italy.mlsac.net.pipeline.tables.SessionSheet;
import italy.mlsac.net.pipeline.tables.TableColumns;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class RestoreWriter {
    static final String ROOT = "data/restore/";
    static final String SERVER_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final DataFolder folder;
    private final PlatformText platform;
    private final WallClock wallClock;
    private final UuidSource uuids;
    private final PlayerConnections connections;

    RestoreWriter(DataFolder folder, PlatformText platform, WallClock wallClock, UuidSource uuids,
            PlayerConnections connections) {
        this.folder = folder;
        this.platform = platform;
        this.wallClock = wallClock;
        this.uuids = uuids;
        this.connections = connections;
    }

    RestoreAnswer write(RestoreContent content, RestoreRequest request) throws IOException {
        RestoreLabel label = request.label();
        String nick = connections.name(content.player());
        String directory = ROOT + label.id() + "/" + label.spelling() + "_" + PlayerNameSanitizer.sanitise(nick) + "_"
                + content.beganAt().toEpochMilli() + "_" + content.sessionKey();
        if (folder.exists(directory)) {
            directory = directory + "_retry_" + uuids.next(UuidPurpose.RESTORE_RETRY);
        }
        String separator = platform.lineSeparator();
        List<List<String>> ticks = tickLines(label.classValue(), content.ticks());
        List<List<String>> hits = hitLines(label.classValue(), content.hits(), content.firstKeptTickStamp());
        store(directory + "/samples.csv", CsvText.render(SampleHeaders.SAMPLES, List.of(), separator));
        store(directory + "/attacks.csv", CsvText.render(SampleHeaders.ATTACKS, List.of(), separator));
        store(directory + "/ticks.csv", CsvText.render(TableColumns.TICK_HEADER, ticks, separator));
        store(directory + "/hits.csv", CsvText.render(TableColumns.CLICK_HEADER, hits, separator));
        Instant savedAt = wallClock.instant(WallPurpose.RESTORE_SAVED);
        List<SessionNote> notes = List.of(
                new SessionNote("restored_by", request.staffName()),
                new SessionNote("restore_kind", label.id()),
                new SessionNote("restore_window", "newest " + content.ticks().capacity() + " kept tick rows"),
                new SessionNote("saved_at_server", wallClock.localStamp(WallPurpose.RESTORE_SAVED,
                        SERVER_TIME_PATTERN)));
        store(directory + "/session.json", SessionSheet.render(new SessionFacts(
                connections.clientLabel(content.player()), content.sessionKey(), content.player(), nick,
                label.spelling(), "restore_" + label.id(), notes, content.beganAt(), savedAt, 0, 0, ticks.size(),
                hits.size())));
        return new RestoreAnswer(RestoreOutcome.SAVED, ticks.size(), hits.size(), directory);
    }

    static List<List<String>> tickLines(int label, RowRing ring) {
        List<List<String>> lines = new ArrayList<>(ring.size());
        for (int position = 0; position < ring.size(); position++) {
            CompactLine line = new CompactLine().integer(label);
            ring.replay(position, line);
            lines.add(line.fields());
        }
        return lines;
    }

    static List<List<String>> hitLines(int label, RowRing ring, long firstTickStamp) {
        List<List<String>> lines = new ArrayList<>(ring.size());
        for (int position = 0; position < ring.size(); position++) {
            if (ring.stampAt(position) < firstTickStamp) {
                continue;
            }
            CompactLine line = new CompactLine().integer(label);
            ring.replay(position, line);
            lines.add(line.fields());
        }
        return lines;
    }

    private void store(String path, String text) throws IOException {
        folder.write(path, text.getBytes(StandardCharsets.UTF_8));
    }
}
