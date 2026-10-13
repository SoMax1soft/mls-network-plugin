// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.List;

public record HistoryEntry(String kind, long at, String action, String model, double probability, String trigger,
                           int level, double buffer, List<String> actions) {

    public static final String PUNISH = "punish";
    public static final String SUPPRESSION = "suppression";
    public static final String DETECTION = "detection";

    public HistoryEntry {
        actions = List.copyOf(actions);
    }
}
