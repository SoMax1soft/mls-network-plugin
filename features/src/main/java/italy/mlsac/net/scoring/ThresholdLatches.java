// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.HashMap;
import java.util.Map;

public final class ThresholdLatches {

    private final Map<String, Boolean> fired = new HashMap<>();

    public synchronized boolean crossed(String name, double value, double threshold) {
        if (value < threshold) {
            fired.put(name, Boolean.FALSE);
            return false;
        }
        if (Boolean.TRUE.equals(fired.get(name))) {
            return false;
        }
        fired.put(name, Boolean.TRUE);
        return true;
    }

    public synchronized boolean armed(String name) {
        return !Boolean.TRUE.equals(fired.get(name));
    }

    public synchronized void clear() {
        fired.clear();
    }
}
