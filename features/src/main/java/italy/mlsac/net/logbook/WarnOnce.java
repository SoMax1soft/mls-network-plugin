// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.logbook;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WarnOnce {

    private final Logbook log;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public WarnOnce(Logbook log) {
        this.log = log;
    }

    public void warn(String key, String line) {
        if (seen.add(key)) {
            log.warn(line);
        }
    }

    public void forget() {
        seen.clear();
    }
}
