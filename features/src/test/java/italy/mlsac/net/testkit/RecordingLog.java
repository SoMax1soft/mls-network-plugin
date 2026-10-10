// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.logbook.Logbook;
import java.util.ArrayList;
import java.util.List;

public final class RecordingLog implements Logbook {

    public final List<String> infos = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    public final List<String> severes = new ArrayList<>();
    public final List<String> debugs = new ArrayList<>();

    @Override
    public void info(String line) {
        infos.add(line);
    }

    @Override
    public void warn(String line) {
        warnings.add(line);
    }

    @Override
    public void severe(String line, Throwable cause) {
        severes.add(line);
    }

    @Override
    public void debug(String line) {
        debugs.add(line);
    }
}
