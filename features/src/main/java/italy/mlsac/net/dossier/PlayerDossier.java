// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.List;
import java.util.OptionalLong;

public record PlayerDossier(String player, boolean known, OptionalLong firstSeen, OptionalLong lastSeen,
                            DetectionTally detections, PunishmentTally punishments, int suppressionTotal,
                            List<HistoryEntry> suppressions, List<HistoryEntry> latest) {

    public PlayerDossier {
        suppressions = List.copyOf(suppressions);
        latest = List.copyOf(latest);
    }
}
